package com.mpp.stellaeomphalos.structure.pattern;

public interface PatternBlueprint extends StructureBlueprint {
    default PatternBlueprint transformed(PlacementTransform t) {
        var actual = mirrorable() ? t : t.withoutMirror();
        var b = PatternBlueprintBuilder.named(id()).mirrorable(mirrorable()).noPaste(noPaste());
        blocks().values().forEach(p -> b.put(p.transform(actual)));
        uniqueSlots().forEach(p -> b.unique(actual.apply(p)));
        return b.build();
    }
}
