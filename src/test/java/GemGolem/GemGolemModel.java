package GemGolem;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModelImpl;
import net.worldseed.multipart.ModelEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GemGolemModel extends GenericModelImpl {
    private final Pos pivot = new Pos(0, 0, 0);
    public Pos getPivot() {
        return pivot;
    }

    private final Pos globalOffset = new Pos(0, 0, 0);
    public Pos getGlobalOffset() {
        return globalOffset;
    }

    private static final String id = "gem_golem.bbmodel";

    @Override
    public String getId() {
        return id;
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, LivingEntity masterEntity, LivingEntity nametag) {
        super.init(instance, position, ModelEngine.RenderType.ZOMBIE, masterEntity);
        setNametagEntity(nametag);
    }
}
