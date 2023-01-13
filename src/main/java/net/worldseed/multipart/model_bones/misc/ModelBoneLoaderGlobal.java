package net.worldseed.multipart.model_bones.misc;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.SlimeMeta;
import net.minestom.server.network.packet.server.play.AttachEntityPacket;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ModelBoneLoaderGlobal extends ModelBoneImpl {
    private final List<Entity> holder = new ArrayList<>();

    public ModelBoneLoaderGlobal(Point pivot, String name, Point rotation, GenericModel model, LivingEntity forwardTo) {
        super(pivot, name, rotation, model);

        String[] spl = name.split("_");
        int size = spl.length > 1 ? Integer.parseInt(spl[1]) : 1;

        if (this.offset != null) {
            this.stand = new LivingEntity(EntityType.SLIME) {
                @Override
                public void tick(long time) {}

                @Override
                public void updateNewViewer(@NotNull Player player) {
                    super.updateNewViewer(player);

                    for (Entity e : holder) {
                        e.addViewer(player);
                        e.updateNewViewer(player);
                        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> player.sendPacket(new AttachEntityPacket(e, this)));
                    }
                }

                @Override
                public void updateOldViewer(@NotNull Player player) {
                    super.updateOldViewer(player);

                    for (Entity e : holder) {
                        e.removeViewer(player);
                        e.updateOldViewer(player);
                    }
                }
            };

            SlimeMeta meta = (SlimeMeta) this.stand.getEntityMeta();
            meta.setSize(size);

            ModelBoneImpl.hookPart(this, forwardTo);
            this.stand.setTag(Tag.String("WSEE"), "loader");
        }
    }

    @Override
    public void setState(String state) {}

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;

        Point p = this.offset;
        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        Pos endPos = Pos.fromPoint(p);

        double divisor;
        if (model.config().itemSlot() == ModelConfig.ItemSlot.HEAD) {
            divisor = model.config().size() == ModelConfig.Size.SMALL ? 1.426 : 1;
        } else {
            divisor = model.config().size() == ModelConfig.Size.SMALL ? 1.25 : 0.624;
        }

        return endPos
                .div(6.4, 6.4, 6.4)
                .div(divisor)
                .add(model.getPosition())
                .add(model.getGlobalOffset());
    }

    public void addLoading(Entity entity) {
        entity.setAutoViewable(false);
        this.holder.add(entity);
        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> entity.sendPacketToViewers(new AttachEntityPacket(entity, stand)));
    }

    @Override
    public Point calculateRotation() {
        return Vec.ZERO;
    }

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;
        stand.teleport(calculatePosition());
    }
}
