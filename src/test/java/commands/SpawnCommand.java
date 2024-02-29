package commands;

import demo_models.bulbasaur.BulbasaurMob;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class SpawnCommand extends Command {
    public SpawnCommand() {
        super("spawn");

        setDefaultExecutor((sender, context) -> {
            final Player player = (Player) sender;
            try {
                new BulbasaurMob(player.getInstance(), player.getPosition());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}