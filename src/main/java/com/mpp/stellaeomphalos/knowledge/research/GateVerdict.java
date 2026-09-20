package com.mpp.stellaeomphalos.knowledge.research;

import java.util.List;

public record GateVerdict(GateLevel level, String blockedBy, List<String> satisfiedBy) {
    public GateVerdict {
        satisfiedBy = List.copyOf(satisfiedBy);
    }

    public boolean active() {
        return level == GateLevel.ACTIVE;
    }
}
