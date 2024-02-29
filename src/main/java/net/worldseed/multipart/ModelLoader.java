package net.worldseed.multipart;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.worldseed.multipart.animations.FrameProvider;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModelLoader {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, JsonObject> loadedAnimations = new HashMap<>();
    private static final Map<String, JsonObject> loadedModels = new HashMap<>();

    // <Model> -> <Bone/Animation>
    private static final Map<String, Map<String, FrameProvider>> interpolationTranslateCache = new HashMap<>();
    private static final Map<String, Map<String, FrameProvider>> interpolationRotateCache = new HashMap<>();

    public static void clearCache() {
        interpolationTranslateCache.clear();
        interpolationRotateCache.clear();
        loadedAnimations.clear();
        loadedModels.clear();
    }

    public enum AnimationType {
        ROTATION, TRANSLATION
    }

    public static JsonObject loadAnimations(String toLoad) {
        if (loadedAnimations.containsKey(toLoad))
            return loadedAnimations.get(toLoad);

        JsonObject loadedAnimations1;

        try {
            loadedAnimations1 = GSON
                .fromJson(
                    new InputStreamReader(new FileInputStream(ModelEngine.getAnimationPath(toLoad))),
                    JsonObject.class
                );
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            loadedAnimations1 = null;
        }

        loadedAnimations.put(toLoad, loadedAnimations1);
        return loadedAnimations1;
    }

    public static JsonObject loadModel(String id) {
        if (loadedModels.containsKey(id))
            return loadedModels.get(id);

        JsonObject loadedModel1;
        try {
            loadedModel1 = GSON.fromJson(new InputStreamReader(new FileInputStream(ModelEngine.getGeoPath(id))), JsonObject.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            loadedModel1 = null;
        }

        loadedModels.put(id, loadedModel1);
        return loadedModel1;
    }

    public static void addToTranslationCache(String key, String model, FrameProvider val) {
        if (!interpolationTranslateCache.containsKey(model))
            interpolationTranslateCache.put(model, new HashMap<>());

        interpolationTranslateCache.get(model).put(key, val);
    }

    public static void addToRotationCache(String key, String model, FrameProvider val) {
        if (!interpolationRotateCache.containsKey(model))
            interpolationRotateCache.put(model, new HashMap<>());

        interpolationRotateCache.get(model).put(key, val);
    }

    public static FrameProvider getCacheRotation(String key, String model) {
        Map<String, FrameProvider> m = interpolationRotateCache.get(model);
        if (m == null) return null;
        return m.get(key);
    }

    public static FrameProvider getCacheTranslation(String key, String model) {
        Map<String, FrameProvider> m = interpolationTranslateCache.get(model);
        if (m == null) return null;
        return m.get(key);
    }

    public static Map<String, JsonObject> parseAnimations(String animationString) {
        Map<String, JsonObject> res = new LinkedHashMap<>();

        JsonObject animations = GSON.fromJson(new StringReader(animationString), JsonObject.class);
        for (Map.Entry<String, JsonElement> animation : animations.get("animations").getAsJsonObject().entrySet()) {
            res.put(animation.getKey(), animation.getValue().getAsJsonObject());
        }

        return res;
    }
}
