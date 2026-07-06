package net.worldseed.multipart;

import net.kyori.adventure.util.RGBLike;
import net.minestom.server.Viewable;
import net.minestom.server.collision.Shape;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventHandler;
import net.minestom.server.instance.Instance;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import net.worldseed.multipart.events.ModelEvent;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GenericModel extends Viewable, EventHandler<@NonNull ModelEvent>, Shape {
    /**
     * Get the ID of the model
     *
     * @return the model ID
     */
    String getId();

    /**
     * Get the pivot point of the model. Used for global rotation
     *
     * @return the global rotation pivot point
     */
    Point getPivot();

    /**
     * Get the rotation of the model on the X axis
     *
     * @return the pitch
     */
    double getPitch();

    /**
     * Get the rotation of the model on the Y axis
     *
     * @return the global rotation
     */
    double getGlobalRotation();

    /**
     * Set the rotation of the model on the Y axis
     *
     * @param rotation new global rotation
     */
    void setGlobalRotation(double rotation);

    /**
     * Set the rotation of the model on the Y and X axis
     *
     * @param yaw   new global rotation
     * @param pitch new pitch
     */
    void setGlobalRotation(double yaw, double pitch);

    /**
     * Get the postion offset for drawing the model
     *
     * @return the position
     */
    Point getGlobalOffset();

    /**
     * Get the position the model is being drawn at
     *
     * @return the model position
     */
    Pos getPosition();

    /**
     * Set the position of the model
     *
     * @param pos new model position
     */
    void setPosition(Pos pos);

    /**
     * Set the state of the model. By default, `normal` and `hit` are supported
     *
     * @param state the new state
     */
    void setState(String state);

    /**
     * Spawn the model's bones into the given instance at the given position. Usually called for you by
     * {@link ModelEntity}; call it directly only when driving a model without a {@link ModelEntity}.
     *
     * @param instance the instance to spawn in
     * @param position the position to spawn at
     */
    void init(@Nullable Instance instance, @NotNull Pos position);

    /**
     * Destroy the model
     */
    void destroy();

    void mountEntity(String name, Entity entity);

    void dismountEntity(String name, Entity entity);

    Collection<Entity> getPassengers(String name);

    /**
     * Get a VFX bone location
     *
     * @param name the name of the bone
     * @return the bone location
     */
    Point getVFX(String name);

    /**
     * Get a Blockbench locator's current world position, or null if there's no such locator/bone. Useful for
     * anchoring particles, projectiles or attached entities at authored points.
     *
     * @param name the locator (or bone) name
     * @return the world position, or null
     */
    Point getLocator(String name);

    @ApiStatus.Internal
    ModelBone getPart(String boneName);

    /**
     * Show or hide a bone at runtime (e.g. damage states, equipment, phase changes). A hidden bone's
     * display entity is removed for all current and future viewers until shown again.
     *
     * @param boneName the bone to toggle
     * @param visible  true to show, false to hide
     */
    void setBoneVisible(String boneName, boolean visible);

    @ApiStatus.Internal
    void draw();

    /**
     * Set the model's head rotation
     *
     * @param name     name of the bone
     * @param rotation rotation of head
     */
    void setHeadRotation(String name, double rotation);

    @NotNull List<ModelBone> getParts();

    Instance getInstance();

    /**
     * The entity that owns/drives this model — the source of its position and viewers, and the entity
     * that receives hits landed on the model's hitboxes (as normal Minestom events). Set automatically
     * when the model is bound to a {@link ModelEntity}; null if the model is driven manually.
     *
     * @return the owning entity, or null
     */
    @Nullable Entity getOwner();

    /**
     * Set the owning entity (see {@link #getOwner()}).
     *
     * @param owner the owning entity, or null to unbind
     */
    void setOwner(@Nullable Entity owner);

    Point getOffset(String bone);

    Point getDiff(String bone);

    void triggerAnimationEnd(String animation, AnimationHandlerImpl.AnimationDirection direction);

    void setGlobalScale(float scale);

    void removeGlowing();

    void setGlowing(RGBLike color);

    void removeGlowing(Player player);

    void setGlowing(Player player, RGBLike color);

    void attachModel(GenericModel model, String boneName);

    Map<String, List<GenericModel>> getAttachedModels();

    void detachModel(GenericModel model, String boneName);

    @Nullable BoneEntity generateRoot();

    void bindNametag(String name, Entity nametag);

    /**
     * Create a floating {@link net.minestom.server.entity.EntityType#TEXT_DISPLAY} showing {@code text} and
     * bind it to the given nametag bone — the modern replacement for attaching an armor stand. Returns the
     * created entity so you can tweak its meta (background, billboard, line width…).
     *
     * @param name the nametag bone name
     * @param text the text to show
     * @return the created text-display entity
     */
    Entity setNametag(String name, net.kyori.adventure.text.Component text);

    void unbindNametag(String name);

    @Nullable Entity getNametag(String name);

    void addPartsAsPassengers(Player player);
}
