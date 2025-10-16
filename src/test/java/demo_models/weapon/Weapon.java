package demo_models.weapon;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Weapon extends GenericModelImpl {
    @Override
    public String getId() {
        return "weapon.bbmodel";
    }

    public void init(@Nullable Instance instance, @NotNull Pos position) {
        super.init(instance, position, 4.5f);
    }
}