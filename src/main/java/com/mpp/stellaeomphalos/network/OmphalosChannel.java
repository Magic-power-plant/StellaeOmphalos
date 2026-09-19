package com.mpp.stellaeomphalos.network;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.Omphalos;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** The only imperative wire adapter: Forge 47 has no StreamCodec or CustomPacketPayload API. */
public final class OmphalosChannel {
    private record Frame(int payloadId, UUID session, int totalBytes, int index, byte[] data, boolean valid) {}
    private record ClientFrame(Frame frame) {}
    private record ServerFrame(Frame frame) {}
    private static final UUID DIRECT = new UUID(0, 0);
    private static final UUID CLIENT_PEER = new UUID(0, 1);
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Omphalos.MODID, "main"), ProtocolVersion.CURRENT::toString,
            ProtocolVersion.CURRENT::accepts, ProtocolVersion.CURRENT::accepts);
    public static final PayloadRegistry PAYLOADS = new PayloadRegistry();
    private static final ChunkAssembler SERVER_ASSEMBLER = new ChunkAssembler();
    private static final ChunkAssembler CLIENT_ASSEMBLER = new ChunkAssembler();
    private static final NetworkThrottle INGRESS = new NetworkThrottle();
    private static final java.util.Map<UUID, ProtocolVersion> SERVER_PEER_VERSIONS = new java.util.HashMap<>();
    private static Consumer<OmphalosPayload> clientReceiver = ignored -> {};
    private static BiConsumer<ServerPlayer, OmphalosPayload> serverReceiver = (player, payload) -> {};
    private OmphalosChannel() {}

    public static void initialize() {
        CHANNEL.registerMessage(0, ClientFrame.class, (frame, buffer) -> encode(frame.frame(), buffer),
                buffer -> new ClientFrame(decode(buffer)), (frame, context) -> receive(frame.frame(), context, PayloadRegistry.Direction.TO_CLIENT),
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, ServerFrame.class, (frame, buffer) -> encode(frame.frame(), buffer),
                buffer -> new ServerFrame(decode(buffer)), (frame, context) -> receive(frame.frame(), context, PayloadRegistry.Direction.TO_SERVER),
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        PAYLOADS.freeze();
    }
    public static void setClientReceiver(Consumer<OmphalosPayload> receiver) { clientReceiver = receiver; }
    public static void setServerReceiver(BiConsumer<ServerPlayer, OmphalosPayload> receiver) { serverReceiver = receiver; }
    public static void send(ServerPlayer player, OmphalosPayload payload) {
        var remote = SERVER_PEER_VERSIONS.getOrDefault(player.getUUID(), new ProtocolVersion(1, 0));
        if (PAYLOADS.type(payload).minimumMinor() > remote.minor()) return;
        frames(payload, PayloadRegistry.Direction.TO_CLIENT, frame -> CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientFrame(frame)));
    }
    public static void acceptPeerVersion(UUID player, ProtocolVersion version) {
        if (version.major() != ProtocolVersion.CURRENT.major()) throw new IllegalArgumentException("Incompatible peer protocol");
        SERVER_PEER_VERSIONS.put(player, version);
        if (version.minor() != ProtocolVersion.CURRENT.minor()) LogUtils.getLogger().warn("Peer {} uses protocol {}, local {}", player, version, ProtocolVersion.CURRENT);
    }
    public static void sendToServer(OmphalosPayload payload) {
        frames(payload, PayloadRegistry.Direction.TO_SERVER, frame -> CHANNEL.sendToServer(new ServerFrame(frame)));
    }
    private static void frames(OmphalosPayload payload, PayloadRegistry.Direction direction, Consumer<Frame> sender) {
        var type = PAYLOADS.type(payload);
        if (type.direction() != direction) throw new IllegalArgumentException("Wrong send direction");
        byte[] bytes = PAYLOADS.encode(payload);
        if (bytes.length <= ChunkedEnvelope.THRESHOLD) sender.accept(new Frame(type.id(), DIRECT, bytes.length, 0, bytes, true));
        else for (var part : ChunkedEnvelope.split(type.id(), bytes))
            sender.accept(new Frame(part.payloadId(), part.session(), part.totalBytes(), part.index(), part.bytes(), true));
    }
    private static void encode(Frame frame, FriendlyByteBuf buffer) {
        buffer.writeVarInt(frame.payloadId()); buffer.writeUUID(frame.session());
        buffer.writeVarInt(frame.totalBytes()); buffer.writeVarInt(frame.index()); buffer.writeByteArray(frame.data());
    }
    private static Frame decode(FriendlyByteBuf buffer) {
        try {
            int id = buffer.readVarInt(); var session = buffer.readUUID();
            int total = buffer.readVarInt(); int index = buffer.readVarInt();
            byte[] data = buffer.readByteArray(ChunkedEnvelope.THRESHOLD);
            if (buffer.isReadable() || id < 0 || total < 1 || total > ChunkedEnvelope.MAX_BYTES) throw new IllegalArgumentException("Invalid frame");
            if (session.equals(DIRECT) && (total != data.length || index != 0)) throw new IllegalArgumentException("Invalid direct frame");
            return new Frame(id, session, total, index, data, true);
        } catch (RuntimeException exception) { return new Frame(-1, DIRECT, 0, 0, new byte[0], false); }
    }
    private static void receive(Frame frame, Supplier<NetworkEvent.Context> supplier, PayloadRegistry.Direction direction) {
        var context = supplier.get();
        context.setPacketHandled(true);
        if (direction == PayloadRegistry.Direction.TO_SERVER) {
            var player = context.getSender();
            if (player == null) return;
            NetworkThrottle.Decision decision;
            synchronized (INGRESS) { decision = INGRESS.acquire(player.getUUID(), 0, System.nanoTime() / 50000000L, 1024, 64); }
            if (!decision.accepted()) {
                if (decision.warn()) LogUtils.getLogger().warn("Network ingress throttled for {}", player.getUUID());
                return;
            }
        }
        context.enqueueWork(() -> {
            if (!frame.valid()) { disconnect(context, "Malformed payload frame"); return; }
            try {
                if (PAYLOADS.type(frame.payloadId()).direction() != direction) throw new IllegalArgumentException("Wrong receive direction");
                if (frame.session().equals(DIRECT)) deliver(PAYLOADS.decode(frame.payloadId(), direction, frame.data()), context, direction);
                else {
                    int count = (frame.totalBytes() + ChunkedEnvelope.CHUNK_SIZE - 1) / ChunkedEnvelope.CHUNK_SIZE;
                    var fragment = new ChunkedEnvelope(frame.session(), count, frame.index(), frame.payloadId(), frame.totalBytes(), frame.data());
                    var assembler = direction == PayloadRegistry.Direction.TO_SERVER ? SERVER_ASSEMBLER : CLIENT_ASSEMBLER;
                    UUID peer = direction == PayloadRegistry.Direction.TO_SERVER ? context.getSender().getUUID() : CLIENT_PEER;
                    assembler.accept(peer, fragment, System.nanoTime()).ifPresent(completed ->
                            deliver(PAYLOADS.decode(completed.payloadId(), direction, completed.bytes()), context, direction));
                }
            } catch (RuntimeException exception) {
                LogUtils.getLogger().warn("Rejected payload {}: {}", frame.payloadId(), exception.toString());
                disconnect(context, "Invalid Stellae Omphalos payload");
            }
        });
    }
    private static void deliver(OmphalosPayload payload, NetworkEvent.Context context, PayloadRegistry.Direction direction) {
        if (direction == PayloadRegistry.Direction.TO_SERVER) serverReceiver.accept(context.getSender(), payload);
        else clientReceiver.accept(payload);
    }
    private static void disconnect(NetworkEvent.Context context, String reason) { context.getNetworkManager().disconnect(Component.literal(reason)); }
    public static void tickServer() { SERVER_ASSEMBLER.expire(System.nanoTime()); }
    public static void tickClient() { CLIENT_ASSEMBLER.expire(System.nanoTime()); }
    public static void disconnectServerPlayer(UUID player) {
        SERVER_PEER_VERSIONS.remove(player);
        SERVER_ASSEMBLER.remove(player);
        synchronized (INGRESS) { INGRESS.remove(player); }
    }
    public static void resetServer() { SERVER_ASSEMBLER.clear(); SERVER_PEER_VERSIONS.clear(); synchronized (INGRESS) { INGRESS.clear(); } }
    public static void resetClient() { CLIENT_ASSEMBLER.clear(); }
}
