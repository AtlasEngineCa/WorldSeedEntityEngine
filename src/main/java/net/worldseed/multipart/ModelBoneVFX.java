package net.worldseed.multipart;

import net.minestom.server.tag.Tag;
import net.worldseed.multipart.animations.ModelAnimation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;

non-sealed class ModelBoneVFX extends ModelBoneGeneric {
    private Point position = Pos.ZERO;

    public Point getPosition() {
        return position;
    }
    public ModelBoneVFX(Point pivot, String name, Point rotation, GenericModel model) {
        super(pivot, name, rotation, model);
        this.stand = null;
    }

    @Override
    public void setState(String state) {}

    public void spawn(Instance instance, Point position) {
    }

    public void draw(short tick) {
        this.children.forEach(bone -> bone.draw(tick));
        if (this.offset == null) return;

        Point p = this.offset;
        p = applyTransform(p, tick);
        p = applyGlobalRotation(p);

        Pos endPos = Pos.fromPoint(p);

        double divisor = 1;
        if (model.getRenderType() == ModelEngine.RenderType.SMALL_ARMOUR_STAND || model.getRenderType() == ModelEngine.RenderType.SMALL_ZOMBIE) {
            divisor = 1.426;
        }

        this.position = endPos
                .div(6.4, 6.4, 6.4)
                .div(divisor)
                .add(model.getPosition());
    }

    @Override
    public void destroy() {
        allAnimations.forEach(ModelAnimation::destroy);
    }
}
