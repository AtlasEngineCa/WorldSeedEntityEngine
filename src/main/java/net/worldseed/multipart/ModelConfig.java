package net.worldseed.multipart;

public class ModelConfig {
    public enum ModelType {
        /**
         * X, Y, Z rotation
         */
        ARMOUR_STAND,
        /**
         * X, Y rotation. No Z
         */
        ZOMBIE
    }

    public enum InterpolationType {
        /**
         * Uses entity yaw for rotation, not head. This interpolates Y rotations
         */
        Y_INTERPOLATION,
        /**
         * Only sends position update every other tick
         */
        POSITION_INTERPOLATION,
        /**
         * Not implemented
         */
        NONE
    }

    public enum Size {
        /**
         * Uses baby zombies or small armour stands
         */
        SMALL,
        /**
         * Uses normal zombies or armour stands
         */
        NORMAL
    }

    public enum ItemSlot {
        /**
         * Put render item in the head slot
         */
        HEAD,
        /**
         * Put render item in the hand slot. This results in the item being around 60% bigger
         */
        HAND
    }

    private final ModelType modelType;
    private final InterpolationType interpolationType;
    private final Size size;
    private final ItemSlot itemSlot;

    public ModelConfig(ModelType modelType, InterpolationType interpolationType, Size size, ItemSlot itemSlot) {
        this.modelType = modelType;
        this.interpolationType = interpolationType;
        this.size = size;
        this.itemSlot = itemSlot;
    }

    public ModelType modelType() {
        return modelType;
    }

    public InterpolationType interpolationType() {
        return interpolationType;
    }

    public Size size() {
        return size;
    }

    public ItemSlot itemSlot() {
        return itemSlot;
    }

    public static ModelConfig defaultConfig =
            new ModelConfig(ModelType.ARMOUR_STAND,
                    InterpolationType.POSITION_INTERPOLATION,
                    Size.NORMAL,
                    ItemSlot.HEAD
            );
}
