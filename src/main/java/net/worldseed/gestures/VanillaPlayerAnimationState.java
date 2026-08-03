package net.worldseed.gestures;

/** Mutable inputs for Minecraft 26.2's third-person humanoid pose equations. */
public final class VanillaPlayerAnimationState {
    public enum AttackArm { RIGHT, LEFT }
    public enum ArmPose {
        EMPTY, ITEM, BLOCK, BOW_AND_ARROW, THROW_TRIDENT, CROSSBOW_CHARGE,
        CROSSBOW_HOLD, SPYGLASS, TOOT_HORN, BRUSH, SPEAR
    }

    private float ageInTicks;
    private float walkAnimationPos;
    private float walkAnimationSpeed;
    private float attackTime;
    private AttackArm attackArm = AttackArm.RIGHT;
    private float headYaw;
    private float headPitch;
    private boolean crouching;
    private boolean passenger;
    private ArmPose rightArmPose = ArmPose.EMPTY;
    private ArmPose leftArmPose = ArmPose.EMPTY;

    public float ageInTicks() { return ageInTicks; }
    public void setAgeInTicks(float value) { ageInTicks = value; }
    public float walkAnimationPos() { return walkAnimationPos; }
    public void setWalkAnimationPos(float value) { walkAnimationPos = value; }
    public float walkAnimationSpeed() { return walkAnimationSpeed; }
    public void setWalkAnimationSpeed(float value) { walkAnimationSpeed = value; }
    public float attackTime() { return attackTime; }
    public void setAttackTime(float value) { attackTime = value; }
    public AttackArm attackArm() { return attackArm; }
    public void setAttackArm(AttackArm value) { attackArm = value; }
    public float headYaw() { return headYaw; }
    public void setHeadYaw(float value) { headYaw = value; }
    public float headPitch() { return headPitch; }
    public void setHeadPitch(float value) { headPitch = value; }
    public boolean crouching() { return crouching; }
    public void setCrouching(boolean value) { crouching = value; }
    public boolean passenger() { return passenger; }
    public void setPassenger(boolean value) { passenger = value; }
    public ArmPose rightArmPose() { return rightArmPose; }
    public void setRightArmPose(ArmPose value) { rightArmPose = value; }
    public ArmPose leftArmPose() { return leftArmPose; }
    public void setLeftArmPose(ArmPose value) { leftArmPose = value; }
}
