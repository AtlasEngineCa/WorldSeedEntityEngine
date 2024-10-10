package net.worldseed.multipart;

import com.google.gson.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket;
import net.worldseed.multipart.events.ModelControlEvent;
import net.worldseed.multipart.events.ModelDamageEvent;
import net.worldseed.multipart.events.ModelDismountEvent;
import net.worldseed.multipart.events.ModelInteractEvent;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.mql.MQLPoint;

import javax.json.JsonNumber;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public class ModelEngine {
    public final static HashMap<String, Point> offsetMappings = new HashMap<>();
    public final static HashMap<String, Point> diffMappings = new HashMap<>();
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final static HashMap<String, HashMap<String, ItemStack>> blockMappings = new HashMap<>();
    private static final EventListener<PlayerPacketEvent> playerListener = EventListener.of(PlayerPacketEvent.class, event -> {
        if (event.getPacket() instanceof ClientSteerVehiclePacket packet) {
            Entity ridingEntity = event.getPlayer().getVehicle();

            if (ridingEntity instanceof BoneEntity bone) {
                if (packet.flags() == 2) {
                    ModelDismountEvent entityRideEvent = new ModelDismountEvent(bone.getModel(), event.getPlayer());
                    EventDispatcher.call(entityRideEvent);
                }

                if (packet.flags() == 1) {
                    EventDispatcher.call(new ModelControlEvent(bone.getModel(), packet.forward(), packet.sideways(), true));
                }

                if (packet.flags() == 0) {
                    EventDispatcher.call(new ModelControlEvent(bone.getModel(), packet.forward(), packet.sideways(), false));
                }
            }
        }
    });
    private static final EventListener<PlayerEntityInteractEvent> playerInteractListener = EventListener.of(PlayerEntityInteractEvent.class, event -> {
        if (event.getTarget() instanceof BoneEntity bone) {
            ModelInteractEvent modelInteractEvent = new ModelInteractEvent(bone.getModel(), event, bone);
            EventDispatcher.call(modelInteractEvent);
        }
    });
    private static final EventListener<EntityDamageEvent> entityDamageListener = EventListener.of(EntityDamageEvent.class, event -> {
        if (event.getEntity() instanceof BoneEntity bone) {
            event.setCancelled(true);
            ModelDamageEvent modelDamageEvent = new ModelDamageEvent(bone.getModel(), event, bone);
            MinecraftServer.getGlobalEventHandler().call(modelDamageEvent);
        }
    });
    private static Path modelPath;
    private static Material modelMaterial = Material.MAGMA_CREAM;

    static {
        MinecraftServer.getGlobalEventHandler()
                .addListener(playerListener)
                .addListener(playerInteractListener)
                .addListener(entityDamageListener);
    }

    /**
     * Loads the model from the given path
     *
     * @param mappingsData mappings file created by model parser
     * @param modelPath    path of the models
     */
    public static void loadMappings(Reader mappingsData, Path modelPath) {
        JsonObject map = GSON.fromJson(mappingsData, JsonObject.class);
        ModelEngine.modelPath = modelPath;

        blockMappings.clear();
        offsetMappings.clear();
        diffMappings.clear();
        ModelLoader.clearCache();

        map.entrySet().forEach(entry -> {
            HashMap<String, ItemStack> keys = new HashMap<>();

            entry.getValue().getAsJsonObject()
                    .get("id")
                    .getAsJsonObject()
                    .entrySet()
                    .forEach(id -> keys.put(id.getKey(), generateBoneItem(id.getValue().getAsInt())));

            blockMappings.put(entry.getKey(), keys);
            offsetMappings.put(entry.getKey(), getPos(entry.getValue().getAsJsonObject().get("offset").getAsJsonArray()).orElse(Pos.ZERO));
            diffMappings.put(entry.getKey(), getPos(entry.getValue().getAsJsonObject().get("diff").getAsJsonArray()).orElse(Pos.ZERO));
        });
    }

    private static ItemStack generateBoneItem(int model_id) {
        return ItemStack.builder(modelMaterial).set(ItemComponent.CUSTOM_MODEL_DATA, model_id).build();
    }

    public static HashMap<String, ItemStack> getItems(String model, String name) {
        return blockMappings.get(model + "/" + name);
    }

    public static Path getGeoPath(String id) {
        return modelPath.resolve(id).resolve("model.geo.json");
    }

    public static Path getAnimationPath(String id) {
        return modelPath.resolve(id).resolve("model.animation.json");
    }

    public static Optional<Point> getPos(JsonElement pivot) {
        if (pivot == null) return Optional.empty();
        else {
            JsonArray arr = pivot.getAsJsonArray();
            return Optional.of(new Vec(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble()));
        }
    }

    public static Optional<MQLPoint> getMQLPos(JsonElement pivot) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (pivot == null) return Optional.empty();
        else if (pivot instanceof JsonObject obj) {
            return Optional.of(new MQLPoint(obj));
        } else if (pivot instanceof JsonNumber num) {
            return Optional.of(new MQLPoint(num.doubleValue(), num.doubleValue(), num.doubleValue()));
        } else {
            return Optional.empty();
        }
    }

    public static Material getModelMaterial() {
        return modelMaterial;
    }

    public static void setModelMaterial(Material modelMaterial) {
        ModelEngine.modelMaterial = modelMaterial;
    }

    public static Optional<MQLPoint> getMQLPos(JsonArray arr) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (arr == null) return Optional.empty();
        else {
            return Optional.of(new MQLPoint(arr));
        }
    }
}
