package emotes;

import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.instance.Instance;
import net.worldseed.gestures.EmotePlayer;
import net.worldseed.multipart.ModelLoader;

import java.util.Map;

public class EmoteExample extends EmotePlayer {
    private static final String ANIMATION_STRING = "{\"format_version\":\"1.8.0\",\"animations\":{\"dab\":{\"animation_length\":2,\"bones\":{\"Head\":{\"rotation\":{\"0.0\":[0,0,0],\"0.45\":[32.5,0,0],\"1.45\":[32.5,0,0],\"1.8\":[0,0,0]}},\"RightArm\":{\"rotation\":{\"0.0\":[0,0,0],\"0.45\":[-47.5,0,-100],\"0.85\":[0,0,100],\"1.25\":[-47.5,0,-100],\"1.8\":[0,0,0]}},\"LeftArm\":{\"rotation\":{\"0.0\":[0,0,0],\"0.45\":[0,0,-100],\"0.85\":[-47.5,0,100],\"1.25\":[0,0,-100],\"1.8\":[0,0,0]}},\"LeftLeg\":{\"rotation\":{\"0.0\":[0,0,0],\"1.45\":[10,0,0],\"1.8\":[0,0,0]}},\"Torso\":{\"rotation\":{\"0.0\":[0,0,0],\"1.45\":[5,0,0],\"1.8\":[0,0,0]}}}},\"wave\":{\"animation_length\":4,\"bones\":{\"right_arm\":{\"rotation\":{\"0.0\":[0,0,0],\"0.3\":[0,0,137.5],\"4.0\":[0,0,0]},\"position\":{\"0.0\":[0,0,0],\"0.3\":[-0.25,0,0],\"4.0\":[0,0,0]}}}}},\"geckolib_format_version\":2}";
    private static final Map<String, JsonObject> ANIMATIONS;

    static {
        ANIMATIONS = ModelLoader.parseAnimations(ANIMATION_STRING);
    }

    public EmoteExample(Instance instance, Pos pos, PlayerSkin skin) {
        super(instance, pos, skin);
        loadEmotes(ANIMATIONS);
        getAnimationHandler().playRepeat("dab");
    }
}
