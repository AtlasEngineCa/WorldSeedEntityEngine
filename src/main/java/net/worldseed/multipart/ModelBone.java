package net.worldseed.multipart;

import net.minestom.server.entity.Entity;
import net.worldseed.multipart.animations.ModelAnimation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
sealed public interface ModelBone permits ModelBoneGeneric {
    Point applyTransform(Point p);
    Point getRotation();
    Point simulateRotation(String animation, int time);
    void draw();
    void setParent(ModelBone parent);
    void addChild(ModelBone child);
    void spawn(Instance instance, Point position);
    void addAnimation(ModelAnimation animation);
    ModelBone getParent();
    void setState(String state);
    Point calculateRotation(Point p, Point rotation, Point pivot);
    String getName();
    void destroy();
    Quaternion calculateFinalAngle(Quaternion q);
    Entity getEntity();
    Point simulateTransform(Point p, String animation, int time);
    Point getOffset();
}