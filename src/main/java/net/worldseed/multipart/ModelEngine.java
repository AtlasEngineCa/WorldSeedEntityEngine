package net.worldseed.multipart;

import com.google.gson.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.worldseed.multipart.animations.AnimationLoader;

import java.io.Reader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public class ModelEngine {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final static HashMap<String, HashMap<String, ItemStack>> blockMappings = new HashMap<>();
    final static HashMap<String, Point> offsetMappings = new HashMap<>();
    final static HashMap<String, Point> diffMappings = new HashMap<>();
    private static Path modelPath;

    /**
     * Entity Type to use for rendering the mode. If you don't know what this is, use ARMOR_STAND
     */
    public enum RenderType {
        ZOMBIE,
        SMALL_ZOMBIE,
        ARMOUR_STAND,
        SMALL_ARMOUR_STAND
    }

    /**
     * Loads the model from the given path
     * @param mappingsData mappings file created by model parser
     * @param modelPath path of the models
     */
    public static void loadMappings(Reader mappingsData, Path modelPath) {
        JsonObject map = GSON.fromJson(mappingsData, JsonObject.class);
        ModelEngine.modelPath = modelPath;

        blockMappings.clear();
        offsetMappings.clear();
        diffMappings.clear();
        AnimationLoader.clearCache();

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
        Material material = Material.LEATHER_HORSE_ARMOR;

        return ItemStack.builder(material).meta(itemMetaBuilder -> {
            itemMetaBuilder
                    .displayName(Component.empty())
                    .unbreakable(true)
                    .hideFlag(127);

            itemMetaBuilder.customModelData(model_id);
        }).build();
    }

    static HashMap<String, ItemStack> getItems(String model, String name) {
        return blockMappings.get(model + "/" + name);
    }

    public static String getGeoPath(String id) {
        return modelPath + "/" + id + "/model.geo.json";
    }
    public static String getAnimationPath(String id) {
        return modelPath + "/" + id + "/model.animation.json";
    }

    public static Optional<Point> getPos(JsonElement pivot) {
        if (pivot == null) return Optional.empty();
        else {
            JsonArray arr = pivot.getAsJsonArray();
            return Optional.of(new Vec(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble()));
        }
    }
}
