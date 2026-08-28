package net.worldseed.gestures;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.worldseed.multipart.ModelLoader;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.BoneAnimation;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void emotePlayerUsesMinestomEquipmentAndWseeEntityApis() {
        EmotePlayer player = new EmotePlayer(instance, Pos.ZERO, new PlayerSkin("", ""));
        ItemStack sword = ItemStack.of(Material.DIAMOND_SWORD);
        ItemStack shield = ItemStack.of(Material.SHIELD);

        player.setItemInMainHand(sword);
        player.setItemInOffHand(shield);

        assertTrue(player.isInvisible());
        assertSame(player, player.getModel().getOwner());
        assertNotNull(player.getAnimationHandler());
        assertEquals(sword, player.getItemInMainHand());
        assertEquals(shield, player.getItemInOffHand());
        assertEquals(sword, player.getEquipment(EquipmentSlot.MAIN_HAND));
        assertEquals(shield, player.getEquipment(EquipmentSlot.OFF_HAND));
        assertEquals(sword, player.getModel().getMainHandItem());
        assertEquals(shield, player.getModel().getOffHandItem());
        assertEquals(0.1, player.getAttributeValue(net.minestom.server.entity.attribute.Attribute.MOVEMENT_SPEED), 1.0e-6);
        player.swingOffHand();
        assertEquals(VanillaPlayerAnimationState.AttackArm.LEFT, player.vanillaAnimationState().attackArm());
        assertEquals(1.0, player.vanillaAnimationState().attackTime(), 1.0e-6);

        player.clearHandItems();
        assertEquals(ItemStack.AIR, player.getItemInMainHand());
        assertEquals(ItemStack.AIR, player.getItemInOffHand());
        player.remove();
    }

    @Test
    void emotePlayerCanSpawnWhileAViewerIsAlreadyInRange() {
        PlayerConnection connection = new PlayerConnection() {
            @Override public void sendPacket(SendablePacket packet) { }
            @Override public SocketAddress getRemoteAddress() {
                return new InetSocketAddress("127.0.0.1", 25565);
            }
        };
        Player viewer = new Player(connection, new GameProfile(UUID.randomUUID(), "viewer"));
        viewer.setInstance(instance, Pos.ZERO).join();

        EmotePlayer emote = assertDoesNotThrow(() ->
                new EmotePlayer(instance, new Pos(1, 0, 1), new PlayerSkin("", "")));

        assertTrue(emote.getViewers().contains(viewer));
        emote.remove();
        viewer.remove();
    }

    @Test
    void ordinaryMinestomVelocityDrivesWalkingAndTheWseeModel() {
        EmotePlayer player = new EmotePlayer(instance, new Pos(0, 10, 0), new PlayerSkin("", ""));
        Pos before = player.getPosition();
        player.setVelocity(new Vec(1, 0, 0));
        player.tick(0);

        assertNotEquals(before, player.getPosition());
        assertTrue(player.vanillaAnimationState().walkAnimationSpeed() > 0);
        assertEquals(player.getPosition(), player.getModel().getPosition());
        player.remove();
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
        assertEquals(-15.0 / 256.0, heldMeta(model, "RightArm").getTranslation().x(), 1.0e-6);
        assertEquals(15.0 / 256.0, heldMeta(model, "LeftArm").getTranslation().x(), 1.0e-6);
        assertEquals(-15.0 * 10.0 / 256.0, heldMeta(model, "RightArm").getTranslation().y(), 1.0e-6);
        assertEquals(-15.0 * 10.0 / 256.0, heldMeta(model, "LeftArm").getTranslation().y(), 1.0e-6);
        assertEquals(15.0 / 16.0, heldMeta(model, "RightArm").getScale().x(), 1.0e-6);
        // The resource-pack model already contains vanilla's 15/16 render scale.
        // Metadata must remain identity or each independently-centred bone develops gaps.
        assertEquals(1.0, visibleMeta(model, "RightArm").getScale().x(), 1.0e-6);
        assertEquals(1, heldMeta(model, "RightArm").getTransformationInterpolationDuration());
        assertEquals(1, heldMeta(model, "RightArm").getPosRotInterpolationDuration());
        assertEquals(1, visibleMeta(model, "RightArm").getTransformationInterpolationDuration());
        assertEquals(1, visibleMeta(model, "RightArm").getPosRotInterpolationDuration());
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
        assertEquals(arm.getEntity().getPosition().x(),
                arm.getHeldItemEntity().getPosition().x(), 1.0e-6);
        assertEquals(arm.getEntity().getPosition().y() - 0.11, arm.getHeldItemEntity().getPosition().y(), 1.0e-6);
        double expectedDepth = 0.585033 - 1.0546875 * Math.sin(Math.toRadians(18.0));
        assertEquals(arm.getEntity().getPosition().z() + expectedDepth,
                arm.getHeldItemEntity().getPosition().z(), 1.0e-6);
        model.destroy();
    }

    @Test
    void idleBobKeepsBothItemsOnTheVanillaHandPivot() {
        EmoteModel model = model();
        VanillaPlayerAnimationState state = new VanillaPlayerAnimationState();
        state.setRightArmPose(VanillaPlayerAnimationState.ArmPose.ITEM);
        state.setLeftArmPose(VanillaPlayerAnimationState.ArmPose.ITEM);
        state.setAgeInTicks(20.0f);
        model.applyVanillaPose(state);

        double bobX = Math.sin(20.0 * 0.067) * 0.05;
        double rightDepth = 0.585033 - 1.0546875 * Math.sin(bobX);
        double leftDepth = 0.585033 + 1.0546875 * Math.sin(bobX);
        ModelBoneEmote right = (ModelBoneEmote) model.getPart("RightArm");
        ModelBoneEmote left = (ModelBoneEmote) model.getPart("LeftArm");

        assertEquals(right.getEntity().getPosition().z() + rightDepth,
                right.getHeldItemEntity().getPosition().z(), 1.0e-6);
        assertEquals(left.getEntity().getPosition().z() + leftDepth,
                left.getHeldItemEntity().getPosition().z(), 1.0e-6);
        model.destroy();
    }

    @Test
    void wholeModelRotationStaysOutsideTheLocalHandTransform() {
        EmoteModel model = model();
        VanillaPlayerAnimationState state = new VanillaPlayerAnimationState();
        state.setAgeInTicks(0);
        state.setRightArmPose(VanillaPlayerAnimationState.ArmPose.ITEM);
        model.applyVanillaPose(state);

        ItemDisplayMeta meta = heldMeta(model, "RightArm");
        Point localGrip = meta.getTranslation();
        float[] localRotation = meta.getLeftRotation();

        model.setGlobalRotation(90);
        model.applyVanillaPose(state);

        assertEquals(localGrip, meta.getTranslation());
        assertArrayEquals(localRotation, meta.getLeftRotation());
        assertEquals(90, ((ModelBoneEmote) model.getPart("RightArm")).getHeldItemEntity().getPosition().yaw(), 1.0e-6);
        model.destroy();
    }

    @Test
    void blockbenchCubesMatchVanillaPlayerModelAroundTheirPivots() throws Exception {
        try (var stream = getClass().getResourceAsStream("/bbmodel/steve.bbmodel")) {
            assertNotNull(stream);
            JsonObject model = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray elements = model.getAsJsonArray("elements");
            var expected = java.util.Map.of(
                    "Head", new double[]{-4, 24, -4, 4, 32, 4},
                    "Body", new double[]{-4, 12, -2, 4, 24, 2},
                    "Right Arm", new double[]{4, 12, -2, 8, 24, 2},
                    "Left Arm", new double[]{-8, 12, -2, -4, 24, 2},
                    "Right Leg", new double[]{-3.9, 0, -2, 0.1, 12, 2},
                    "Left Leg", new double[]{-0.1, 0, -2, 3.9, 12, 2});
            var counts = new java.util.HashMap<String, Integer>();
            for (var value : elements) {
                JsonObject cube = value.getAsJsonObject();
                String name = cube.get("name").getAsString();
                double[] bounds = expected.get(name);
                assertNotNull(bounds, "unexpected player cube " + name);
                assertVec(cube.getAsJsonArray("from"), bounds[0], bounds[1], bounds[2]);
                assertVec(cube.getAsJsonArray("to"), bounds[3], bounds[4], bounds[5]);
                counts.merge(name, 1, Integer::sum);
            }
            // Base skin plus outer skin layer for every vanilla part.
            expected.keySet().forEach(name -> assertEquals(2, counts.get(name), name));
        }
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

    private static ItemDisplayMeta visibleMeta(EmoteModel model, String boneName) {
        ModelBoneEmote bone = (ModelBoneEmote) model.getPart(boneName);
        return (ItemDisplayMeta) bone.getEntity().getEntityMeta();
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

    private static void assertVec(JsonArray actual, double x, double y, double z) {
        assertEquals(x, actual.get(0).getAsDouble(), 1.0e-9);
        assertEquals(y, actual.get(1).getAsDouble(), 1.0e-9);
        assertEquals(z, actual.get(2).getAsDouble(), 1.0e-9);
    }
}
