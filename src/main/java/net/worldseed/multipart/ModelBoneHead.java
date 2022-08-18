package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.entity.metadata.monster.zombie.ZombieMeta;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.animations.AnimationLoader;
import net.worldseed.multipart.animations.ModelAnimation;
import net.worldseed.multipart.events.EntityControlEvent;
import net.worldseed.multipart.events.EntityDismountEvent;
import net.worldseed.multipart.events.EntityInteractEvent;
import net.worldseed.multipart.mount.MobRidable;

class ModelBoneHead extends ModelBonePart {
    private double headRotation;

    public ModelBoneHead(Point pivot, String name, Point rotation, GenericModel model, ModelEngine.RenderType renderType, LivingEntity forwardTo) {
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
