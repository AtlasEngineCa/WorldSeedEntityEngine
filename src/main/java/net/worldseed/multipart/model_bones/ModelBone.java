package net.worldseed.multipart.model_bones;

import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.animations.BoneAnimation;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@ApiStatus.Internal
public interface ModelBone {
    CompletableFuture<Void> spawn(Instance instance, Pos position);
    Point applyTransform(Point p);
    void draw();
    void destroy();

    Point simulateTransform(Point p, String animation, int time);
    Point simulateRotation(String animation, int time);

    void setState(String state);
    void setParent(ModelBone parent);

    String getName();
    BoneEntity getEntity();
    Point getOffset();
    Point getPosition();
    ModelBone getParent();
    Point getPropogatedRotation();

    Point calculateRotation(Point p, Point rotation, Point pivot);
    Quaternion calculateFinalAngle(Quaternion q);
    Pos calculatePosition();
    Point calculateRotation();

    void addChild(ModelBone child);
    void addAnimation(BoneAnimation animation);

    void addViewer(Player player);
    void removeViewer(Player player);

    void setScale(float scale);

    void removeGlowing();
    void setGlowing(Color color);

    void attachModel(GenericModel model);
    List<GenericModel> getAttachedModels();
    void detachModel(GenericModel model);

    void setGlobalRotation(double rotation);
}