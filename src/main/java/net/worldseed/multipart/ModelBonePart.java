package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.events.EntityControlEvent;
import net.worldseed.multipart.events.EntityDismountEvent;
import net.worldseed.multipart.events.EntityInteractEvent;
import net.worldseed.multipart.mount.MobRidable;

non-sealed class ModelBonePart extends ModelBoneGeneric {
    private final ModelEngine.RenderType renderType;
    private Pos lastPos = Pos.ZERO;

    public ModelBonePart(Point pivot, String name, Point rotation, GenericModel model, ModelEngine.RenderType renderType, LivingEntity forwardTo) {
        super(pivot, name, rotation, model);

        this.renderType = renderType;

        if (this.offset != null) {
            if (renderType == ModelEngine.RenderType.ZOMBIE) {
                this.stand = new LivingEntity(EntityType.ZOMBIE) {
                    @Override
                    public void tick(long time) {}
                };
            } else if (renderType == ModelEngine.RenderType.ARMOUR_STAND) {
                this.stand = new LivingEntity(EntityType.ARMOR_STAND) {
                    @Override
                    public void tick(long time) {}
                };
            }

            this.stand.setTag(Tag.String("WSEE"), "hitbox");
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

    public void spawn(Instance instance, Point position) {
        if (this.offset != null) {
            this.stand.setHelmet(items.get("normal"));
            this.stand.setNoGravity(true);
            this.stand.setSilent(true);
            this.stand.setInvisible(true);

            this.stand.setInstance(instance, position);
        }
    }

    public void setBoneRotation(Point rotation) {
        ArmorStandMeta meta = (ArmorStandMeta) this.stand.getEntityMeta();

        meta.setHeadRotation(new Vec(
                rotation.x(),
                0,
                -rotation.z()
        ));
    }

    public void draw(short tick) {
        this.children.forEach(bone -> bone.draw(tick));
        if (this.offset == null) return;

        Point p = this.offset.sub(0, 1.6, 0);
        p = applyTransform(p, tick);
        p = applyGlobalRotation(p);

        Pos endPos = Pos.fromPoint(p);

        Quaternion q = calculateFinalAngle(new Quaternion(getRotation(tick)), tick);
        if (model.getGlobalRotation() != 0) {
            Quaternion pq = new Quaternion(new Vec(0, this.model.getGlobalRotation(), 0));
            q = pq.multiply(q);
        }

        Point rotation = q.toEulerYZX();

        if (renderType == ModelEngine.RenderType.ARMOUR_STAND) {
            Pos newPos =
                    endPos
                            .div(6.4, 6.4, 6.4)
                            .add(model.getPosition())
                            .add(model.getGlobalOffset());

            Pos diff = newPos.sub(lastPos).mul(0.5);
            this.lastPos = newPos;
            stand.teleport(newPos.add(diff).withYaw((float) -rotation.y()));
            setBoneRotation(rotation);
        } else if (renderType == ModelEngine.RenderType.ZOMBIE) {
            // TODO: I think this sends two packets?
            stand.setView((float) -rotation.y(), (float) rotation.x());

            Pos newPos = endPos
                    .div(6.4, 6.4, 6.4)
                    .add(model.getPosition())
                    .add(0, -1.4, 0)
                    .add(model.getGlobalOffset());

            stand.teleport(newPos.withView((float) -rotation.y(), (float) rotation.x()));
        } else {
            throw new RuntimeException("Unknown render type..... If you see this message something went horribly wrong");
        }
    }

    @Override
    public void setState(String state) {
        if (this.stand != null) {
            this.stand.setHelmet(this.items.get(state));
        }
    }
}
