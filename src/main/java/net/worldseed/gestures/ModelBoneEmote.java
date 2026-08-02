package net.worldseed.gestures;

import net.kyori.adventure.util.RGBLike;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomModelData;
import net.minestom.server.network.player.ResolvableProfile;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModelBoneEmote extends ModelBoneImpl implements ModelBoneViewable {
    private static final Quaternion RIGHT_HAND_ROTATION = new Quaternion(new Vec(-90, 0, 0));
    private static final Quaternion LEFT_HAND_ROTATION = RIGHT_HAND_ROTATION;
    private static final double PLAYER_RENDER_SCALE = 15.0 / 16.0;
    private static final Point RIGHT_HAND_GRIP = new Vec(
            PLAYER_RENDER_SCALE / 16.0, -PLAYER_RENDER_SCALE * 10.0 / 16.0, -PLAYER_RENDER_SCALE * 2.0 / 16.0);
    private static final Point LEFT_HAND_GRIP = new Vec(
            -PLAYER_RENDER_SCALE / 16.0, -PLAYER_RENDER_SCALE * 10.0 / 16.0, -PLAYER_RENDER_SCALE * 2.0 / 16.0);
    private final Double verticalOffset;
    private final BoneEntity heldItemStand;
    private final Point heldItemGrip;
    private final Quaternion heldItemRotation;

    public ModelBoneEmote(Point pivot, String name, Point rotation, GenericModel model, int translation, Double verticalOffset, PlayerSkin skin) {
        super(pivot, name, rotation, model, 1);

        this.verticalOffset = verticalOffset;

        boolean rightArm = name.equals("RightArm");
        boolean leftArm = name.equals("LeftArm");
        this.heldItemGrip = rightArm ? RIGHT_HAND_GRIP : leftArm ? LEFT_HAND_GRIP : null;
        this.heldItemRotation = rightArm ? RIGHT_HAND_ROTATION : leftArm ? LEFT_HAND_ROTATION : null;
        if (rightArm || leftArm) {
            this.heldItemStand = new BoneEntity(EntityType.ITEM_DISPLAY, model, name + "HeldItem");
            this.heldItemStand.editEntityMeta(ItemDisplayMeta.class, meta -> {
                meta.setViewRange(10000);
                meta.setTransformationInterpolationDuration(2);
                meta.setPosRotInterpolationDuration(2);
                meta.setScale(new Vec(PLAYER_RENDER_SCALE));
                meta.setDisplayContext(rightArm
                        ? ItemDisplayMeta.DisplayContext.THIRDPERSON_RIGHT_HAND
                        : ItemDisplayMeta.DisplayContext.THIRDPERSON_LEFT_HAND);
                meta.setRightRotation(new float[]{
                        (float) heldItemRotation.x(), (float) heldItemRotation.y(),
                        (float) heldItemRotation.z(), (float) heldItemRotation.w()
                });
                meta.setItemStack(ItemStack.AIR);
            });
        } else {
            this.heldItemStand = null;
        }

        if (this.offset != null) {
            this.stand = new BoneEntity(EntityType.ITEM_DISPLAY, model, name);
            this.stand.editEntityMeta(ItemDisplayMeta.class, meta -> {
                meta.setViewRange(10000);
                meta.setTransformationInterpolationDuration(2);
                meta.setPosRotInterpolationDuration(2);
                meta.setTranslation(new Vec(0, translation, 0));
                meta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRDPERSON_RIGHT_HAND);

                meta.setItemStack(ItemStack.builder(Material.PLAYER_HEAD)
                        .set(DataComponents.PROFILE, new ResolvableProfile(skin))
                        .set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(customModelDataFromName(name)), List.of(), List.of(), List.of()))
                        .build()
                );
            });
        }

        switch (this.name) {
            case "Head", "Body" -> this.diff = this.pivot.add(0, 0, 0);
            case "RightArm" -> this.diff = this.pivot.add(-1.17, 0, 0);
            case "LeftArm" -> this.diff = this.pivot.add(1.17, 0, 0);
            case "RightLeg" -> this.diff = this.pivot.add(-0.4446, 0, 0);
            case "LeftLeg" -> this.diff = this.pivot.add(0.4446, 0, 0);
        }
    }

    @Override
    public CompletableFuture<Void> spawn(Instance instance, Pos position) {
        var correctLocation = (180 + this.model.getGlobalRotation() + 360) % 360;
        CompletableFuture<Void> bodySpawn = super.spawn(instance, new Pos(position).withYaw((float) correctLocation));
        CompletableFuture<Void> itemSpawn = heldItemStand == null
                ? CompletableFuture.completedFuture(null)
                : heldItemStand.setInstance(instance, calculateHeldItemPosition().withYaw((float) correctLocation));
        return CompletableFuture.allOf(bodySpawn, itemSpawn).whenCompleteAsync((_, e) -> {
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

        if (this.heldItemStand != null && this.heldItemStand.getEntityMeta() instanceof ItemDisplayMeta meta) {
            Quaternion q = new Quaternion(calculateRotation());
            meta.setNotifyAboutChanges(false);
            meta.setTransformationInterpolationStartDelta(0);
            meta.setTranslation(calculateHeldItemTranslation());
            meta.setLeftRotation(new float[]{(float) q.x(), (float) q.y(), (float) q.z(), (float) q.w()});
            meta.setNotifyAboutChanges(true);
            this.heldItemStand.teleport(calculateHeldItemPosition().withView(0, 0));
        }
    }

    @Override
    public Pos calculatePosition() {
        return calculatePosition(this.offset == null ? Pos.ZERO : this.offset);
    }

    private Pos calculateHeldItemPosition() {
        return calculatePosition();
    }

    private Point calculateHeldItemTranslation() {
        Quaternion rotation = new Quaternion(calculateRotation());
        return rotate(heldItemGrip.mul(scale), rotation);
    }

    private static Point rotate(Point point, Quaternion rotation) {
        Quaternion vector = new Quaternion(point.x(), point.y(), point.z(), 0);
        Quaternion inverse = new Quaternion(-rotation.x(), -rotation.y(), -rotation.z(), rotation.w());
        Quaternion result = rotation.multiply(vector).multiply(inverse);
        return new Vec(result.x(), result.y(), result.z());
    }

    private Pos calculatePosition(Point point) {
        Point p = point;
        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        return new Pos(p)
                .div(4, 4, 4).mul(scale)
                .add(model.getPosition())
                .add(0, verticalOffset, 0)
                .add(model.getGlobalOffset());
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = calculateFinalAngle(new Quaternion(getPropagatedRotation()));
        Quaternion pq = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
        q = pq.multiply(q);

        return q.toEuler();
    }

    @Override
    public Point calculateScale() {
        return Vec.ONE;
    }

    private float customModelDataFromName(String name) {
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
        if (this.heldItemStand != null) this.heldItemStand.addViewer(player);
    }

    @Override
    public void removeViewer(Player player) {
        if (this.stand != null) this.stand.removeViewer(player);
        if (this.heldItemStand != null) this.heldItemStand.removeViewer(player);
    }

    public void setHeldItem(ItemStack item) {
        if (this.heldItemStand == null) throw new IllegalStateException(name + " is not an arm bone");
        this.heldItemStand.editEntityMeta(ItemDisplayMeta.class, meta -> meta.setItemStack(item));
    }

    public ItemStack getHeldItem() {
        if (this.heldItemStand == null) return ItemStack.AIR;
        return ((ItemDisplayMeta) this.heldItemStand.getEntityMeta()).getItemStack();
    }

    BoneEntity getHeldItemEntity() {
        return this.heldItemStand;
    }

    @Override
    public void destroy() {
        if (this.heldItemStand != null) this.heldItemStand.remove();
        super.destroy();
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
    public void setGlobalRotation(double yaw, double pitch) {
    }
}
