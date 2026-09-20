package com.mpp.stellaeomphalos.knowledge;

import com.mpp.stellaeomphalos.knowledge.codex.KnowledgeCatalog;
import com.mpp.stellaeomphalos.knowledge.shard.ShardPool;

import java.util.*;

/** Executed in fresh JVMs with different locales by ShardProcessTest. */
public final class ShardDeterminismProbe {
    public static void main(String[] args) {
        Locale.setDefault(Locale.forLanguageTag(args[0]));
        KnowledgeCatalog.initialize();
        var pool = new ArrayList<>(KnowledgeCatalog.SHARDS.all());
        if (args[0].startsWith("zh")) Collections.reverse(pool);
        for (long seed = 0; seed < 32; seed++)
            System.out.println("SHARD=" + ShardPool.resolve(seed, pool).orElseThrow().id());
    }
}
