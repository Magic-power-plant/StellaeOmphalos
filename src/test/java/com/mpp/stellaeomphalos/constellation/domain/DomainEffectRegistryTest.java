package com.mpp.stellaeomphalos.constellation.domain;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Registry contract tests on a probe id only. Initializing the twelve built-ins is verified in
 * DomainGameTests (their statics touch vanilla registries, unavailable in a plain JUnit JVM).
 */
class DomainEffectRegistryTest {
    private static final ResourceLocation TEST_ID = new ResourceLocation("stellaeomphalos", "registry_test_sign");
    private static final ResourceLocation RENDERER_ID = new ResourceLocation("stellaeomphalos", "registry_test_renderer");

    private static final class DummyEffect extends DomainEffect {
        DummyEffect() { super(null); }
        @Override public boolean play(DomainContext ctx, float strength, DomainProperties props) { return false; }
        @Override public DomainProperties provideProperties(int mirrorCount) { return new DomainProperties(1, 1, 1, false, 0, 1); }
    }

    @Test void duplicateRegistrationThrows() {
        DomainEffectRegistry.register(TEST_ID, new DummyEffect());
        assertThrows(IllegalStateException.class, () -> DomainEffectRegistry.register(TEST_ID, new DummyEffect()));
        assertSame(DomainEffectRegistry.bySign(TEST_ID), DomainEffectRegistry.bySign(TEST_ID));
    }

    @Test void clientRendererDuplicatesThrow() {
        DomainEffectRegistry.registerClientRenderer(RENDERER_ID, new DummyEffect());
        assertThrows(IllegalStateException.class,
                () -> DomainEffectRegistry.registerClientRenderer(RENDERER_ID, new DummyEffect()));
    }
}
