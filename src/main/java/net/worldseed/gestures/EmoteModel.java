package net.worldseed.gestures;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.metadata.PlayerHeadMeta;
import net.worldseed.multipart.GenericModelImpl;
import net.worldseed.multipart.ModelConfig;
import net.worldseed.multipart.model_bones.ModelBone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.util.*;

public class EmoteModel extends GenericModelImpl {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String MODEL_STRING = "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"geometry.unknown\",\"texture_width\":64,\"texture_height\":64},\"bones\":[{\"name\":\"right_leg\",\"pivot\":[-0.4446,2.8080000000000003,0.0],\"rotation\":[0,0,0],\"cubes\":[{\"origin\":[-0.936,0.0,-0.468],\"size\":[0.936,2.808,0.936],\"pivot\":[-0.0234,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[4.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"east\":{\"uv\":[0.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"south\":{\"uv\":[12.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"west\":{\"uv\":[8.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"up\":{\"uv\":[4.0,16.0],\"uv_size\":[4.0,4.0],\"texture\":\"0\"},\"down\":{\"uv\":[8.0,20.0],\"uv_size\":[4.0,-4.0],\"texture\":\"0\"}}},{\"origin\":[-0.936,0.0,-0.468],\"size\":[0.936,2.808,0.936],\"pivot\":[-0.0234,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[4.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"east\":{\"uv\":[0.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"south\":{\"uv\":[12.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"west\":{\"uv\":[8.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"up\":{\"uv\":[4.0,16.0],\"uv_size\":[4.0,4.0],\"texture\":\"0\"},\"down\":{\"uv\":[8.0,20.0],\"uv_size\":[4.0,-4.0],\"texture\":\"0\"}}}],\"parent\":\"model\"},{\"name\":\"left_leg\",\"pivot\":[0.4446,2.8080000000000003,0.0],\"rotation\":[0,0,0],\"cubes\":[{\"origin\":[0.0,0.0,-0.468],\"size\":[0.936,2.808,0.936],\"pivot\":[0.0234,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[20.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"east\":{\"uv\":[16.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"south\":{\"uv\":[28.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"west\":{\"uv\":[24.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"up\":{\"uv\":[20.0,48.0],\"uv_size\":[4.0,4.0],\"texture\":\"0\"},\"down\":{\"uv\":[24.0,52.0],\"uv_size\":[4.0,-4.0],\"texture\":\"0\"}}},{\"origin\":[0.0,0.0,-0.468],\"size\":[0.936,2.808,0.936],\"pivot\":[0.0234,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[20.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"east\":{\"uv\":[16.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"south\":{\"uv\":[28.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"west\":{\"uv\":[24.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"up\":{\"uv\":[20.0,48.0],\"uv_size\":[4.0,4.0],\"texture\":\"0\"},\"down\":{\"uv\":[24.0,52.0],\"uv_size\":[4.0,-4.0],\"texture\":\"0\"}}}],\"parent\":\"model\"},{\"name\":\"right_arm\",\"pivot\":[-1.1700000000000002,5.148000000000001,0.0],\"rotation\":[0,0,0],\"cubes\":[{\"origin\":[-1.872,2.808,-0.468],\"size\":[0.936,2.808,0.936],\"pivot\":[0.0,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[44.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"east\":{\"uv\":[40.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"south\":{\"uv\":[52.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"west\":{\"uv\":[48.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"up\":{\"uv\":[44.0,16.0],\"uv_size\":[4.0,4.0],\"texture\":\"0\"},\"down\":{\"uv\":[48.0,20.0],\"uv_size\":[4.0,-4.0],\"texture\":\"0\"}}},{\"origin\":[-1.872,2.808,-0.468],\"size\":[0.936,2.808,0.936],\"pivot\":[0.0,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[44.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"east\":{\"uv\":[40.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"south\":{\"uv\":[52.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"west\":{\"uv\":[48.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"up\":{\"uv\":[44.0,16.0],\"uv_size\":[4.0,4.0],\"texture\":\"0\"},\"down\":{\"uv\":[48.0,20.0],\"uv_size\":[4.0,-4.0],\"texture\":\"0\"}}}],\"parent\":\"body\"},{\"name\":\"left_arm\",\"pivot\":[1.1700000000000002,5.148000000000001,0.0],\"rotation\":[0,0,0],\"cubes\":[{\"origin\":[0.936,2.808,-0.468],\"size\":[0.936,2.808,0.936],\"pivot\":[0.0,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[36.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"east\":{\"uv\":[32.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"south\":{\"uv\":[44.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"west\":{\"uv\":[40.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"up\":{\"uv\":[36.0,48.0],\"uv_size\":[4.0,4.0],\"texture\":\"0\"},\"down\":{\"uv\":[40.0,52.0],\"uv_size\":[4.0,-4.0],\"texture\":\"0\"}}},{\"origin\":[0.936,2.808,-0.468],\"size\":[0.936,2.808,0.936],\"pivot\":[0.0,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[36.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"east\":{\"uv\":[32.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"south\":{\"uv\":[44.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"west\":{\"uv\":[40.0,52.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"up\":{\"uv\":[36.0,48.0],\"uv_size\":[4.0,4.0],\"texture\":\"0\"},\"down\":{\"uv\":[40.0,52.0],\"uv_size\":[4.0,-4.0],\"texture\":\"0\"}}}],\"parent\":\"body\"},{\"name\":\"body\",\"pivot\":[0.0,2.8660000000000005,0.0],\"rotation\":[0,0,0],\"cubes\":[{\"origin\":[-0.936,2.808,-0.468],\"size\":[1.872,2.808,0.936],\"pivot\":[0.0,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[20.0,20.0],\"uv_size\":[8.0,12.0],\"texture\":\"0\"},\"east\":{\"uv\":[16.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"south\":{\"uv\":[32.0,20.0],\"uv_size\":[8.0,12.0],\"texture\":\"0\"},\"west\":{\"uv\":[28.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"up\":{\"uv\":[20.0,16.0],\"uv_size\":[8.0,4.0],\"texture\":\"0\"},\"down\":{\"uv\":[28.0,20.0],\"uv_size\":[8.0,-4.0],\"texture\":\"0\"}}},{\"origin\":[-0.936,2.808,-0.468],\"size\":[1.872,2.808,0.936],\"pivot\":[0.0,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[20.0,20.0],\"uv_size\":[8.0,12.0],\"texture\":\"0\"},\"east\":{\"uv\":[16.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"south\":{\"uv\":[32.0,20.0],\"uv_size\":[8.0,12.0],\"texture\":\"0\"},\"west\":{\"uv\":[28.0,20.0],\"uv_size\":[4.0,12.0],\"texture\":\"0\"},\"up\":{\"uv\":[20.0,16.0],\"uv_size\":[8.0,4.0],\"texture\":\"0\"},\"down\":{\"uv\":[28.0,20.0],\"uv_size\":[8.0,-4.0],\"texture\":\"0\"}}}],\"parent\":\"torso\"},{\"name\":\"_head\",\"pivot\":[0.0,5.6160000000000005,0.0],\"rotation\":[0,0,0],\"cubes\":[{\"origin\":[-0.936,5.616,-0.936],\"size\":[1.872,1.872,1.872],\"pivot\":[0.0,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[8.0,8.0],\"uv_size\":[8.0,8.0],\"texture\":\"0\"},\"east\":{\"uv\":[0.0,8.0],\"uv_size\":[8.0,8.0],\"texture\":\"0\"},\"south\":{\"uv\":[24.0,8.0],\"uv_size\":[8.0,8.0],\"texture\":\"0\"},\"west\":{\"uv\":[16.0,8.0],\"uv_size\":[8.0,8.0],\"texture\":\"0\"},\"up\":{\"uv\":[8.0,0.0],\"uv_size\":[8.0,8.0],\"texture\":\"0\"},\"down\":{\"uv\":[16.0,8.0],\"uv_size\":[8.0,-8.0],\"texture\":\"0\"}}},{\"origin\":[-0.936,5.616,-0.936],\"size\":[1.872,1.872,1.872],\"pivot\":[0.0,0.0,0.0],\"rotation\":[0,0,0],\"uv\":{\"north\":{\"uv\":[8.0,8.0],\"uv_size\":[8.0,8.0],\"texture\":\"0\"},\"east\":{\"uv\":[0.0,8.0],\"uv_size\":[8.0,8.0],\"texture\":\"0\"},\"south\":{\"uv\":[24.0,8.0],\"uv_size\":[8.0,8.0],\"texture\":\"0\"},\"west\":{\"uv\":[16.0,8.0],\"uv_size\":[8.0,8.0],\"texture\":\"0\"},\"up\":{\"uv\":[8.0,0.0],\"uv_size\":[8.0,8.0],\"texture\":\"0\"},\"down\":{\"uv\":[16.0,8.0],\"uv_size\":[8.0,-8.0],\"texture\":\"0\"}}}],\"parent\":\"torso\"},{\"name\":\"torso\",\"pivot\":[0.0,2.75,0.0],\"rotation\":[0,0,0],\"cubes\":[],\"parent\":\"model\"},{\"name\":\"model\",\"pivot\":[0.0,0.0,0.0],\"rotation\":[0,0,0],\"cubes\":[]}]}]}";
    private static final JsonObject MODEL_JSON;

    private static final Map<String, Point> BONE_OFFSETS = Map.ofEntries(
            Map.entry("_head", new Vec(0.0,5.616,0.0)),
            Map.entry("body", new Vec(0.0,5.616,0.0)),
            Map.entry("right_arm", new Vec(1.17,5.148,0.0)),
            Map.entry("left_arm", new Vec(-1.17,5.148,0.0)),
            Map.entry("right_leg", new Vec(0.4446,2.808,0.0)),
            Map.entry("left_leg", new Vec(-0.4446,2.808,0.0))
    );

    private static final Map<String, Point> BONE_DIFFS = Map.ofEntries(
            Map.entry("_head", new Vec(8.936,8.0,8.936)),
            Map.entry("body", new Vec(8.936,10.808,8.468)),
            Map.entry("right_arm", new Vec(8.234,10.34,8.468)),
            Map.entry("left_arm", new Vec(8.702,10.34,8.468)),
            Map.entry("right_leg", new Vec(8.4446,10.808,8.468)),
            Map.entry("left_leg", new Vec(8.4914,10.808,8.468))
    );

    private static final String[] SPAWN_ORDER = {
            "_head",
            "body",
            "right_arm",
            "left_arm",
            "right_leg",
            "left_leg",
    };

    static {
        MODEL_JSON = GSON.fromJson(new StringReader(MODEL_STRING), JsonObject.class);
    }

    private final List<ModelBone> standBones = new ArrayList<>();
    private final PlayerSkin skin;

    public EmoteModel(PlayerSkin skin) {
        this.skin = skin;
    }

    @Override
    public String getId() {
        return null;
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, ModelConfig config, LivingEntity masterEntity) {
        this.config = config;
        this.instance = instance;
        this.setPosition(position);

        this.setGlobalRotation(position.yaw());
        loadBones(MODEL_JSON, masterEntity);

        for (String boneName : SPAWN_ORDER) {
            ModelBone bone = this.parts.get(boneName);
            standBones.add(bone);
            if (bone != null) {
                bone.spawn(instance, bone.calculatePosition()).join();
            }
        }

        draw();
        draw();
        draw();
    }

    public void init(@Nullable Instance instance, @NotNull Pos position, LivingEntity masterEntity) {
        this.init(instance, position, new ModelConfig(
            ModelConfig.ModelType.ARMOUR_STAND,
            ModelConfig.InterpolationType.POSITION_INTERPOLATION,
            ModelConfig.Size.NORMAL,
            ModelConfig.ItemSlot.HAND
        ), masterEntity);

        Map<Integer, ItemStack> heads = new HashMap<>();
        for (int i = 1; i <= SPAWN_ORDER.length; ++i) {
            int finalI = i;
            ItemStack playerHeads = ItemStack.builder(Material.PLAYER_HEAD)
                    .meta(PlayerHeadMeta.class, meta -> {
                        meta.skullOwner(UUID.randomUUID()).playerSkin(skin);
                        meta.customModelData(finalI);
                    }).build();

            heads.put(i, playerHeads);
        }

        setHeads(heads);
    }

    private void setHeads(Map<Integer, ItemStack> heads) {
        for (int i = 0; i < SPAWN_ORDER.length; ++i) {
            Entity f = standBones.get(i).getEntity();
            ((LivingEntity) f).setItemInMainHand(heads.get(i+1));
        }
    }

    @Override
    public Point getDiff(String boneName) {
        return BONE_DIFFS.get(boneName);
    }

    @Override
    public Point getOffset(String boneName) {
        return BONE_OFFSETS.get(boneName);
    }
}
