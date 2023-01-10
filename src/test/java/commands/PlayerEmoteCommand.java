package commands;

import emotes.EmoteExample;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;

public class PlayerEmoteCommand extends Command {
    public PlayerEmoteCommand() {
        super("emote");

        setDefaultExecutor((sender, context) -> {
            final Player player = (Player) sender;
            PlayerSkin skin = PlayerSkin.fromUsername("Sg_Voltage");
            new EmoteExample(player.getInstance(), player.getPosition(), skin);
        });
    }
}