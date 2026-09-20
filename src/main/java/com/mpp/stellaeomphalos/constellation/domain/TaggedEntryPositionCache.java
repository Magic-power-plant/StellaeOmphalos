package com.mpp.stellaeomphalos.constellation.domain;

import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.TaggedTupleEntry;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Reserved extension point: a position cache whose entries carry a (key, value) NBT tuple —
 * the equivalent of the original position-map. Simplified per plan 2.2.6.4: no built-in user in
 * this part; subclasses supply typed codecs on top of the raw {@link CompoundTag} pair if needed.
 */
public abstract class TaggedEntryPositionCache extends DomainPositionCache<TaggedTupleEntry> {
    protected TaggedEntryPositionCache(@Nullable MajorSign owner, int cap, Predicate<BlockPos> verifier) {
        super(owner, cap, verifier, TaggedTupleEntry::new);
    }

    /** First entry whose key NBT equals the probe. */
    public Optional<TaggedTupleEntry> byKey(CompoundTag key) {
        return entries().stream().filter(entry -> entry.key().equals(key)).findFirst();
    }

    public boolean offer(BlockPos pos, CompoundTag key, CompoundTag value) {
        if (size() >= cap) return false;
        var existing = byKey(key);
        if (existing.isPresent()) { existing.get().set(key, value); return true; }
        var entry = new TaggedTupleEntry(pos);
        entry.set(key, value);
        return offer(entry);
    }
}
