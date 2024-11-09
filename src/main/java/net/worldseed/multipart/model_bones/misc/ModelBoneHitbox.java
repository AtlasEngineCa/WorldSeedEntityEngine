package net.worldseed.multipart.model_bones.misc;

import com.google.gson.JsonArray;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntityTeleportPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.animations.BoneAnimation;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.bone_types.HitboxBone;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class ModelBoneHitbox extends ModelBoneImpl implements HitboxBone {
    private static final int INTERPOLATE_TICKS = 2;
    private static final Tag<String> WSEE = Tag.String("WSEE");
    private final JsonArray cubes;
    private final Collection<ModelBone> illegitimateChildren = new ConcurrentLinkedDeque<>();
    private final Point orgPivot;
    private Task positionTask;

    public ModelBoneHitbox(Point pivot, String name, Point rotation, GenericModel model, Point newOffset, double sizeX, double sizeY, JsonArray cubes, boolean parent, float scale) {
        super(pivot, name, rotation, model, scale);

        this.orgPivot = pivot;
        this.cubes = cubes;

        if (parent) {
            generateStands(cubes, pivot, name, rotation, model);
            this.offset = null;
        } else {
            if (this.offset != null) {
                this.stand = new BoneEntity(EntityType.INTERACTION, model, name) {
                    @Override
                    public void updateNewViewer(@NotNull Player player) {
                        super.updateNewViewer(player);

                        EntityTeleportPacket packet = new EntityTeleportPacket(this.getEntityId(), this.position, Vec.ZERO, 0, false);
                        player.getPlayerConnection().sendPacket(packet);
                    }

                    @Override
                    public void updateOldViewer(@NotNull Player player) {
                        super.updateOldViewer(player);
                    }
                };

                this.stand.setTag(WSEE, "hitbox");
                this.offset = newOffset;

                InteractionMeta meta = (InteractionMeta) this.stand.getEntityMeta();
                meta.setHeight((float) (sizeY / 4f) * scale);
                meta.setWidth((float) (sizeX / 4f) * scale);

                this.stand.setBoundingBox(sizeX / 4f * scale, sizeY / 4f * scale, sizeX / 4f * scale);
            }
        }
    }

    public void addViewer(Player player) {
        if (this.stand != null) this.stand.addViewer(player);
        illegitimateChildren.forEach(modelBone -> modelBone.addViewer(player));
    }

    public void removeViewer(Player player) {
        if (this.stand != null) this.stand.removeViewer(player);
        illegitimateChildren.forEach(modelBone -> modelBone.removeViewer(player));
    }

    @Override
    public void setGlobalScale(float scale) {
        super.setGlobalScale(scale);

        illegitimateChildren.forEach(ModelBone::destroy);
        this.illegitimateChildren.clear();

        generateStands(this.cubes, orgPivot, this.name, this.rotation, this.model);
        this.illegitimateChildren.forEach(modelBone -> {
            modelBone.spawn(model.getInstance(), model.getPosition());
            modelBone.setParent(getParent());
            model.getViewers().forEach(modelBone::addViewer);
        });
    }

    @Override
    public void removeGlowing() {
    }

    @Override
    public void setGlowing(RGBLike color) {
    }

    @Override
    public void removeGlowing(Player player) {
    }

    @Override
    public void setGlowing(Player player, RGBLike color) {
    }

    @Override
    public void attachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot attach a model to a hitbox");
    }

    @Override
    public List<GenericModel> getAttachedModels() {
        return List.of();
    }

    @Override
    public void detachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot detach a model from a hitbox");
    }

    @Override
    public void setGlobalRotation(double yaw, double pitch) {
    }

    public void generateStands(JsonArray cubes, Point pivotPos, String name, Point boneRotation, GenericModel genericModel) {
        for (var cube : cubes) {
            JsonArray sizeArray = cube.getAsJsonObject().get("size").getAsJsonArray();
            JsonArray origin = cube.getAsJsonObject().get("origin").getAsJsonArray();

            Point sizePoint = new Vec(sizeArray.get(0).getAsFloat(), sizeArray.get(1).getAsFloat(), sizeArray.get(2).getAsFloat());
            Point originPoint = new Vec(origin.get(0).getAsFloat(), origin.get(1).getAsFloat(), origin.get(2).getAsFloat());

            double maxSize = Math.max(Math.min(Math.min(sizePoint.x(), sizePoint.y()), sizePoint.z()), 0.5);
            while (maxSize > (16 / scale)) {
                maxSize /= 2;
            }

            var newPoint = originPoint
                    .add(sizePoint.x() / 2, 0, sizePoint.z() / 2)
                    .mul(-1, 1, 1);

            for (int x = 0; x < sizePoint.x() / maxSize; ++x) {
                for (int y = 0; y < sizePoint.y() / maxSize; ++y) {
                    for (int z = 0; z < sizePoint.z() / maxSize; ++z) {
                        var currentPos = new Vec(x * maxSize, y * maxSize, z * maxSize);

                        currentPos = currentPos.sub(sizePoint.x() / 2, 0, sizePoint.z() / 2);
                        currentPos = currentPos.add(maxSize / 2, 0, maxSize / 2);

                        if ((currentPos.x() + maxSize) > sizePoint.x()) {
                            currentPos = currentPos.withX(sizePoint.x() - maxSize);
                        }

                        if ((currentPos.z() + maxSize) > sizePoint.z())
                            currentPos = currentPos.withZ(sizePoint.z() - maxSize);

                        if ((currentPos.y() + maxSize) > sizePoint.y())
                            currentPos = currentPos.withY(sizePoint.y() - maxSize);

                        var created = new ModelBoneHitbox(pivotPos, name, boneRotation, genericModel, currentPos.add(newPoint), maxSize, maxSize, cubes, false, scale);
                        illegitimateChildren.add(created);
                    }
                }
            }
        }
    }

    @Override
    public void setParent(ModelBone parent) {
        super.setParent(parent);
        this.illegitimateChildren.forEach(modelBone -> modelBone.setParent(parent));
    }

    @Override
    public Point getPosition() {
        return stand.getPosition();
    }

    @Override
    public @NotNull Collection<ModelBone> getChildren() {
        if (this.illegitimateChildren == null) return List.of();
        return this.illegitimateChildren;
    }

    @Override
    public CompletableFuture<Void> spawn(Instance instance, Pos position) {
        this.illegitimateChildren.forEach(modelBone -> {
            modelBone.spawn(instance, modelBone.calculatePosition().add(model.getPosition()));
            MinecraftServer.getSchedulerManager().scheduleNextTick(modelBone::draw);
        });
        return super.spawn(instance, position);
    }

    @Override
    public void addAnimation(BoneAnimation animation) {
        super.addAnimation(animation);
        this.illegitimateChildren.forEach(modelBone -> modelBone.addAnimation(animation));
    }

    @Override
    public void setState(String state) {
    }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;
        Point p = this.offset;

        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        return Pos.fromPoint(p).div(4).mul(scale);
    }

    @Override
    public void destroy() {
        super.destroy();
        illegitimateChildren.forEach(ModelBone::destroy);
    }

    @Override
    public Point calculateRotation() {
        return Vec.ZERO;
    }

    @Override
    public Point calculateScale() {
        return Vec.ZERO;
    }

    @Override
    public void teleport(Point position) {
        draw();
    }

    public void draw() {
        if (!this.illegitimateChildren.isEmpty()) {
            this.children.forEach(ModelBone::draw);
            this.illegitimateChildren.forEach(ModelBone::draw);
        }

        if (this.offset == null || this.stand == null) return;

        var finalPosition = calculatePosition().add(model.getPosition());
        if (this.positionTask != null) this.positionTask.cancel();

        Pos currentPos = stand.getPosition();
        var diff = finalPosition.sub(currentPos).div(INTERPOLATE_TICKS);
        AtomicInteger ticks = new AtomicInteger(1);

        this.positionTask = MinecraftServer.getSchedulerManager().submitTask(() -> {
            var t = ticks.getAndIncrement();
            if (stand.isRemoved()) return TaskSchedule.stop();

            var newPos = currentPos.add(diff.mul(t));
            if (stand.getDistanceSquared(newPos) > 0.005) stand.teleport(newPos);

            if (t >= INTERPOLATE_TICKS) {
                this.positionTask = null;
                return TaskSchedule.stop();
            }

            return TaskSchedule.tick(1);
        });
    }
}
