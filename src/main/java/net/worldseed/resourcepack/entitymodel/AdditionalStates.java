package net.worldseed.resourcepack.entitymodel;

import net.worldseed.resourcepack.entitymodel.generator.TextureGenerator;
import net.worldseed.resourcepack.entitymodel.parser.ModelParser;

import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.HashMap;
import java.util.Map;

public class AdditionalStates {
    public record StateDescription(Map<String, String> boneTextureMappings, ModelParser.TextureState state) {}
    public final Map<String, StateDescription> states = new HashMap<>();

    private AdditionalStates() { }
    public static AdditionalStates EMPTY() {
        return new AdditionalStates();
    }

    public AdditionalStates(JsonObject obj, Map<String, TextureGenerator.TextureData> data) {
        Map<String, String> nameMapping = new HashMap<>();

        for (var entry : data.entrySet()) {
            nameMapping.put(entry.getValue().name(), entry.getValue().id());
        }

        obj.forEach((k, v) -> {
            Map<String, String> mappings = new HashMap<>();

            v.asJsonObject().forEach((x, y) -> {
                mappings.put(x, nameMapping.get(((JsonString)y).getString()));
            });

            states.put(k, new StateDescription(mappings, new ModelParser.TextureState(1, 1, 1, 0, 0, 0, k)));
        });
    }

    @Override
    public String toString() {
        return "StateDescriptionList{" +
                "states=" + states +
                '}';
    }
}
