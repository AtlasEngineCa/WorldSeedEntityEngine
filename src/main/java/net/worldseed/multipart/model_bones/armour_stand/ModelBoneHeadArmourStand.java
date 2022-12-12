package net.worldseed.multipart.model_bones.armour_stand;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.LivingEntity;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.model_bones.ModelBoneHead;
import net.worldseed.multipart.animations.AnimationLoader;
import net.worldseed.multipart.animations.ModelAnimation;
import net.worldseed.multipart.model_bones.ModelBoneViewable;

public class ModelBoneHeadArmourStand extends ModelBonePartArmourStand implements ModelBoneHead, ModelBoneViewable {
    private double headRotation;

    public ModelBoneHeadArmourStand(Point pivot, String name, Point rotation, GenericModel model, ModelConfig config, LivingEntity forwardTo) {
        super(pivot, name, rotation, model, config, forwardTo);
    }

    @Override
    public Point getPropogatedRotation() {
        Point netTransform = Vec.ZERO;

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == AnimationLoader.AnimationType.ROTATION) {
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
