import Commands.SpawnCommand;
import Events.CombatEvent;
import Events.PackEvent;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.extras.lan.OpenToLAN;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.monitoring.TickMonitor;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.world.DimensionType;
import net.worldseed.multipart.ModelEngine;
import net.worldseed.multipart.parser.ModelParser;
import org.apache.commons.io.FileUtils;

import javax.naming.SizeLimitExceededException;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    private static final String BASE_PATH = "src/test/resources/";
    private static final String ZIP_PATH = BASE_PATH + "resourcepack.zip";
    private static final String MODEL_PATH = BASE_PATH + "models/";

    public static void main(String[] args) throws IOException, SizeLimitExceededException, NoSuchAlgorithmException {
        MinecraftServer minecraftServer = MinecraftServer.init();

        try {
            FileUtils.deleteDirectory(new File(BASE_PATH + "resourcepack"));
        } catch (IllegalArgumentException ignored) { }

        FileUtils.copyDirectory(new File(BASE_PATH + "resourcepack_template"), new File(BASE_PATH + "resourcepack"));
        ModelParser.parse(BASE_PATH + "resourcepack/assets/wsee/", MODEL_PATH, BASE_PATH);
        ModelEngine.loadMappings(new File(BASE_PATH + "model_mappings.json"), MODEL_PATH);

        PackZip.ZipResourcePack(new File(BASE_PATH + "resourcepack"), ZIP_PATH);
        File zipFile = new File(ZIP_PATH);

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer lobby = instanceManager.createInstanceContainer(DimensionType.OVERWORLD);
        lobby.enableAutoChunkLoad(true);
        lobby.setGenerator(unit -> unit.modifier().fillHeight(0, 1, Block.STONE));
        lobby.setTimeRate(0);
        instanceManager.registerInstance(lobby);

        // Commands
        {
            CommandManager manager = MinecraftServer.getCommandManager();
            manager.setUnknownCommandCallback((sender, c) -> sender.sendMessage("Command not found."));
            manager.register(new SpawnCommand());
        }

        // Events
        {
            GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler();

            // Group events
            CombatEvent.hook(handler);
            PackEvent.hook(handler, zipFile);

            // Login
            handler.addListener(PlayerLoginEvent.class, event -> {
                final Player player = event.getPlayer();
                player.setRespawnPoint(new Pos(0.5, 16, 0.5));
                event.setSpawningInstance(lobby);

                Audiences.all().sendMessage(Component.text(
                        player.getUsername() + " has joined",
                        NamedTextColor.GREEN
                ));
            });

            handler.addListener(PlayerSpawnEvent.class, event -> {
                if (!event.isFirstSpawn()) return;
                final Player player = event.getPlayer();
                player.setGameMode(GameMode.SURVIVAL);
                player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1f));
                player.setEnableRespawnScreen(false);
            });

            // Logout
            handler.addListener(PlayerDisconnectEvent.class, event -> Audiences.all().sendMessage(Component.text(
                    event.getPlayer().getUsername() + " has left",
                    NamedTextColor.RED
            )));

            // Chat
            handler.addListener(PlayerChatEvent.class, chatEvent -> {
                chatEvent.setChatFormat((event) -> Component.text(event.getEntity().getUsername())
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY)
                                .append(Component.text(event.getMessage(), NamedTextColor.WHITE))));
            });

            // Monitoring
            AtomicReference<TickMonitor> lastTick = new AtomicReference<>();
            handler.addListener(ServerTickMonitorEvent.class, event -> lastTick.set(event.getTickMonitor()));

            // Header/footer
            MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                Collection<Player> players = MinecraftServer.getConnectionManager().getOnlinePlayers();
                if (players.isEmpty()) return;

                final Runtime runtime = Runtime.getRuntime();
                final TickMonitor tickMonitor = lastTick.get();
                final long ramUsage = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;

                final Component header = Component.newline()
                        .append(Component.newline()).append(Component.text("Players: " + players.size()))
                        .append(Component.newline()).append(Component.newline())
                        .append(Component.text("RAM USAGE: " + ramUsage + " MB", NamedTextColor.GRAY).append(Component.newline())
                                .append(Component.text("TICK TIME: " + MathUtils.round(tickMonitor.getTickTime(), 2) + "ms", NamedTextColor.GRAY))).append(Component.newline());

                final Component footer = Component.newline().append(Component.text("Project: minestom.net").append(Component.newline())
                                .append(Component.text("    Source: github.com/WorldSeedMMO/WorldSeedEntityEngine", TextColor.color(31, 142, 91))).append(Component.newline()))
                        .append(Component.newline());

                Audiences.players().sendPlayerListHeaderAndFooter(header, footer);
            }, TaskSchedule.tick(10), TaskSchedule.tick(10));
        }

        OpenToLAN.open();

        minecraftServer.start("0.0.0.0", 25565);
        System.out.println("Server startup done!");
    }
}
