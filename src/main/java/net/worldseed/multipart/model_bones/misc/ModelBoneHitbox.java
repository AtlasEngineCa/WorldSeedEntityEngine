package net.worldseed.multipart.model_bones.misc;

import com.google.gson.JsonArray;
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
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.animations.ModelAnimation;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ModelBoneHitbox extends ModelBoneImpl {
    private final JsonArray cubes;

    Pos actualPosition = null;
    Collection<ModelBoneHitbox> illegitimateChildren = new ConcurrentLinkedDeque<>();

    public void addViewer(Player player) {
        if (this.stand != null) this.stand.addViewer(player);
        illegitimateChildren.forEach(modelBone -> modelBone.addViewer(player));
    }

    public void removeViewer(Player player) {
        if (this.stand != null) this.stand.removeViewer(player);
        illegitimateChildren.forEach(modelBone -> modelBone.removeViewer(player));
    }

    @Override
    public void setScale(float scale) {
        super.setScale(scale);

        this.destroy();
        this.illegitimateChildren.clear();
        generateStands(this.cubes, this.pivot, this.name, this.rotation, this.model);
        this.illegitimateChildren.forEach(modelBone -> modelBone.spawn(model.getInstance(), model.getPosition()));
    }

    private static final Tag<String> WSEE = Tag.String("WSEE");

    public ModelBoneHitbox(Point pivot, String name, Point rotation, GenericModel model, Point newOffset, double sizeX, double sizeY, JsonArray cubes, boolean parent, float scale) {
        super(pivot, name, rotation, model, scale);

        this.cubes = cubes;

        if (parent) {
            generateStands(cubes, pivot, name, rotation, model);
            this.offset = null;
        } else {
            if (this.offset != null) {
                this.stand = new BoneEntity(EntityType.INTERACTION, model) {
                    @Override
                    public void updateNewViewer(@NotNull Player player) {
                        super.updateNewViewer(player);

                        EntityTeleportPacket packet = new EntityTeleportPacket(this.getEntityId(), this.position, true);
                        player.getPlayerConnection().sendPacket(packet);

                        illegitimateChildren.forEach(modelBone -> modelBone.addViewer(player));
                    }

                    @Override
                    public void updateOldViewer(@NotNull Player player) {
                        super.updateOldViewer(player);
                        illegitimateChildren.forEach(modelBone -> modelBone.removeViewer(player));
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

    public void generateStands(JsonArray cubes, Point pivotPos, String name, Point boneRotation, GenericModel genericModel) {
        for (var cube : cubes) {
            JsonArray sizeArray = cube.getAsJsonObject().get("size").getAsJsonArray();
            JsonArray origin = cube.getAsJsonObject().get("origin").getAsJsonArray();

            Point sizePoint = new Vec(sizeArray.get(0).getAsFloat(), sizeArray.get(1).getAsFloat(), sizeArray.get(2).getAsFloat());
            Point originPoint = new Vec(origin.get(0).getAsFloat(), origin.get(1).getAsFloat(), origin.get(2).getAsFloat());

            double maxSize = Math.max(Math.min(Math.min(sizePoint.x(), sizePoint.y()), sizePoint.z()), 0.5);
            while (maxSize > (32 / scale)) {
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
                        currentPos = currentPos.add(newPoint);

                        if ((currentPos.x() + maxSize) > sizePoint.x()) {
                            currentPos = currentPos.withX(sizePoint.x() - maxSize);
                        }

                        if ((currentPos.z() + maxSize) > sizePoint.z())
                            currentPos = currentPos.withZ(sizePoint.z() - maxSize);

                        if ((currentPos.y() + maxSize) > sizePoint.y())
                            currentPos = currentPos.withY(sizePoint.y() - maxSize);

                        var created = new ModelBoneHitbox(pivotPos, name, boneRotation, genericModel, currentPos, maxSize, maxSize, cubes, false, scale);
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

    public Collection<ModelBoneHitbox> getParts() {
        if (this.illegitimateChildren == null) return List.of();
        return this.illegitimateChildren;
    }

    @Override
    public CompletableFuture<Void> spawn(Instance instance, Point position) {
        this.illegitimateChildren.forEach(modelBone -> {
            modelBone.spawn(instance, modelBone.calculatePosition().add(model.getPosition()));
            MinecraftServer.getSchedulerManager().scheduleNextTick(modelBone::draw);
        });
        return super.spawn(instance, position);
    }

    @Override
    public void addAnimation(ModelAnimation animation) {
        super.addAnimation(animation);
        this.illegitimateChildren.forEach(modelBone -> modelBone.addAnimation(animation));
    }

    @Override
    public void setState(String state) { }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;
        Point p = this.offset;

        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        var lp = actualPosition;

        if (actualPosition == null) {
            actualPosition = Pos.fromPoint(p).div(4).mul(scale);
            return actualPosition;
        }

        var newPoint = Pos.fromPoint(p).div(4).mul(scale);
        actualPosition = Pos.fromPoint(lp.asVec().lerp(Vec.fromPoint(newPoint), 0.25));

        return lp;
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

    public void draw() {
        if (this.illegitimateChildren.size() > 0) {
            this.children.forEach(ModelBone::draw);
            this.illegitimateChildren.forEach(ModelBone::draw);
        }

        if (this.offset == null) return;

        try {
            stand.teleport(calculatePosition().add(model.getPosition()));
        } catch (Exception ignored) { }
    }
}
