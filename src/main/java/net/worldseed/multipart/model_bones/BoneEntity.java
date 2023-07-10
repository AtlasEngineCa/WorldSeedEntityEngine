package net.worldseed.multipart.model_bones;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.entity.metadata.ObjectDataProvider;
import net.minestom.server.network.packet.server.LazyPacket;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.GenericModel;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class BoneEntity extends LivingEntity {
    private final GenericModel model;

    public BoneEntity(@NotNull EntityType entityType, GenericModel model) {
        super(entityType);
        this.setAutoViewable(false);
        setTag(Tag.String("WSEE"), "part");
        this.model = model;
    }

    @Override
    public @NotNull Set<Player> getViewers() {
        return model.getViewers();
    }

    public GenericModel getModel() {
        return model;
    }

    @Override
    public void tick(long time) { }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        Pos position = this.getPosition();
        var spawnPacket = new SpawnEntityPacket(this.getEntityId(), this.getUuid(), this.getEntityType().id(), model.getPosition(), position.yaw(), 0, (short) 0, (short) 0, (short) 0);

        player.sendPacket(spawnPacket);
        player.sendPacket(new LazyPacket(this::getMetadataPacket));
    }
}
