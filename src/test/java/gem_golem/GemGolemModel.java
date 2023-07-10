package gem_golem;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModelImpl;
import net.worldseed.multipart.model_bones.BoneEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GemGolemModel extends GenericModelImpl {
    @Override
    public String getId() {
        return "gem_golem.bbmodel";
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, BoneEntity nametag) {
        super.init(instance, position);
        setNametagEntity(nametag);
    }
}