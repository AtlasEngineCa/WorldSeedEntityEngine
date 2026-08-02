package net.worldseed.gestures;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.worldseed.multipart.ModelLoader;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.BoneAnimation;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EmoteHeldItemTest {
    private static Instance instance;

    @BeforeAll
    static void startServer() {
        MinecraftServer.init();
        instance = MinecraftServer.getInstanceManager().createInstanceContainer();
    }

    @AfterAll
    static void stopServer() {
        MinecraftServer.stopCleanly();
    }

    @Test
    void itemsUseTheCorrectHandContextAndCanBeCleared() {
        EmoteModel model = model();
        ItemStack sword = ItemStack.of(Material.DIAMOND_SWORD);
        ItemStack shield = ItemStack.of(Material.SHIELD);

        model.setMainHandItem(sword);
        model.setOffHandItem(shield);

        assertEquals(sword, model.getMainHandItem());
        assertEquals(shield, model.getOffHandItem());
        assertEquals(ItemDisplayMeta.DisplayContext.THIRDPERSON_RIGHT_HAND, heldMeta(model, "RightArm").getDisplayContext());
        assertEquals(ItemDisplayMeta.DisplayContext.THIRDPERSON_LEFT_HAND, heldMeta(model, "LeftArm").getDisplayContext());
        assertEquals(15.0 / 256.0, Math.abs(heldMeta(model, "RightArm").getTranslation().x()), 1.0e-6);
        assertEquals(15.0 / 256.0, Math.abs(heldMeta(model, "LeftArm").getTranslation().x()), 1.0e-6);
        assertEquals(-15.0 * 10.0 / 256.0, heldMeta(model, "RightArm").getTranslation().y(), 1.0e-6);
        assertEquals(-15.0 * 10.0 / 256.0, heldMeta(model, "LeftArm").getTranslation().y(), 1.0e-6);
        assertEquals(15.0 / 16.0, heldMeta(model, "RightArm").getScale().x(), 1.0e-6);
        assertArrayEquals(
                heldMeta(model, "RightArm").getRightRotation(),
                heldMeta(model, "LeftArm").getRightRotation());

        model.setMainHandItem(ItemStack.AIR);
        model.setOffHandItem(ItemStack.AIR);
        assertEquals(ItemStack.AIR, model.getMainHandItem());
        assertEquals(ItemStack.AIR, model.getOffHandItem());
        model.destroy();
    }

    @Test
    void heldItemMovesWithItsAnimatedArm() {
        EmoteModel model = model();
        ModelBoneEmote arm = (ModelBoneEmote) model.getPart("RightArm");
        float[] resting = heldMeta(model, "RightArm").getLeftRotation();
        Point restingGrip = heldMeta(model, "RightArm").getTranslation();

        arm.addAnimation(rotation(new Vec(0, 0, 90)));
        ModelBoneImpl.beginDrawFrame();
        model.draw();

        assertFalse(java.util.Arrays.equals(resting, heldMeta(model, "RightArm").getLeftRotation()));
        assertNotEquals(restingGrip, heldMeta(model, "RightArm").getTranslation());
        assertEquals(arm.getEntity().getPosition(), arm.getHeldItemEntity().getPosition());
        model.destroy();
    }

    private static EmoteModel model() {
        EmoteModel model = new EmoteModel(new PlayerSkin("", ""));
        model.init(instance, Pos.ZERO);
        return model;
    }

    private static ItemDisplayMeta heldMeta(EmoteModel model, String armName) {
        ModelBoneEmote arm = (ModelBoneEmote) model.getPart(armName);
        return (ItemDisplayMeta) arm.getHeldItemEntity().getEntityMeta();
    }

    private static BoneAnimation rotation(Point value) {
        return new BoneAnimation() {
            @Override public String name() { return "test"; }
            @Override public String boneName() { return "RightArm"; }
            @Override public ModelLoader.AnimationType getType() { return ModelLoader.AnimationType.ROTATION; }
            @Override public Point getTransformAtTime(int time) { return value; }
            @Override public boolean isPlaying() { return true; }
            @Override public Point getTransform() { return value; }
            @Override public void setDirection(AnimationHandler.AnimationDirection direction) { }
            @Override public void stop() { }
            @Override public void play() { }
            @Override public void tick() { }
            @Override public void resume(short tick) { }
            @Override public short getTick() { return 0; }
        };
    }
}
