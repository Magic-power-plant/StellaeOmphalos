package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoonStatReaderTest {

    private static int sequence;

    private static BoonAttribute attribute() {
        return BoonAttributeRegistry.register(new BoonAttribute(
                new ResourceLocation("stellaeomphalos", "test_reader_" + (++sequence)), 1.0,
                BoonAttributeClamp.UNBOUNDED, false));
    }

    @Test void flatReaderShowsRawValue() {
        var attribute = attribute();
        var line = new FlatStatReader(attribute, "").format(2.5);
        assertSame(attribute, line.attribute());
        assertEquals("boon_attribute.stellaeomphalos." + attribute.id().getPath(), line.nameKey());
        assertEquals(2.5, line.value());
        assertEquals("", line.suffix());
    }

    @Test void additivePercentReaderScalesByHundred() {
        var line = new AdditivePercentStatReader(attribute()).format(0.05);
        assertEquals(5.0, line.value(), 1e-9);
        assertEquals("%", line.suffix());
    }

    @Test void multiplicativePercentReaderShowsGainOverOne() {
        var reader = new MultiplicativePercentStatReader(attribute());
        assertEquals(50.0, reader.format(1.5).value(), 1e-9);
        assertEquals(0.0, reader.format(1.0).value(), 1e-9);
        assertEquals("%", reader.format(2.0).suffix());
    }

    @Test void harvestSpeedReaderShowsFlatMultiplier() {
        var line = new HarvestSpeedStatReader(attribute()).format(1.5);
        assertEquals(1.5, line.value(), 1e-9);
        assertEquals("x", line.suffix());
    }

    @Test void vanillaReaderShowsFlatValue() {
        var owner = new VanillaBoonAttribute("test_reader_vanilla", 0.0, List.of()) {};
        assertEquals(20.0, new VanillaStatReader(owner).format(20.0).value(), 1e-9);
    }

    @Test void registryRejectsUnknownAttributesAndOverwrite() {
        var attribute = attribute();
        var reader = new FlatStatReader(attribute);
        BoonStatReaderRegistry.register(reader);
        assertSame(reader, BoonStatReaderRegistry.readerFor(attribute));
        assertThrows(IllegalStateException.class, () -> BoonStatReaderRegistry.register(new FlatStatReader(attribute)));
        var unknown = new BoonAttribute(new ResourceLocation("stellaeomphalos", "test_reader_unknown"), 0.0,
                BoonAttributeClamp.UNBOUNDED, false);
        assertThrows(IllegalArgumentException.class, () -> BoonStatReaderRegistry.register(new FlatStatReader(unknown)));
    }

    @Test void unregisteredReaderLookupFallsBackToFlat() {
        var attribute = attribute();
        var reader = BoonStatReaderRegistry.readerFor(attribute);
        assertInstanceOf(FlatStatReader.class, reader);
        assertEquals(3.0, reader.format(3.0).value(), 1e-9);
    }

    @Test void baselineIsAttributeDefault() {
        var attribute = attribute();
        assertEquals(1.0, new FlatStatReader(attribute).baseline());
    }
}
