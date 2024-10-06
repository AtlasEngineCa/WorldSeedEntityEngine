package net.worldseed.multipart.model_bones.display_entity;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RootBoneEntity extends BoneEntity {
    public RootBoneEntity(GenericModel model) {
        super(EntityType.ARMOR_STAND, model, "root");

        ArmorStandMeta meta = (ArmorStandMeta) this.getEntityMeta();
        meta.setMarker(true);

        this.setInvisible(true);
        this.setNoGravity(true);
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);

        List<Integer> parts = this.getModel().getParts().stream()
                .map(ModelBone::getEntity)
                .filter(e -> e != null && e.getEntityType() == EntityType.ITEM_DISPLAY)
                .map(Entity::getEntityId)
                .toList();
        SetPassengersPacket packet = new SetPassengersPacket(this.getEntityId(), parts);
        player.sendPacket(packet);
    }
}
