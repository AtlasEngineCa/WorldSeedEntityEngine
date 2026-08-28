package net.worldseed.multipart.animations;

import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.model_bones.ModelBone;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationHandlerRepeatTest {
    @Test
    void stoppingActiveRepeatResumesPreviousRepeat() {
        GenericModel model = (GenericModel) Proxy.newProxyInstance(
                GenericModel.class.getClassLoader(),
                new Class<?>[]{GenericModel.class},
                (_, method, _) -> method.getName().equals("getParts")
                        ? List.of(dummyBone())
                        : defaultValue(method.getReturnType()));
        var handler = new TestHandler(model);
        var idle = new TestAnimation("idle", 0);
        var walk = new TestAnimation("walk", -1);
        handler.registerAnimation(idle);
        handler.registerAnimation(walk);

        handler.playRepeat("idle");
        handler.playRepeat("walk");
        handler.stopRepeat("walk");

        assertEquals("idle", handler.getRepeating());
        assertTrue(idle.playCount >= 2, "idle must be restarted after walk stops");
    }

    private static ModelBone dummyBone() {
        return (ModelBone) Proxy.newProxyInstance(
                ModelBone.class.getClassLoader(), new Class<?>[]{ModelBone.class},
                (_, method, _) -> defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        return 0D;
    }

    private static final class TestHandler extends AnimationHandlerImpl {
        private TestHandler(GenericModel model) {
            super(model, false);
        }

        @Override
        protected void loadDefaultAnimations() {
        }
    }

    private static final class TestAnimation implements ModelAnimation {
        private final String name;
        private final int priority;
        private AnimationHandler.AnimationDirection direction = AnimationHandler.AnimationDirection.PAUSE;
        private int playCount;

        private TestAnimation(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override public int priority() { return priority; }
        @Override public int animationTime() { return 20; }
        @Override public String name() { return name; }
        @Override public AnimationHandler.AnimationDirection direction() { return direction; }
        @Override public Set<String> getAnimatedBones() { return Set.of(); }
        @Override public void setDirection(AnimationHandler.AnimationDirection direction) { this.direction = direction; }
        @Override public void stop() { direction = AnimationHandler.AnimationDirection.PAUSE; }
        @Override public void stop(Set<String> animatedBones) { stop(); }
        @Override public void play(boolean resume) { playCount++; }
        @Override public void tick() { }
    }
}
