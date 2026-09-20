package com.mpp.stellaeomphalos.constellation.sign;

import net.minecraft.world.level.Level;

/** A sign with its own show-up rule (e.g. tied to an eclipse) instead of the regular cycle. */
public interface AnomalousSign extends Sign {
    boolean doesShowUp(SignSkyState sky, Level level, long day);
    float distribution(SignSkyState sky, Level level, long day);
}
