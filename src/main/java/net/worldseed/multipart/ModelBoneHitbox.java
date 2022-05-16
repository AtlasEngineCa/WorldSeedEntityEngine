package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.metadata.other.SlimeMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;

non-sealed class ModelBoneHitbox extends ModelBoneGeneric {
    public ModelBoneHitbox(Point pivot, String name, Point rotation, GenericModel model, Entity forwardTo) {
        super(pivot, name, rotation, model);

        String[] spl = name.split("_");
        int size = Integer.parseInt(spl[1]);

        if (this.offset != null) {
            this.stand = new LivingEntity(EntityType.SLIME);

            SlimeMeta meta = (SlimeMeta) this.stand.getEntityMeta();
            meta.setSize(size);

            this.stand.setTag(Tag.Integer("ForwardAttack"), forwardTo.getEntityId());
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
                .add(0, stand.getBoundingBox().maxY()/2, 0)
                .add(model.getGlobalOffset()));
    }
}
