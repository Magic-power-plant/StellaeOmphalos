package com.mpp.stellaeomphalos.core.util.collections;

/** Exposes a domain value without prescribing token lifetime or storage. */
@FunctionalInterface
public interface ValueToken<T> { T value(); }
