package net.worldseed.gestures;

import net.kyori.adventure.util.RGBLike;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModelBoneEmote extends ModelBoneImpl implements ModelBoneViewable {
    private final Double verticalOffset;

    public ModelBoneEmote(Point pivot, String name, Point rotation, GenericModel model, int translation, Double verticalOffset, PlayerSkin skin) {
        super(pivot, name, rotation, model, 1);

        this.verticalOffset = verticalOffset;

        if (this.offset != null) {
            this.stand = new BoneEntity(EntityType.ITEM_DISPLAY, model, name);
            this.stand.editEntityMeta(ItemDisplayMeta.class, meta -> {
                meta.setViewRange(10000);
                meta.setTransformationInterpolationDuration(2);
                meta.setPosRotInterpolationDuration(2);
                meta.setTranslation(new Vec(0, translation, 0));
                meta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_RIGHT_HAND);

                meta.setItemStack(ItemStack.builder(Material.PLAYER_HEAD)
                        .set(ItemComponent.PROFILE, new HeadProfile(skin))
                        .set(ItemComponent.CUSTOM_MODEL_DATA, customModelDataFromName(name))
                        .build()
                );
            });
        }

        switch (this.name) {
            case "Head" -> {
                this.diff = this.pivot.add(0, 0, 0);
            }
            case "RightArm" -> {
                this.diff = this.pivot.add(-1.17, 0, 0);
            }
            case "LeftArm" -> {
                this.diff = this.pivot.add(1.17, 0, 0);
            }
            case "RightLeg" -> {
                this.diff = this.pivot.add(-0.4446, 0, 0);
            }
            case "LeftLeg" -> {
                this.diff = this.pivot.add(0.4446, 0, 0);
            }
            case "Body" -> {
                this.diff = this.pivot.add(0, 0, 0);
            }
        }
    }

    @Override
    public CompletableFuture<Void> spawn(Instance instance, Pos position) {
        var correctLocation = (180 + this.model.getGlobalRotation() + 360) % 360;
        return super.spawn(instance, Pos.fromPoint(position).withYaw((float) correctLocation)).whenCompleteAsync((v, e) -> {
            if (e != null) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        if (this.stand != null) {
            var scale = calculateScale();
            var position = calculatePosition();

            if (this.stand.getEntityMeta() instanceof ItemDisplayMeta meta) {
                Quaternion q = new Quaternion(calculateRotation());

                meta.setNotifyAboutChanges(false);
                meta.setTransformationInterpolationStartDelta(0);
                meta.setScale(new Vec(scale.x() * this.scale, scale.y() * this.scale, scale.z() * this.scale));
                meta.setRightRotation(new float[]{(float) q.x(), (float) q.y(), (float) q.z(), (float) q.w()});
                meta.setNotifyAboutChanges(true);

                this.stand.teleport(position.withView((float) 0, 0));
            }
        }
    }

    @Override
    public Pos calculatePosition() {
        Point p = this.offset == null ? Pos.ZERO : this.offset;
        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        return Pos.fromPoint(p)
                .div(4, 4, 4).mul(scale)
                .add(model.getPosition())
                .add(0, verticalOffset, 0)
                .add(model.getGlobalOffset());
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = calculateFinalAngle(new Quaternion(getPropogatedRotation()));
        Quaternion pq = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
        q = pq.multiply(q);

        return q.toEuler();
    }

    @Override
    public Point calculateScale() {
        return Vec.ONE;
    }

    private int customModelDataFromName(String name) {
        return switch (name) {
            case "Head" -> 1;
            case "RightArm" -> 2;
            case "LeftArm" -> 3;
            case "Body" -> 4;
            case "RightLeg" -> 5;
            case "LeftLeg" -> 6;
            case "slim_right" -> 7;
            case "slim_left" -> 8;
            default -> 0;
        };
    }

    @Override
    public void setState(String state) {
        throw new UnsupportedOperationException("Cannot set state on an emote");
    }

    @Override
    public Point getPosition() {
        return calculatePosition();
    }

    @Override
    public void addViewer(Player player) {
        if (this.stand != null) this.stand.addViewer(player);
    }

    @Override
    public void removeViewer(Player player) {
        if (this.stand != null) this.stand.removeViewer(player);
    }

    @Override
    public void removeGlowing() {
        if (this.stand != null) this.stand.setGlowing(false);
    }

    @Override
    public void setGlowing(RGBLike color) {
        if (this.stand != null) this.stand.setGlowing(true);
    }

    @Override
    public void removeGlowing(Player player) {

    }

    @Override
    public void setGlowing(Player player, RGBLike color) {

    }

    @Override
    public void attachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot attach a model to this bone type");
    }

    @Override
    public List<GenericModel> getAttachedModels() {
        return List.of();
    }

    @Override
    public void detachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot detach a model from this bone type");
    }

    @Override
    public @NotNull Collection<ModelBone> getChildren() {
        return List.of();
    }

    @Override
    public void setGlobalRotation(double yaw, double pitch) {
    }
}
