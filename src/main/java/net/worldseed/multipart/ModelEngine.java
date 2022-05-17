package net.worldseed.multipart;

import com.google.gson.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ModelEngine {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final static HashMap<String, HashMap<String, ItemStack>> blockMappings = new HashMap<>();
    final static HashMap<String, Pos> offsetMappings = new HashMap<>();
    final static HashMap<String, Pos> diffMappings = new HashMap<>();
    private static String modelPath;

    public enum RenderType {
        ZOMBIE,
        ARMOUR_STAND
    }

    public static void loadMappings() throws FileNotFoundException {
        Map<String, String> env = System.getenv();
        String path =  env.containsKey("MINECRAFT_ENV") ? "/var/files/Textures/generated/model_mappings.json" : "../Files/Textures/generated/model_mappings.json";
        modelPath =  env.containsKey("MINECRAFT_ENV") ? "/var/files/Models" : "../Files/Models";

        JsonObject map = GSON.fromJson(new InputStreamReader(new FileInputStream(path)), JsonObject.class);

        blockMappings.clear();
        offsetMappings.clear();
        diffMappings.clear();

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

    public static Optional<Pos> getPos(JsonElement pivot) {
        if (pivot == null) return Optional.empty();
        else {
            JsonArray arr = pivot.getAsJsonArray();
            return Optional.of(new Pos(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble()));
        }
    }

    static {
        try {
            loadMappings();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
