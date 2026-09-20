package com.mpp.stellaeomphalos.lumen.capability;

import java.util.List;
import net.minecraft.core.Direction;

/** Splitting node (lens/prism): distributes input across outputs by weight. */
public interface LumenPrism extends LumenNode {
    /** Current split weights by output face; an empty list means lossless pass-through. */
    List<Double> splitWeights(Direction from);
}
