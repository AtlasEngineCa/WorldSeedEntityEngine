package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.LivingEntity;
import net.worldseed.multipart.animations.AnimationLoader;
import net.worldseed.multipart.animations.ModelAnimation;

class ModelBoneHeadArmourStand extends ModelBonePartArmourStand implements ModelBoneHead {
    private double headRotation;

    public ModelBoneHeadArmourStand(Point pivot, String name, Point rotation, GenericModel model, ModelEngine.RenderType renderType, LivingEntity forwardTo) {
        super(pivot, name, rotation, model, renderType, forwardTo);
    }

    @Override
    public Point getRotation(short tick) {
        Point netTransform = Vec.ZERO;

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == AnimationLoader.AnimationType.ROTATION) {
                    Point calculatedTransform = currentAnimation.getTransform(tick);
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
