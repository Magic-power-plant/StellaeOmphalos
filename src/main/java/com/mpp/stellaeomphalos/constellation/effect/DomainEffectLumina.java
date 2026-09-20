package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainEffect;
import com.mpp.stellaeomphalos.constellation.domain.DomainEffectRunner;
import com.mpp.stellaeomphalos.constellation.domain.DomainEffectStatus;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.domain.DomainSpawnDeny;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;

import javax.annotation.Nullable;

/**
 * lucerna — lumina. Status-type effect: no strength consumption. Every 80 ticks renews a very large
 * spawn-deny token (radius 64 + mirrors x 64; token cap 400 ticks, rebuilt on radius change).
 * Corrupted: winds the night backwards, lengthening darkness.
 */
public final class DomainEffectLumina extends DomainEffect implements DomainEffectStatus {
    private static final int RENEW_INTERVAL = 80;
    private static final long REWIND_TICKS = 60;

    public DomainEffectLumina(@Nullable MajorSign owner) {
        super(owner);
    }

    @Override
    public DomainProperties provideProperties(int mirrorCount) {
        return new DomainProperties(64.0 + mirrorCount * 64.0, 1.0, 1.0, false, 0.0, 1.0);
    }

    @Override
    public void suspend(DomainContext ctx) {
        DomainSpawnDeny.revoke(ctx.level(), ctx.origin());
    }

    @Override
    public boolean isActive(DomainContext ctx) {
        return DomainEffectRunner.enabled();
    }

    @Override
    public boolean play(DomainContext ctx, float strength, DomainProperties props) {
        var level = ctx.level();
        if (props.corrupted()) {
            // Corrupted: rewind night time (doDaylightCycle honored — nothing to rewind when
            // frozen).
            if (level.isNight()
                    && level.getGameRules()
                            .getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)) {
                level.setDayTime(level.getDayTime() - REWIND_TICKS);
                return true;
            }
            return false;
        }
        if (level.getGameTime() % RENEW_INTERVAL == 0) {
            DomainSpawnDeny.renew(level, ctx.origin(), props.size(), RENEW_INTERVAL * 2L);
            return true;
        }
        return false;
    }
}
