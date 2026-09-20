package com.mpp.stellaeomphalos.structure.pattern;

import java.util.List;

public interface BuildBlueprint extends PatternBlueprint {
    List<PlacementProcessor> postProcessors();
}
