package net.worldseed.resourcepack.multipart.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelParserUvTest {
    @Test
    void clampsUvEndpointsToMinecraftModelBounds() {
        var converted = ModelParser.convertUV(
                new ModelParser.UV(-8, 127, 48, 2, "0", 0),
                128, 128, false);

        assertEquals(0.0, converted.x1());
        assertEquals(15.875, converted.y1());
        assertEquals(5.0, converted.x2());
        assertEquals(16.0, converted.y2());
    }

    @Test
    void preservesClampedEndpointsWhenUvIsInverted() {
        var converted = ModelParser.convertUV(
                new ModelParser.UV(-8, 127, 48, 2, "0", 0),
                128, 128, true);

        assertEquals(5.0, converted.x1());
        assertEquals(16.0, converted.y1());
        assertEquals(0.0, converted.x2());
        assertEquals(15.875, converted.y2());
    }
}
