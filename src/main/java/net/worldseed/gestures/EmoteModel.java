package net.worldseed.gestures;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.GenericModelImpl;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.util.Map;

public class EmoteModel extends GenericModelImpl {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final JsonObject MODEL_JSON;

    // summon minecraft:item_display ~ ~1.4 ~ {Tags:["head"],item_display:"thirdperson_righthand",view_range:0.6f,transformation:{translation:[0.0f,0.0f,0.0f],left_rotation:[0.0f,0.0f,0.0f,1.0f],scale:[1.0f,1.0f,1.0f],right_rotation:[0.0f,0.0f,0.0f,1.0f]}}
    // summon minecraft:item_display ~ ~1.4 ~ {Tags:["arm_r"],item_display:"thirdperson_righthand",view_range:0.6f,transformation:{translation:[0.0f,-1024.0f,0.0f],left_rotation:[0.0f,0.0f,0.0f,1.0f],scale:[1.0f,1.0f,1.0f],right_rotation:[0.0f,0.0f,0.0f,1.0f]}}
    // summon minecraft:item_display ~ ~1.4 ~ {Tags:["arm_l"],item_display:"thirdperson_righthand",view_range:0.6f,transformation:{translation:[0.0f,-2048.0f,0.0f],left_rotation:[0.0f,0.0f,0.0f,1.0f],scale:[1.0f,1.0f,1.0f],right_rotation:[0.0f,0.0f,0.0f,1.0f]}}
    // summon minecraft:item_display ~ ~1.4 ~ {Tags:["torso"],item_display:"thirdperson_righthand",view_range:0.6f,transformation:{translation:[0.0f,-3072.0f,0.0f],left_rotation:[0.0f,0.0f,0.0f,1.0f],scale:[1.0f,1.0f,1.0f],right_rotation:[0.0f,0.0f,0.0f,1.0f]}}
    // summon minecraft:item_display ~ ~0.7 ~ {Tags:["leg_r"],item_display:"thirdperson_righthand",view_range:0.6f,transformation:{translation:[0.0f,-4096.0f,0.0f],left_rotation:[0.0f,0.0f,0.0f,1.0f],scale:[1.0f,1.0f,1.0f],right_rotation:[0.0f,0.0f,0.0f,1.0f]}}
    // summon minecraft:item_display ~ ~0.7 ~ {Tags:["leg_l"],item_display:"thirdperson_righthand",view_range:0.6f,transformation:{translation:[0.0f,-5120.0f,0.0f],left_rotation:[0.0f,0.0f,0.0f,1.0f],scale:[1.0f,1.0f,1.0f],right_rotation:[0.0f,0.0f,0.0f,1.0f]}}
    // item replace entity @e[tag=head] hotbar.0 with minecraft:player_head{SkullOwner:"Notch",CustomModelData:1}
    // item replace entity @e[tag=arm_r] hotbar.0 with minecraft:player_head{SkullOwner:"Notch",CustomModelData:2}
    // item replace entity @e[tag=arm_l] hotbar.0 with minecraft:player_head{SkullOwner:"Notch",CustomModelData:3}
    // item replace entity @e[tag=torso] hotbar.0 with minecraft:player_head{SkullOwner:"Notch",CustomModelData:4}
    // item replace entity @e[tag=leg_r] hotbar.0 with minecraft:player_head{SkullOwner:"Notch",CustomModelData:5}
    // item replace entity @e[tag=leg_l] hotbar.0 with minecraft:player_head{SkullOwner:"Notch",CustomModelData:6}

    private static final Map<String, Point> BONE_OFFSETS = Map.ofEntries(
            Map.entry("Head", new Vec(0, 0, 0)),
            Map.entry("RightArm", new Vec(1.17, 0, 0)),
            Map.entry("LeftArm", new Vec(-1.17, 0, 0)),
            Map.entry("Body", new Vec(0, 0, 0)),
            Map.entry("RightLeg", new Vec(0.4446, 0, 0)),
            Map.entry("LeftLeg", new Vec(-0.4446, 0, 0))
    );

    private static final Map<String, Double> VERTICAL_OFFSETS = Map.of(
            "Head", 1.4,
            "RightArm", 1.4,
            "LeftArm", 1.4,
            "Body", 1.4,
            "RightLeg", 0.7,
            "LeftLeg", 0.7
    );

    private static final Map<String, Integer> BONE_TRANSLATIONS = Map.of(
            "Head", 0,
            "RightArm", -1024,
            "LeftArm", -2048,
            "Body", -3072,
            "RightLeg", -4096,
            "LeftLeg", -5120
    );

    private static final Map<String, Point> BONE_DIFFS = Map.ofEntries(
            // Map.entry("RightArm", new Vec(0, 0, 0)),
            // Map.entry("LeftArm", new Vec(0, 0, 0))
    );

    static {
        MODEL_JSON = GSON.fromJson(new StringReader(SteveModel.MODEL_STRING), JsonObject.class);
    }

    private final PlayerSkin skin;

    public EmoteModel(PlayerSkin skin) {
        this.skin = skin;
    }

    @Override
    protected void registerBoneSuppliers() {
        boneSuppliers.put(name -> true, (info) -> {
            return new ModelBoneEmote(info.pivot(), info.name(), info.rotation(), info.model(), BONE_TRANSLATIONS.get(info.name()), VERTICAL_OFFSETS.getOrDefault(info.name(), 0.0), skin);
        });
    }

    @Override
    public boolean addViewer(@NotNull Player player) {
        System.out.println("Adding viewer " + player.getUsername());
        return super.addViewer(player);
    }

    @Override
    public boolean removeViewer(@NotNull Player player) {
        System.out.println("Removing viewer " + player.getUsername());
        return super.removeViewer(player);
    }

    @Override
    public String getId() {
        return null;
    }

    private void init_(@Nullable Instance instance, @NotNull Pos position) {
        this.instance = instance;
        this.setPosition(position);

        this.setGlobalRotation(position.yaw());

        try {
            super.loadBones(MODEL_JSON, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (ModelBone modelBonePart : this.parts.values()) {
            if (modelBonePart instanceof ModelBoneViewable)
                viewableBones.add((ModelBoneImpl) modelBonePart);

            modelBonePart.spawn(instance, modelBonePart.calculatePosition()).join();
        }

        draw();
    }

    public void init(@Nullable Instance instance, @NotNull Pos position) {
        this.init_(instance, position);
    }

    @Override
    public Point getDiff(String boneName) {
        return BONE_DIFFS.getOrDefault(boneName, null);
    }

    @Override
    public void setGlobalScale(float scale) {
    }

    @Override
    public Point getOffset(String boneName) {
        return BONE_OFFSETS.getOrDefault(boneName, Vec.ZERO);
    }
}
