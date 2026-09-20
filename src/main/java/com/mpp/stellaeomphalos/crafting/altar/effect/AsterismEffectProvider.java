package com.mpp.stellaeomphalos.crafting.altar.effect;

/** Side-neutral capability; client implementations own their rendering resources. */
public interface AsterismEffectProvider {
    void tick(ClientCraftView view);

    void finish(ClientCraftView view);

    default void close() {}
}
