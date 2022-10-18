package Commands;

import TuffGolem.TuffGolemMob;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class SpawnCommand extends Command {
    public SpawnCommand() {
        super("spawn");

        setDefaultExecutor((sender, context) -> {
            final Player player = (Player) sender;
            new TuffGolemMob(player.getInstance(), player.getPosition(), player);
        });
    }
}