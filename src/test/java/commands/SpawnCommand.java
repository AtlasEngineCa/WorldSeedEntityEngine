package commands;

import minimal.MinimalMob;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class SpawnCommand extends Command {
    public SpawnCommand() {
        super("spawn");

        setDefaultExecutor((sender, context) -> {
            final Player player = (Player) sender;
            new MinimalMob(player.getInstance(), player.getPosition());
        });
    }
}