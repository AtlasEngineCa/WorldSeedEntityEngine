package commands;

import demo_models.weapon.WeaponMob;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class SpawnCommand extends Command {
    public SpawnCommand() {
        super("spawn");

        setDefaultExecutor((sender, _) -> {
            final Player player = (Player) sender;
            try {
                new WeaponMob(player);
//                MinecraftServer.getSchedulerManager().buildTask(() -> {
//                            System.out.println("123");
//                            entity.
//                        })
//                        .repeat(TaskSchedule.tick(1))
//                        .schedule();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}