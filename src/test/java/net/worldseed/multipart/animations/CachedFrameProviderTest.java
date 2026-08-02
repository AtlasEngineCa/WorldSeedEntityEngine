package net.worldseed.multipart.animations;

import net.minestom.server.coordinate.Vec;
import net.worldseed.multipart.ModelLoader;
import net.worldseed.multipart.mql.MQLPoint;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CachedFrameProviderTest {
    @Test
    void animationSecondsAdvanceAtTwentyFramesPerSecond() {
        var keys = new LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation>();
        keys.put(0.0, new BoneAnimationImpl.PointInterpolation(new MQLPoint(0, 0, 0), "linear"));
        keys.put(1.0, new BoneAnimationImpl.PointInterpolation(new MQLPoint(20, 0, 0), "linear"));
        var frames = new CachedFrameProvider(20, keys, ModelLoader.AnimationType.TRANSLATION);

        assertEquals(0, frames.getFrame(0).x(), 1e-9);
        // WSEE mirrors Blockbench X into Minecraft's coordinate convention.
        assertEquals(-2.5, frames.getFrame(10).x(), 1e-9);
        assertEquals(-5, frames.getFrame(20).x(), 1e-9);
        assertNotEquals(frames.getFrame(0), frames.getFrame(10));
    }

    @Test
    void emptyChannelsUseIdentityTransforms() {
        var empty = new LinkedHashMap<Double, BoneAnimationImpl.PointInterpolation>();
        var rotation = new CachedFrameProvider(20, empty,
                ModelLoader.AnimationType.ROTATION).getFrame(10);
        assertEquals(0, rotation.x(), 1e-9);
        assertEquals(0, rotation.y(), 1e-9);
        assertEquals(0, rotation.z(), 1e-9);
        assertEquals(Vec.ONE, new CachedFrameProvider(20, empty,
                ModelLoader.AnimationType.SCALE).getFrame(10));
    }
}
