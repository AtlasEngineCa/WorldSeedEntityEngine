package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;

public interface GenericModel {
    String getId();
    Point getPivot();
    double getGlobalRotation();
    Point getGlobalOffset();
    Point getPosition();
    void setPosition(Point pos);
    void setGlobalRotation(double rotation);
    void setState(String state);
    ModelBone getPart(String boneName);
    void drawBones(short tick);
    void destroy();
    Point getVFX(String name);
}
