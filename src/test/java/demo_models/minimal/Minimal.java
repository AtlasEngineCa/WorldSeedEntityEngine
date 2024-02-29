package demo_models.minimal;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Minimal extends GenericModelImpl {
    @Override
    public String getId() {
        return "steve.bbmodel";
    }

    public void init(@Nullable Instance instance, @NotNull Pos position) {
        super.init(instance, position, 2.5f);
    }
}