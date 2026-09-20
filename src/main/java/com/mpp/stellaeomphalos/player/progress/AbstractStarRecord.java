package com.mpp.stellaeomphalos.player.progress;

import com.mpp.stellaeomphalos.core.platform.StarRecordView;

/** Main-thread entity lifecycle: live records mutate, invalid or protected records cannot. */
public abstract class AbstractStarRecord implements StarRecordView {
    private final boolean valid;
    private long revision;

    protected AbstractStarRecord(boolean valid) {
        this.valid = valid;
    }

    @Override
    public final boolean valid() {
        return valid;
    }

    @Override
    public final long revision() {
        return revision;
    }

    protected final boolean changed(boolean changed) {
        if (valid && changed) {
            revision++;
            return true;
        }
        return false;
    }
}
