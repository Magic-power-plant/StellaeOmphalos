package com.mpp.stellaeomphalos.player.progress;

/** Invalid records reject every mutation, including first-join rewards and shared knowledge. */
public final class FakeStarRecord extends StarRecord {
    public FakeStarRecord() {
        super(false);
    }
}
