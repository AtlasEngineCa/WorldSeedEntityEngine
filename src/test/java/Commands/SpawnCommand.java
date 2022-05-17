package Commands;

import GemGolem.GemGolemMob;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class SpawnCommand extends Command {
    public SpawnCommand() {
        super("spawn");

        setDefaultExecutor((sender, context) -> {
            final Player player = (Player) sender;
            GemGolemMob golem = new GemGolemMob(player.getInstance(), player.getPosition());
        });
    }
}
