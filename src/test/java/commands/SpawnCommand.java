package commands;

import demo_models.bulbasaur.BulbasaurMob;
import demo_models.gem_golem.GemGolemMob;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.entity.Player;

public class SpawnCommand extends Command {
    public SpawnCommand() {
        super("spawn");

        ArgumentInteger entityArg = new ArgumentInteger("entity");

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            int entity = context.get(entityArg);

            try {
                switch (entity) {
                    case 1 -> new BulbasaurMob(player.getInstance(), player.getPosition());
                    case 2 -> new GemGolemMob(player.getInstance(), player.getPosition());
                    default -> sender.sendMessage("Неизвестный моб: " + entity);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, entityArg);
    }
}
