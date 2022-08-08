package net.worldseed.multipart.parser.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.worldseed.multipart.parser.ModelParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoGenerator {
    private static List<JsonObject> parseRecursive(JsonObject obj, Map<String, JsonObject> cubeMap, String parent) {
        List<JsonObject> res = new ArrayList<>();

        String name = obj.get("name").getAsString();
        JsonArray pivot_ = obj.get("origin").getAsJsonArray();
        JsonArray pivot = new JsonArray();
        pivot.add(-pivot_.get(0).getAsDouble());
        pivot.add(pivot_.get(1).getAsDouble());
        pivot.add(pivot_.get(2).getAsDouble());

        JsonArray cubes = new JsonArray();

        JsonArray rotation = new JsonArray();
        if (!obj.has("rotation")) {
            rotation = new JsonArray();
            rotation.add(0);
            rotation.add(0);
            rotation.add(0);
        } else {
            JsonArray rotation_ = obj.get("rotation").getAsJsonArray();
            rotation.add(-rotation_.get(0).getAsDouble());
            rotation.add(-rotation_.get(1).getAsDouble());
            rotation.add(rotation_.get(2).getAsDouble());
        }

        for (JsonElement child : obj.get("children").getAsJsonArray()) {
            if (child.isJsonObject()) {
                res.addAll(parseRecursive(child.getAsJsonObject(), cubeMap, name));
            } else {
                JsonObject cube = cubeMap.get(child.getAsString());
                cubes.add(cube);
            }
        }

        JsonObject thisEl = new JsonObject();
        thisEl.addProperty("name", name);
        thisEl.add("pivot", pivot);
        thisEl.add("rotation", rotation);
        thisEl.add("cubes", cubes);

        if (parent != null) {
            thisEl.addProperty("parent", parent);
        }

        res.add(thisEl);

        return res;
    }

    public static JsonArray generate(JsonArray elements, JsonArray outliner) {
        Map<String, JsonObject> blocks = new HashMap<>();

        for (var element : elements) {
            JsonObject el = element.getAsJsonObject();

            double inflate = 0;
            if (el.has("inflate")) {
                inflate = el.get("inflate").getAsDouble();
            }

            JsonArray origin_ = el.get("origin").getAsJsonArray();
            JsonArray origin = new JsonArray();
            origin.add(-Math.round(origin_.get(0).getAsDouble() * 1000) / 1000.0);
            origin.add(Math.round(origin_.get(1).getAsDouble() * 1000) / 1000.0);
            origin.add(Math.round(origin_.get(2).getAsDouble() * 1000) / 1000.0);

            JsonArray from__ = ModelParser.applyInflate(el.get("from").getAsJsonArray(), -inflate);
            JsonArray to_ = ModelParser.applyInflate(el.get("to").getAsJsonArray(), inflate);

            JsonArray to = new JsonArray();
            to.add(Math.round(to_.get(0).getAsDouble() * 1000) / 1000.0);
            to.add(Math.round(to_.get(1).getAsDouble() * 1000) / 1000.0);
            to.add(Math.round(to_.get(2).getAsDouble() * 1000) / 1000.0);

            JsonArray from_ = new JsonArray();
            from_.add(Math.round(from__.get(0).getAsDouble() * 1000) / 1000.0);
            from_.add(Math.round(from__.get(1).getAsDouble() * 1000) / 1000.0);
            from_.add(Math.round(from__.get(2).getAsDouble() * 1000) / 1000.0);

            JsonArray size = buildSize(from_, to);

            JsonArray from = new JsonArray();
            from.add(-(from_.get(0).getAsDouble() + size.get(0).getAsDouble()));
            from.add(from_.get(1).getAsDouble());
            from.add(from_.get(2).getAsDouble());

            JsonArray rotation;
            if (!el.has("rotation")) {
                rotation = new JsonArray();
                rotation.add(0);
                rotation.add(0);
                rotation.add(0);
            } else {
                JsonArray rotation_ = el.get("rotation").getAsJsonArray();
                rotation = new JsonArray();
                rotation.add(-Math.round(rotation_.get(0).getAsDouble() * 10) / 10.0);
                rotation.add(-Math.round(rotation_.get(1).getAsDouble() * 10) / 10.0);
                rotation.add(Math.round(rotation_.get(2).getAsDouble() * 10) / 10.0);
            }

            JsonObject faces = parseFaces(el.get("faces").getAsJsonObject());
            JsonObject built = new JsonObject();
            built.add("origin", from);
            built.add("size", size);
            built.add("pivot", origin);
            built.add("rotation", rotation);
            built.add("uv", faces);

            blocks.put(el.get("uuid").getAsString(), built);
        }

        List<JsonObject> bonesList = new ArrayList<>();
        for (var outline : outliner) {
            JsonObject el = outline.getAsJsonObject();
            bonesList.addAll(parseRecursive(el, blocks, null));
        }

        JsonArray bones = new JsonArray();
        for (var bone : bonesList) bones.add(bone);

        return bones;
    }

    private static JsonObject parseFaces(JsonObject obj) {
        JsonObject res = new JsonObject();

        for (var entry : obj.entrySet()) {
            String face = entry.getKey();
            JsonElement uv = entry.getValue().getAsJsonObject().get("uv");

            JsonArray shape = uv.getAsJsonArray();

            JsonArray size = new JsonArray();
            size.add(shape.get(2).getAsDouble() - shape.get(0).getAsDouble());
            size.add(shape.get(3).getAsDouble() - shape.get(1).getAsDouble());

            JsonArray from = new JsonArray();
            from.add(shape.get(0).getAsDouble());
            from.add(shape.get(1).getAsDouble());

            JsonObject faceParsed = new JsonObject();
            faceParsed.add("uv", from);
            faceParsed.add("uv_size", size);

            res.add(face, faceParsed);
        }

        return res;
    }

    private static JsonArray buildSize(JsonArray from, JsonArray to) {
        JsonArray builder = new JsonArray();

        for (int i = 0; i < from.size(); i++) {
            double from_ = from.get(i).getAsDouble();
            double to_ = to.get(i).getAsDouble();

            builder.add(Math.round((to_ - from_) * 1000) / 1000.0);
        }

        return builder;
    }
}