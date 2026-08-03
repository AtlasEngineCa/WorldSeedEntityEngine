package net.worldseed.gestures;

import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import net.worldseed.multipart.ModelEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.EnumMap;

/**
 * A transparent Minestom living entity backed by WSEE's vanilla player model.
 *
 * <p>Use ordinary Minestom entity APIs for movement, facing and equipment. In
 * particular, {@link #setItemInMainHand(ItemStack)} and
 * {@link #setItemInOffHand(ItemStack)} update the rendered hands without exposing
 * equipment on the invisible carrier. WSEE-specific animations remain available
 * through {@link #getAnimationHandler()} and {@link #getModel()}.</p>
 */
public class EmotePlayer extends ModelEntity {

    private final EmoteModel model;
    private final AnimationHandler animationHandler;
    private int emoteIndex = 0;
    private final VanillaPlayerAnimationState vanillaAnimationState = new VanillaPlayerAnimationState();
    private boolean vanillaAnimationsEnabled = true;
    private final Map<EquipmentSlot, ItemStack> visualEquipment = new EnumMap<>(EquipmentSlot.class);

    public EmotePlayer(Instance instance, Pos pos, PlayerSkin skin, EntityType entityType) {
        this(new EmoteModel(skin), instance, pos, entityType);
    }

    private EmotePlayer(EmoteModel model, Instance instance, Pos pos, EntityType entityType) {
        super(entityType, model, instance, pos);
        this.model = model;

        setBoundingBox(0.8, 1.8, 0.8);
        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1f);

        this.animationHandler = new AnimationHandlerImpl(model) {
            @Override
            protected void loadDefaultAnimations() {
            }
        };

        // Hits on the emote model surface as normal Minestom events; keep the emote itself invulnerable.
        this.eventNode().addListener(EntityDamageEvent.class, event -> event.setCancelled(true));

        this.model.draw();
        this.model.draw();
    }

    public EmotePlayer(Instance instance, Pos pos, PlayerSkin skin) {
        this(instance, pos, skin, EntityType.ZOMBIE);
    }

    /**
     * Loads the emotes into the animation handler
     *
     * @param emotes Map containing the emote name, and emote data
     */
    public void loadEmotes(Map<String, JsonObject> emotes) {
        for (Map.Entry<String, JsonObject> entry : emotes.entrySet()) {
            this.animationHandler.registerAnimation(entry.getKey(), entry.getValue(), emoteIndex);
            emoteIndex++;
        }
    }

    @Override
    public void tick(long time) {
        var position = this.getPosition();
        super.tick(time);
        if (isRemoved()) return;
        var newPosition = this.getPosition();
        model.setGlobalRotation(newPosition.yaw(), newPosition.pitch());
        if (vanillaAnimationsEnabled) {
            vanillaAnimationState.setAgeInTicks(vanillaAnimationState.ageInTicks() + 1);
            vanillaAnimationState.setHeadYaw(0);
            vanillaAnimationState.setHeadPitch(newPosition.pitch());
            // Minecraft 26.2 LivingEntity#updateWalkAnimation / WalkAnimationState#update:
            // measure this tick's horizontal displacement, smooth toward it by 0.4,
            // then advance the phase by the smoothed speed.
            float targetSpeed = (float) Math.min(1.0,
                    Math.hypot(newPosition.x() - position.x(), newPosition.z() - position.z()) * 4.0);
            float walkSpeed = vanillaAnimationState.walkAnimationSpeed();
            walkSpeed += (targetSpeed - walkSpeed) * 0.4f;
            vanillaAnimationState.setWalkAnimationSpeed(walkSpeed);
            vanillaAnimationState.setWalkAnimationPos(vanillaAnimationState.walkAnimationPos() + walkSpeed);
            if (vanillaAnimationState.attackTime() > 0) {
                vanillaAnimationState.setAttackTime(Math.max(0, vanillaAnimationState.attackTime() - 0.1f));
            }
            model.applyVanillaPose(vanillaAnimationState);
        }
    }

    public VanillaPlayerAnimationState vanillaAnimationState() {
        return vanillaAnimationState;
    }

    public void setVanillaAnimationsEnabled(boolean enabled) {
        this.vanillaAnimationsEnabled = enabled;
        if (enabled) model.applyVanillaPose(vanillaAnimationState);
    }

    public boolean vanillaAnimationsEnabled() {
        return vanillaAnimationsEnabled;
    }

    /** Apply the current vanilla animation inputs immediately without advancing their clock. */
    public void applyVanillaAnimationState() {
        model.applyVanillaPose(vanillaAnimationState);
    }

    public void swingMainHand() {
        super.swingMainHand();
        vanillaAnimationState.setAttackArm(VanillaPlayerAnimationState.AttackArm.RIGHT);
        vanillaAnimationState.setAttackTime(1.0f);
    }

    @Override
    public void swingOffHand() {
        super.swingOffHand();
        vanillaAnimationState.setAttackArm(VanillaPlayerAnimationState.AttackArm.LEFT);
        vanillaAnimationState.setAttackTime(1.0f);
    }

    @Override
    public void swingMainHand(boolean sendPacketToSelf) {
        super.swingMainHand(sendPacketToSelf);
        vanillaAnimationState.setAttackArm(VanillaPlayerAnimationState.AttackArm.RIGHT);
        vanillaAnimationState.setAttackTime(1.0f);
    }

    @Override
    public void swingOffHand(boolean sendPacketToSelf) {
        super.swingOffHand(sendPacketToSelf);
        vanillaAnimationState.setAttackArm(VanillaPlayerAnimationState.AttackArm.LEFT);
        vanillaAnimationState.setAttackTime(1.0f);
    }

    public void setRotation(float yaw) {
        Pos position = getPosition().withYaw(yaw);
        teleport(position);
        model.setGlobalRotation(yaw, position.pitch());
    }

    /**
     * Set the item rendered in this emote player's right (main) hand. The item follows all
     * translations and rotations applied to the right arm.
     *
     * @param item item to render, or {@link ItemStack#AIR} to clear the hand
     */
    public void setMainHandItem(@NotNull ItemStack item) {
        setItemInMainHand(item);
    }

    /** @return the item currently rendered in the right (main) hand */
    public @NotNull ItemStack getMainHandItem() {
        return getItemInMainHand();
    }

    /**
     * Set the item rendered in this emote player's left (off) hand. The item follows all
     * translations and rotations applied to the left arm.
     *
     * @param item item to render, or {@link ItemStack#AIR} to clear the hand
     */
    public void setOffHandItem(@NotNull ItemStack item) {
        setItemInOffHand(item);
    }

    /** @return the item currently rendered in the left (off) hand */
    public @NotNull ItemStack getOffHandItem() {
        return getItemInOffHand();
    }

    /** Clear both rendered hand items. */
    public void clearHandItems() {
        setMainHandItem(ItemStack.AIR);
        setOffHandItem(ItemStack.AIR);
    }

    @Override
    public @NotNull ItemStack getEquipment(@NotNull EquipmentSlot slot) {
        return visualEquipment.getOrDefault(slot, ItemStack.AIR);
    }

    @Override
    public void setEquipment(@NotNull EquipmentSlot slot, @NotNull ItemStack item) {
        if (item.equals(ItemStack.AIR)) visualEquipment.remove(slot);
        else visualEquipment.put(slot, item);
        switch (slot) {
            case MAIN_HAND -> {
                model.setMainHandItem(item);
                vanillaAnimationState.setRightArmPose(item.equals(ItemStack.AIR)
                        ? VanillaPlayerAnimationState.ArmPose.EMPTY
                        : VanillaPlayerAnimationState.ArmPose.ITEM);
            }
            case OFF_HAND -> {
                model.setOffHandItem(item);
                vanillaAnimationState.setLeftArmPose(item.equals(ItemStack.AIR)
                        ? VanillaPlayerAnimationState.ArmPose.EMPTY
                        : VanillaPlayerAnimationState.ArmPose.ITEM);
            }
            default -> { }
        }
        model.applyVanillaPose(vanillaAnimationState);
    }

    @Override
    public @NotNull EmoteModel getModel() {
        return model;
    }

    public @NotNull AnimationHandler getAnimationHandler() {
        return animationHandler;
    }

    @Override
    public void remove() {
        animationHandler.destroy();
        super.remove();
    }
}
