package gem_golem;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModelImpl;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.ModelEngine;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GemGolemModel extends GenericModelImpl {
    @Override
    public String getId() {
        return "gem_golem.bbmodel";
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, BoneEntity nametag) {
        super.init(instance, position, new ModelConfig(ModelConfig.ModelType.ZOMBIE, ModelConfig.InterpolationType.Y_INTERPOLATION, ModelConfig.Size.NORMAL, ModelConfig.ItemSlot.HEAD));
        setNametagEntity(nametag);
    }
}