package events;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.network.packet.server.common.ResourcePackPushPacket;
import net.minestom.server.resourcepack.ResourcePack;
import net.minestom.server.timer.TaskSchedule;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PackEvent {
    static class PackHandler implements HttpHandler {
        private final File zipFile;

        public PackHandler(File zipFile) {
            this.zipFile = zipFile;
        }

        public void handle(HttpExchange t) throws IOException {
            t.sendResponseHeaders(200, zipFile.getTotalSpace());

            OutputStream os = t.getResponseBody();
            Files.copy(zipFile.toPath(), os);
            os.close();
        }
    }

    private static void startHttpServer(File zipFile) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080),0);
        server.createContext("/pack", new PackHandler(zipFile));
        server.start();
    }

    private static String calculateMD5(File file) throws NoSuchAlgorithmException, FileNotFoundException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int read = 0;
        try {
            while( (read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);

            return output;
        }
        catch(IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        }
        finally {
            try { is.close(); }
            catch(IOException e) {
                throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
            }
        }
    }

    public static void hook(GlobalEventHandler handler, File zipFile) throws IOException, NoSuchAlgorithmException {
        startHttpServer(zipFile);
        String hash = calculateMD5(zipFile);

        handler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                ResourcePack pack = ResourcePack.optional(UUID.randomUUID(), "http://127.0.0.1:8080/pack?hash=" + hash, hash, Component.text("WSEE Resource Pack"));
                ResourcePackPushPacket packSendPacket = new ResourcePackPushPacket(pack);
                event.getPlayer().sendPacket(packSendPacket);
            }, TaskSchedule.tick(20), TaskSchedule.stop());
        });
    }
}
