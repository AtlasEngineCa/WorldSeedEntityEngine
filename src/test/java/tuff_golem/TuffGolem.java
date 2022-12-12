package tuff_golem;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModelImpl;
import net.worldseed.multipart.ModelConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TuffGolem extends GenericModelImpl {
    private final Pos pivot = new Pos(0, 0, 0);
    private final Pos globalOffset = new Pos(0, 0, 0);
    private static final String id = "tuff_golem.bbmodel";

    public Pos getPivot() {
        return pivot;
    }
    public Pos getGlobalOffset() {
        return globalOffset;
    }

    @Override
    public String getId() {
        return id;
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, LivingEntity masterEntity) {
        super.init(instance, position, ModelConfig.defaultConfig, masterEntity);
    }
}
