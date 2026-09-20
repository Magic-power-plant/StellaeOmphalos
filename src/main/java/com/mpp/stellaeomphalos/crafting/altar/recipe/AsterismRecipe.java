package com.mpp.stellaeomphalos.crafting.altar.recipe;

import com.google.gson.*;
import com.mpp.stellaeomphalos.data.codec.*;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

public class AsterismRecipe extends AbstractMachineRecipe {
    public record RelayRequirement(int index, MaterialSpec material, double bindAt) {}

    private final AsterismTier tier;
    private final GridPattern grid;
    private final Map<Integer, MaterialSpec> auxiliary;
    private final Set<Integer> keep;
    private final List<RelayRequirement> relays;
    private final Set<String> flags;
    private final List<PhaseSpec> phases;
    private final long lumen;
    private final int duration;
    private final ResourceLocation focus, advancement;

    public AsterismRecipe(ResourceLocation id, JsonObject json) {
        this(id, "asterism_crafting", json);
    }

    protected AsterismRecipe(ResourceLocation id, String family, JsonObject json) {
        super(id, family, json);
        tier =
                AsterismTier.parse(
                        RecipeJson.text(
                                json, "tier", RecipeJson.text(json, "from_tier", "discovery")));
        if (tier == AsterismTier.RADIANCE)
            throw new JsonParseException("Radiance has no recipe family");
        grid = new GridPattern(json);
        var aux = new TreeMap<Integer, MaterialSpec>();
        var kept = new HashSet<Integer>();
        parseSlots(
                json,
                "resonance",
                9,
                new String[] {"upper_left", "upper_right", "lower_left", "lower_right"},
                aux,
                kept);
        parseSlots(
                json,
                "sign",
                13,
                new String[] {
                    "up_up_left",
                    "up_up_right",
                    "up_left_left",
                    "up_right_right",
                    "down_left_left",
                    "down_right_right",
                    "down_down_left",
                    "down_down_right"
                },
                aux,
                kept);
        parseSlots(
                json,
                "trait",
                21,
                new String[] {"upper_center", "left_center", "right_center", "lower_center"},
                aux,
                kept);
        if (aux.keySet().stream().anyMatch(i -> i >= tier.visibleSlotCount()))
            throw new JsonParseException("Slot unavailable at recipe tier");
        auxiliary = Map.copyOf(aux);
        keep = Set.copyOf(kept);
        var relayList = new ArrayList<RelayRequirement>();
        var indices = new HashSet<Integer>();
        if (json.has("relay"))
            for (var element : json.getAsJsonArray("relay")) {
                var r = element.getAsJsonObject();
                int i = RecipeJson.integer(r, "index", 0, 0, 127);
                if (!indices.add(i)) throw new JsonParseException("Duplicate relay requirement");
                relayList.add(
                        new RelayRequirement(
                                i,
                                MaterialSpec.parse(r.get("ingredient")),
                                RecipeJson.decimal(r, "bind_at", 0.25, 0, 0.5)));
            }
        if (!relayList.isEmpty() && !tier.supports(AsterismTier.TRAIT))
            throw new JsonParseException("Relay needs trait tier");
        relays = List.copyOf(relayList);
        var set = new HashSet<String>();
        if (json.has("flags"))
            json.getAsJsonArray("flags")
                    .forEach(v -> set.add(v.getAsString().toLowerCase(Locale.ROOT)));
        flags = Set.copyOf(set);
        var phaseList = new ArrayList<PhaseSpec>();
        if (json.has("phases"))
            for (var phase : json.getAsJsonArray("phases"))
                phaseList.add(PhaseSpec.parse(phase.getAsJsonObject()));
        if (phaseList.size() > 128) throw new JsonParseException("Too many phases");
        phaseList.sort(Comparator.comparingDouble(PhaseSpec::fraction));
        phases = List.copyOf(phaseList);
        lumen = RecipeJson.amount(json, "lumen", 700);
        duration = RecipeJson.integer(json, "duration", 100, 1, 1000000);
        focus = json.has("focus_sign") ? RecipeJson.id(json.get("focus_sign").getAsString()) : null;
        advancement =
                json.has("required_advancement")
                        ? RecipeJson.id(json.get("required_advancement").getAsString())
                        : null;
    }

    private static void parseSlots(
            JsonObject json,
            String key,
            int offset,
            String[] names,
            Map<Integer, MaterialSpec> slots,
            Set<Integer> kept) {
        var object = RecipeJson.object(json, key);
        for (var entry : object.entrySet()) {
            int i = Arrays.asList(names).indexOf(entry.getKey());
            if (i < 0) throw new JsonParseException("Unknown " + key + " slot " + entry.getKey());
            var value = entry.getValue();
            if (value.isJsonObject() && value.getAsJsonObject().has("ingredient")) {
                var wrapper = value.getAsJsonObject();
                if (!RecipeJson.flag(wrapper, "consume", true)) kept.add(offset + i);
                value = wrapper.get("ingredient");
            }
            slots.put(offset + i, MaterialSpec.parse(value));
        }
    }

    public List<PhaseSpec> phases() {
        return phases;
    }

    public AsterismTier tier() {
        return tier;
    }

    public long lumen() {
        return lumen;
    }

    public List<RelayRequirement> relays() {
        return relays;
    }

    public boolean flag(String flag) {
        return flags.contains(flag);
    }

    public boolean consume(int slot) {
        return !keep.contains(slot);
    }

    public Optional<ResourceLocation> focusSign() {
        return Optional.ofNullable(focus);
    }

    public boolean gates(AsterismRecipeInput input, Level level) {
        return (!flag("night_only") || level.isNight())
                && (focus == null || input.focused(focus))
                && (advancement == null || input.unlocked(advancement));
    }

    public Optional<Map<Integer, MaterialSpec>> matchedSlots(Container input) {
        var match = grid.match(input);
        if (match.isEmpty()) return Optional.empty();
        for (int slot = 9; slot < 25; slot++) {
            var spec = auxiliary.get(slot);
            if (spec == null ? !input.getItem(slot).isEmpty() : !spec.test(input.getItem(slot)))
                return Optional.empty();
        }
        var slots = new TreeMap<>(match.get());
        slots.putAll(auxiliary);
        return Optional.of(Map.copyOf(slots));
    }

    @Override
    public boolean matches(Container input, Level level) {
        return input instanceof AsterismRecipeInput ctx
                && ctx.tier().supports(tier)
                && ctx.lumen() >= lumen
                && gates(ctx, level)
                && matchedSlots(input).isPresent();
    }

    @Override
    public ItemStack assemble(Container input, RegistryAccess access) {
        if (flag("no_item_output")) return ItemStack.EMPTY;
        var matched =
                grid.match(input).orElseThrow(() -> new IllegalStateException("Grid changed"));
        return result.assemble(
                slot -> {
                    if (slot >= 9)
                        return slot < input.getContainerSize()
                                ? input.getItem(slot)
                                : ItemStack.EMPTY;
                    var logical = grid.display().get(slot);
                    if (logical == null) return ItemStack.EMPTY;
                    return matched.entrySet().stream()
                            .filter(e -> e.getValue() == logical)
                            .findFirst()
                            .map(e -> input.getItem(e.getKey()))
                            .orElse(ItemStack.EMPTY);
                });
    }

    @Override
    public Map<Integer, MaterialSpec> displaySlots() {
        var slots = new TreeMap<>(grid.display());
        slots.putAll(auxiliary);
        return Map.copyOf(slots);
    }

    @Override
    public int displayDuration() {
        return duration;
    }

    @Override
    public Map<String, Number> displayScalars() {
        return Map.of("lumen", lumen, "duration", duration);
    }

    @Override
    public int priority() {
        return tier.ordinal();
    }

    @Override
    public boolean hidden() {
        return flag("ignore_jei") || super.hidden();
    }

    @Override
    public ResourceLocation displayCategory() {
        return new ResourceLocation(
                "stellaeomphalos", "asterism_" + tier.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public List<ResourceLocation> catalysts() {
        return List.of(
                new ResourceLocation(
                        "stellaeomphalos",
                        "asterism_altar_" + tier.name().toLowerCase(Locale.ROOT)));
    }
}
