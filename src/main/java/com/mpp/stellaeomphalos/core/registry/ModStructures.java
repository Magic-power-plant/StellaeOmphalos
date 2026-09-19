package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

public final class ModStructures {
    public static final RegistryFamily<StructureType<?>> TYPES = new RegistryFamily<>(Registries.STRUCTURE_TYPE);
    public static final RegistryFamily<StructurePieceType> PIECES = new RegistryFamily<>(Registries.STRUCTURE_PIECE);
    public static final RegistryFamily<StructureProcessorType<?>> PROCESSORS = new RegistryFamily<>(Registries.STRUCTURE_PROCESSOR);
    private ModStructures() {}
}
