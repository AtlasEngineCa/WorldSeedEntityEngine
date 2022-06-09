package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.entity.metadata.other.SlimeMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.events.EntityDismountEvent;
import net.worldseed.multipart.events.EntityControlEvent;
import net.worldseed.multipart.events.EntityInteractEvent;
import net.worldseed.multipart.mount.MobRidable;

non-sealed class ModelBoneHitbox extends ModelBoneGeneric {
    public ModelBoneHitbox(Point pivot, String name, Point rotation, GenericModel model, LivingEntity forwardTo) {
        super(pivot, name, rotation, model);

        String[] spl = name.split("_");
        int size = Integer.parseInt(spl[1]);

        if (this.offset != null) {
            this.stand = new LivingEntity(EntityType.SLIME) {
                @Override
                public void tick(long time) {}
            };

            this.stand.setTag(Tag.String("WSEE"), "hitbox");

            SlimeMeta meta = (SlimeMeta) this.stand.getEntityMeta();
            meta.setSize(size);

            this.stand.eventNode().addListener(EntityDamageEvent.class, (event -> {
                event.setCancelled(true);

                if (forwardTo != null) {
                    if (event.getDamageType() instanceof EntityDamage source) {
                        forwardTo.damage(DamageType.fromEntity(source.getSource()), event.getDamage());
                    }
                }
            }));

            this.stand.eventNode().addListener(EntityDismountEvent.class, (event -> {
                model.dismountEntity(event.getRider());
            }));

            this.stand.eventNode().addListener(EntityControlEvent.class, (event -> {
                if (forwardTo instanceof MobRidable rideable) {
                    rideable.getControlGoal().setForward(event.getForward());
                    rideable.getControlGoal().setSideways(event.getSideways());
                    rideable.getControlGoal().setJump(event.getJump());
                }
            }));

            this.stand.eventNode().addListener(EntityInteractEvent.class, (event -> {
                if (forwardTo != null) {
                    EntityInteractEvent entityInteractEvent = new EntityInteractEvent(forwardTo, event.getInteracted());
                    EventDispatcher.call(entityInteractEvent);
                }
            }));
        }
    }

    @Override
    public void setState(String state) {}

    public void spawn(Instance instance, Point position) {
        if (this.offset != null) {
            this.stand.setInvisible(true);
            this.stand.setNoGravity(true);
            this.stand.setSilent(true);

            this.stand.setInstance(instance, position);
        }
    }

    public void draw(short tick) {
        this.children.forEach(bone -> bone.draw(tick));
        if (this.offset == null) return;

        Point p = this.offset;
        p = applyTransform(p, tick);
        p = applyGlobalRotation(p);

        Pos endPos = Pos.fromPoint(p);

        stand.teleport(
            endPos
                .div(6.4, 6.4, 6.4)
                .add(model.getPosition())
                .add(model.getGlobalOffset()));
    }
}
