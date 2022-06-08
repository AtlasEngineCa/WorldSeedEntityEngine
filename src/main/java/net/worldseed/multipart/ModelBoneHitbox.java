package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.other.SlimeMeta;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.events.EntityDismountEvent;
import net.worldseed.multipart.events.EntityMountEvent;

non-sealed class ModelBoneHitbox extends ModelBoneGeneric {
    public ModelBoneHitbox(Point pivot, String name, Point rotation, GenericModel model, LivingEntity forwardTo) {
        super(pivot, name, rotation, model);

        String[] spl = name.split("_");
        int size = Integer.parseInt(spl[1]);

        if (this.offset != null) {
            this.stand = new LivingEntity(EntityType.SLIME);
            this.stand.setTag(Tag.String("WSEE"), "hitbox");

            SlimeMeta meta = (SlimeMeta) this.stand.getEntityMeta();
            meta.setSize(size);

            this.stand.eventNode().addListener(EntityDamageEvent.class, (event -> {
                event.setCancelled(true);

                if (forwardTo != null)
                    forwardTo.damage(DamageType.fromEntity(event.getEntity()), event.getDamage());
            }));

            this.stand.eventNode().addListener(EntityMountEvent.class, (event -> {
                model.mountEntity(event.getRider());
            }));

            this.stand.eventNode().addListener(EntityDismountEvent.class, (event -> {
                model.dismountEntity(event.getRider());
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
