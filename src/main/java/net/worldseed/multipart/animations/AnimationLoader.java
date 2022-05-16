package net.worldseed.multipart.animations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Point;
import net.worldseed.multipart.ModelEngine;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class AnimationLoader {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, JsonObject> loadedAnimations = new HashMap<>();
    private static final Map<String, JsonObject> loadedModels = new HashMap<>();

    // <Model> -> <Bone/Animation>
    private static final Map<String, Map<String, Map<Short, Point>>> interpolationTranslateCache = new HashMap<>();
    private static final Map<String, Map<String, Map<Short, Point>>> interpolationRotateCache = new HashMap<>();

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

    public static void addToTranslationCache(String key, String model, Map<Short, Point> val) {
        if (!interpolationTranslateCache.containsKey(model))
            interpolationTranslateCache.put(model, new HashMap<>());

        interpolationTranslateCache.get(model).put(key, val);
    }

    public static void addToRotationCache(String key, String model, Map<Short, Point> val) {
        if (!interpolationRotateCache.containsKey(model))
            interpolationRotateCache.put(model, new HashMap<>());

        interpolationRotateCache.get(model).put(key, val);
    }

    public static Map<Short, Point> getCacheRotation(String key, String model) {
        Map<String, Map<Short, Point>> m = interpolationRotateCache.get(model);
        if (m == null) return null;
        return m.get(key);
    }

    public static Map<Short, Point> getCacheTranslation(String key, String model) {
        Map<String, Map<Short, Point>> m = interpolationTranslateCache.get(model);
        if (m == null) return null;
        return m.get(key);
    }
}
