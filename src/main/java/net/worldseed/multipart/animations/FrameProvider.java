package net.worldseed.multipart.animations;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

public interface FrameProvider {
    Point RotationMul = new Vec(-1, -1, 1);
    Point TranslationMul = new Vec(-1, 1, 1);

    Point getFrame(int tick);
}
