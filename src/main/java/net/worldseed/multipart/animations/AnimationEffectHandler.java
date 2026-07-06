package net.worldseed.multipart.animations;

import net.worldseed.multipart.GenericModel;

/**
 * Handles a Blockbench animation effect (sound / particle / timeline) when it fires. Set one on an
 * {@link AnimationHandler} via {@link AnimationHandler#setEffectHandler} to map effect ids to your own
 * content; the default handler ({@link AnimationHandler#DEFAULT_EFFECT_HANDLER}) plays the sound / particle
 * whose id is a valid Minecraft key at the effect's locator (or the model origin) for the model's viewers.
 */
@FunctionalInterface
public interface AnimationEffectHandler {
    void onEffect(GenericModel model, AnimationEffect effect);
}
