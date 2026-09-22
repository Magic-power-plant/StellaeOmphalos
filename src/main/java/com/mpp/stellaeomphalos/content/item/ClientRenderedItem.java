package com.mpp.stellaeomphalos.content.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Forge's required extension signature; installation is deferred until the client exists. */
public abstract class ClientRenderedItem extends Item {
    private Consumer<IClientItemExtensions> pending;
    private IClientItemExtensions extensions;

    protected ClientRenderedItem(Properties properties) {
        super(properties);
    }

    @Override
    public final void initializeClient(Consumer<IClientItemExtensions> consumer) {
        if (extensions == null) pending = consumer;
        else consumer.accept(extensions);
    }

    public final void installClientExtensions(IClientItemExtensions value) {
        extensions = java.util.Objects.requireNonNull(value);
        if (pending != null) {
            pending.accept(value);
            pending = null;
        }
    }
}
