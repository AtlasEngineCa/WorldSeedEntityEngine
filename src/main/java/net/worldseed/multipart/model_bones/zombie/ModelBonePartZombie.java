package net.worldseed.multipart.model_bones.zombie;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.metadata.monster.zombie.ZombieMeta;
import net.minestom.server.item.ItemStack;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;

public class ModelBonePartZombie extends ModelBoneImpl implements ModelBoneViewable {
    private final Pos SMALL_SUB = new Pos(0, 0.76, 0);
    private final Pos NORMAL_SUB = new Pos(0, 1.5, 0);

    public ModelBonePartZombie(Point pivot, String name, Point rotation, GenericModel model, ModelConfig config, LivingEntity forwardTo) {
        super(pivot, name, rotation, model);

        if (this.offset != null) {
            this.stand = new LivingEntity(EntityType.ZOMBIE) {
                @Override
                public void tick(long time) {}
            };

            if (config.size() == ModelConfig.Size.SMALL) {
                ZombieMeta meta = (ZombieMeta) this.stand.getEntityMeta();
                meta.setBaby(true);
            }

            stand.setInvisible(true);
            ModelBoneImpl.hookPart(this, forwardTo);
        }
    }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;
        var rotation = calculateRotation();

        Point p = this.offset;
        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        Pos endPos = Pos.fromPoint(p);

        double divisor = model.config().size() == ModelConfig.Size.SMALL ? 1.426 : 1;

        Pos sub = model.config().size() == ModelConfig.Size.SMALL
                ? SMALL_SUB : NORMAL_SUB;

        return endPos
                .div(6.4, 6.4, 6.4)
                .div(divisor)
                .add(model.getPosition())
                .sub(sub)
                .add(model.getGlobalOffset())
                .withView((float) -rotation.y(), (float) rotation.x());
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
        Quaternion pq = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
        q = pq.multiply(q);

        return q.toEulerYZX();
    }

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        var position = calculatePosition();

        // TODO: I think this sends two packets?
        stand.setView(position.yaw(), position.pitch());
        stand.teleport(position);
    }

    @Override
    public void setState(String state) {
        if (this.stand != null && this.stand instanceof LivingEntity e) {
            if (state.equals("invisible")) {
                e.setHelmet(ItemStack.AIR);
                return;
            }

            var item = this.items.get(state);
            if (item != null) {
                e.setHelmet(item);
            }
        }
    }
}
