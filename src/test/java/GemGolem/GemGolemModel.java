package GemGolem;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
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

    private final Pos globalOffset = new Pos(0, -1.4, 0);
    public Pos getGlobalOffset() {
        return globalOffset;
    }

    private static final String id = "gem_golem";

    @Override
    public String getId() {
        return id;
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, Entity masterEntity) {
        super.init(instance, position, ModelEngine.RenderType.ZOMBIE, masterEntity);
    }
}
