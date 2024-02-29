package demo_models.bulbasaur;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BulbasaurModel extends GenericModelImpl {
    @Override
    public String getId() {
        return "bulbasaur/bulbasaur.bbmodel";
    }

    public void init(@Nullable Instance instance, @NotNull Pos position) {
        super.init(instance, position, 1.0f);
    }
}