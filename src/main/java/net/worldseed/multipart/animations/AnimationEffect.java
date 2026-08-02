package net.worldseed.multipart.animations;

/**
 * A sound / particle / timeline effect authored on a Blockbench animation's "effects" track, resolved to a
 * playback tick. Fired by the {@link AnimationHandler} while the owning animation plays (see
 * {@link AnimationEffectHandler}).
 *
 * @param animation the animation this effect belongs to
 * @param type      SOUND, PARTICLE or TIMELINE
 * @param tick      when it fires, in ticks from the start of the animation (Blockbench time * 20)
 * @param effect    the effect id — a sound key (SOUND) or particle id (PARTICLE); null for TIMELINE
 * @param locator   the locator/VFX bone the effect is anchored to, or null for the model origin
 * @param script    the Molang/script instruction (TIMELINE, and optional particle script), or null
 */
public record AnimationEffect(String animation, Type type, int tick, String effect, String locator, String script) {
    public enum Type { SOUND, PARTICLE, TIMELINE }
}
