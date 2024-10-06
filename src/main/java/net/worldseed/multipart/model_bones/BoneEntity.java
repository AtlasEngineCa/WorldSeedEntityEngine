package net.worldseed.multipart.model_bones;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.LazyPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.tag.Tag;
import net.worldseed.multipart.GenericModel;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class BoneEntity extends LivingEntity {
    private final GenericModel model;
    private final String name;

    public BoneEntity(@NotNull EntityType entityType, GenericModel model, String name) {
        super(entityType);
        this.setAutoViewable(false);
        setTag(Tag.String("WSEE"), "part");
        this.model = model;
        this.name = name;

        this.setNoGravity(true);
        this.setSynchronizationTicks(Integer.MAX_VALUE);
    }

    @Override
    public @NotNull Set<Player> getViewers() {
        return model.getViewers();
    }

    public GenericModel getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    @Override
    public void tick(long time) {
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        Pos position = this.getPosition();
        var spawnPacket = new SpawnEntityPacket(this.getEntityId(), this.getUuid(), this.getEntityType().id(), model.getPosition().withView(position.yaw(), 0), position.yaw(), 0, (short) 0, (short) 0, (short) 0);

        player.sendPacket(spawnPacket);
        player.sendPacket(new LazyPacket(this::getMetadataPacket));

        if (this.getEntityType() == EntityType.ZOMBIE || this.getEntityType() == EntityType.ARMOR_STAND)
            player.sendPacket(getEquipmentsPacket());
    }
}
