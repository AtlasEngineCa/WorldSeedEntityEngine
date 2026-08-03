package net.worldseed.gestures;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

import java.util.HashMap;
import java.util.Map;

/** Direct port of the Minecraft 26.2 HumanoidModel pose equations. */
final class VanillaPlayerPose {
    private static final float PI = (float) Math.PI;

    private VanillaPlayerPose() {}

    record Pose(Map<String, Point> rotations, Map<String, Point> translations) {}

    static Map<String, Point> rotations(VanillaPlayerAnimationState state) {
        return pose(state).rotations();
    }

    static Pose pose(VanillaPlayerAnimationState state) {
        Part head = new Part(rad(state.headPitch()), rad(state.headYaw()), 0);
        Part body = new Part();
        float pos = state.walkAnimationPos();
        float speed = state.walkAnimationSpeed();
        Part rightArm = new Part(cos(pos * 0.6662F + PI) * speed, 0, 0);
        Part leftArm = new Part(cos(pos * 0.6662F) * speed, 0, 0);
        Part rightLeg = new Part(cos(pos * 0.6662F) * 1.4F * speed, 0.005F, 0.005F);
        Part leftLeg = new Part(cos(pos * 0.6662F + PI) * 1.4F * speed, -0.005F, -0.005F);

        if (state.passenger()) {
            rightArm.x += -PI / 5;
            leftArm.x += -PI / 5;
            rightLeg.set(-1.4137167F, PI / 10, 0.07853982F);
            leftLeg.set(-1.4137167F, -PI / 10, -0.07853982F);
        }

        if (isTwoHanded(state.rightArmPose())) {
            poseArm(state.rightArmPose(), rightArm, leftArm, head, true);
        } else if (isTwoHanded(state.leftArmPose())) {
            poseArm(state.leftArmPose(), leftArm, rightArm, head, false);
        } else {
            poseArm(state.rightArmPose(), rightArm, leftArm, head, true);
            poseArm(state.leftArmPose(), leftArm, rightArm, head, false);
        }
        Map<String, Point> positions = basePositions();
        attack(state.attackTime(), state.attackArm(), rightArm, leftArm, body, head, positions);

        if (state.crouching()) {
            body.x = 0.5F;
            rightArm.x += 0.4F;
            leftArm.x += 0.4F;
            positions.put("Head", new Vec(0, 4.2F, 0));
            positions.put("Body", new Vec(0, 3.2F, 0));
            positions.put("RightArm", positions.get("RightArm").add(0, 3.2F, 0));
            positions.put("LeftArm", positions.get("LeftArm").add(0, 3.2F, 0));
            positions.put("RightLeg", new Vec(0, 0, 4));
            positions.put("LeftLeg", new Vec(0, 0, 4));
        }

        if (state.rightArmPose() != VanillaPlayerAnimationState.ArmPose.SPYGLASS) bob(rightArm, state.ageInTicks(), 1);
        if (state.leftArmPose() != VanillaPlayerAnimationState.ArmPose.SPYGLASS) bob(leftArm, state.ageInTicks(), -1);

        Map<String, Point> result = new HashMap<>();
        result.put("Head", head.degrees());
        result.put("Body", body.degrees());
        result.put("RightArm", rightArm.degrees());
        result.put("LeftArm", leftArm.degrees());
        result.put("RightLeg", rightLeg.degrees());
        result.put("LeftLeg", leftLeg.degrees());
        Map<String, Point> translations = new HashMap<>();
        // ModelPart pivot offsets are pixels in vanilla's render space. Convert them to
        // the emote bone's pre-display coordinate system and include the player's 15/16
        // render scale.
        positions.forEach((bone, value) -> translations.put(bone,
                new Vec(-value.x(), -value.y(), value.z()).mul(15.0 / 64.0)));
        if (state.crouching()) {
            // AvatarRenderer#getRenderOffset lowers the entire crouching player by
            // 2/16 block outside the scaled PlayerModel pose stack. In emote model
            // coordinates (divided by four when positioned), that is -0.5 on Y.
            translations.replaceAll((bone, value) -> value.add(0, -0.5, 0));
        }
        return new Pose(result, translations);
    }

    private static void poseArm(VanillaPlayerAnimationState.ArmPose pose, Part arm, Part other, Part head, boolean right) {
        switch (pose) {
            case EMPTY -> arm.y = 0;
            case ITEM -> { arm.x = arm.x * 0.5F - PI / 10; arm.y = 0; }
            case BLOCK -> {
                arm.x = arm.x * 0.5F - 0.9424779F + clamp(head.x, -PI * 4 / 9, 0.43633232F);
                arm.y = (right ? -PI / 6 : PI / 6) + clamp(head.y, -PI / 6, PI / 6);
            }
            case BOW_AND_ARROW -> {
                arm.y = (right ? -0.1F : 0.1F) + head.y;
                other.y = (right ? 0.5F : -0.5F) + head.y;
                arm.x = other.x = -PI / 2 + head.x;
            }
            case THROW_TRIDENT -> { arm.x = arm.x * 0.5F - PI; arm.y = 0; }
            case CROSSBOW_HOLD -> {
                arm.y = (right ? -0.3F : 0.6F) + head.y;
                other.y = (right ? 0.6F : -0.3F) + head.y;
                arm.x = -PI / 2 + head.x + 0.1F;
                other.x = -1.5F + head.x;
            }
            case SPYGLASS -> {
                arm.x = clamp(head.x - 1.9198622F, -2.4F, 3.3F);
                arm.y = head.y + (right ? -PI / 12 : PI / 12);
            }
            case TOOT_HORN -> {
                arm.x = clamp(head.x, -1.2F, 1.2F) - 1.4835298F;
                arm.y = head.y + (right ? -PI / 6 : PI / 6);
            }
            case BRUSH -> { arm.x = arm.x * 0.5F - PI / 5; arm.y = 0; }
            case CROSSBOW_CHARGE, SPEAR -> { /* duration-dependent inputs are applied by the controller */ }
        }
    }

    private static boolean isTwoHanded(VanillaPlayerAnimationState.ArmPose pose) {
        return pose == VanillaPlayerAnimationState.ArmPose.BOW_AND_ARROW
                || pose == VanillaPlayerAnimationState.ArmPose.CROSSBOW_CHARGE
                || pose == VanillaPlayerAnimationState.ArmPose.CROSSBOW_HOLD;
    }

    private static void attack(float attackTime, VanillaPlayerAnimationState.AttackArm attackArm,
                               Part rightArm, Part leftArm, Part body, Part head,
                               Map<String, Point> positions) {
        if (attackTime <= 0) return;
        body.y = sin((float) Math.sqrt(attackTime) * PI * 2) * 0.2F;
        if (attackArm == VanillaPlayerAnimationState.AttackArm.LEFT) body.y *= -1;
        float armX = cos(body.y) * 5.0F;
        float armZ = sin(body.y) * 5.0F;
        positions.put("RightArm", new Vec(5.0F - armX, 0, armZ));
        positions.put("LeftArm", new Vec(armX - 5.0F, 0, -armZ));
        rightArm.y += body.y;
        leftArm.y += body.y;
        leftArm.x += body.y;
        float swing = 1 - (float) Math.pow(1 - attackTime, 4);
        float aa = sin(swing * PI);
        float bb = sin(attackTime * PI) * -(head.x - 0.7F) * 0.75F;
        Part activeArm = attackArm == VanillaPlayerAnimationState.AttackArm.RIGHT ? rightArm : leftArm;
        activeArm.x -= aa * 1.2F + bb;
        activeArm.y += body.y * 2;
        activeArm.z += sin(attackTime * PI) * -0.4F;
    }

    private static Map<String, Point> basePositions() {
        Map<String, Point> result = new HashMap<>();
        result.put("Head", Vec.ZERO);
        result.put("Body", Vec.ZERO);
        result.put("RightArm", Vec.ZERO);
        result.put("LeftArm", Vec.ZERO);
        result.put("RightLeg", Vec.ZERO);
        result.put("LeftLeg", Vec.ZERO);
        return result;
    }

    private static void bob(Part arm, float age, float direction) {
        arm.z += direction * (cos(age * 0.09F) * 0.05F + 0.05F);
        arm.x += direction * sin(age * 0.067F) * 0.05F;
    }

    private static float rad(float degrees) { return degrees * PI / 180; }
    private static float sin(float value) { return (float) Math.sin(value); }
    private static float cos(float value) { return (float) Math.cos(value); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    private static final class Part {
        float x, y, z;
        Part() {}
        Part(float x, float y, float z) { set(x, y, z); }
        void set(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
        Point degrees() { return new Vec(Math.toDegrees(x), Math.toDegrees(y), Math.toDegrees(z)); }
    }
}
