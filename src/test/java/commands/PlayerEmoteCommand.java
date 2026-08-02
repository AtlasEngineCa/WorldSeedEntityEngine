package commands;

import emotes.EmoteExample;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;

public class PlayerEmoteCommand extends Command {
    public PlayerEmoteCommand() {
        super("emote");

        setDefaultExecutor((sender, context) -> {
            final Player player = (Player) sender;
            PlayerSkin skin = player.getSkin();
            if (skin == null) skin = PlayerSkin.fromUsername("Notch");
            if (skin == null) throw new IllegalStateException("Could not resolve a player skin for the emote demo");

            Pos playerPos = player.getPosition();
            Vec forward = playerPos.direction().withY(0).normalize().mul(4);
            Pos emotePos = new Pos(playerPos.add(forward), playerPos.yaw() + 180, 0);
            new EmoteExample(player.getInstance(), emotePos, skin);
        });
    }
}
