package net.worldseed.multipart.model_bones.display_entity;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelLoader;
import net.worldseed.multipart.animations.BoneAnimation;
import net.worldseed.multipart.model_bones.ModelBoneViewable;

public class ModelBoneHeadDisplay extends ModelBonePartDisplay implements ModelBoneViewable {
    private double headRotation;

    public ModelBoneHeadDisplay(Point pivot, String name, Point rotation, GenericModel model, float scale) {
        super(pivot, name, rotation, model, scale);
    }

    @Override
    public Point getPropagatedRotation() {
        Point netTransform = Vec.ZERO;

        for (BoneAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == ModelLoader.AnimationType.ROTATION) {
                    Point calculatedTransform = currentAnimation.getTransform();
                    netTransform = netTransform.add(calculatedTransform);
                }
            }
        }

        return this.rotation.add(netTransform).add(0, this.headRotation, 0);
    }

    public void setRotation(double rotation) {
        this.headRotation = rotation;
    }
}
