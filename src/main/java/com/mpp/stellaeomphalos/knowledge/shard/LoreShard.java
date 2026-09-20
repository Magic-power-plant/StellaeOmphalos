package com.mpp.stellaeomphalos.knowledge.shard;

import com.mpp.stellaeomphalos.knowledge.research.*;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Predicate;

public record LoreShard(
        ResourceLocation id,
        String nameKey,
        String ribbonKey,
        String bodyKey,
        CodexGate visibility,
        CodexGate discovery,
        String surface,
        String boundSign) {
    public boolean localized(Predicate<String> probe) {
        return probe.test(nameKey) && probe.test(ribbonKey) && probe.test(bodyKey);
    }

    public boolean discoverable(GateContext context) {
        return visibility.evaluate(context).active() && discovery.evaluate(context).active();
    }

    public List<Integer> moonPhases(long seed) {
        var random = new java.util.SplittableRandom(seed ^ id.toString().hashCode());
        var phases = new LinkedHashSet<Integer>();
        int count = 2 + random.nextInt(2);
        while (phases.size() < count) phases.add(random.nextInt(8));
        return List.copyOf(phases);
    }
}
