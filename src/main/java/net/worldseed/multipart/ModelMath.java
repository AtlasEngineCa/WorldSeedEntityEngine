package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

class Matrix3 {
    double x1, x2, x3;
    double y1, y2, y3;
    double z1, z2, z3;

    Matrix3(double x1, double x2, double x3, double y1, double y2, double y3, double z1, double z2, double z3) {
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
        this.y1 = y1;
        this.y2 = y2;
        this.y3 = y3;
        this.z1 = z1;
        this.z2 = z2;
        this.z3 = z3;
    }

    Matrix3 mul(Matrix3 matrix3) {
        double x1 = this.x1 * matrix3.x1 + this.x2 * matrix3.y1 + this.x3 * matrix3.z1;
        double x2 = this.x1 * matrix3.x2 + this.x2 * matrix3.y2 + this.x3 * matrix3.z2;
        double x3 = this.x1 * matrix3.x3 + this.x2 * matrix3.y3 + this.x3 * matrix3.z3;

        double y1 = this.y1 * matrix3.x1 + this.y2 * matrix3.y1 + this.y3 * matrix3.z1;
        double y2 = this.y1 * matrix3.x2 + this.y2 * matrix3.y2 + this.y3 * matrix3.z2;
        double y3 = this.y1 * matrix3.x3 + this.y2 * matrix3.y3 + this.y3 * matrix3.z3;

        double z1 = this.z1 * matrix3.x1 + this.z2 * matrix3.y1 + this.z3 * matrix3.z1;
        double z2 = this.z1 * matrix3.x2 + this.z2 * matrix3.y2 + this.z3 * matrix3.z2;
        double z3 = this.z1 * matrix3.x3 + this.z2 * matrix3.y3 + this.z3 * matrix3.z3;

        return new Matrix3(x1, x2, x3, y1, y2, y3, z1, z2, z3);
    }

    Point mul(Point vec) {
        double vx = vec.x() * x1 + vec.y() * x2 + vec.z() * x3;
        double vy = vec.x() * y1 + vec.y() * y2 + vec.z() * y3;
        double vz = vec.x() * z1 + vec.y() * z2 + vec.z() * z3;

        return new Vec(vx, vy, vz);
    }
}

class ModelMath {
    private static final float DEGREE = 0.017453292519943295F;
    private static final float RADIAN = 57.29577951308232F;

    static Point toRadians(Point vector) {
        return vector.mul(DEGREE);
    }
    static Point toDegrees(Point vector) {
        return vector.mul(RADIAN);
    }

    static Point rotate(Point vector, Point rotation) {
        Point rot = toRadians(rotation);

        double rotX = rot.x();
        double rotY = rot.y();
        double rotZ = rot.z();

        Matrix3 rotMatrixX = new Matrix3(1, 0, 0, 0, Math.cos(rotX), -Math.sin(rotX), 0, Math.sin(rotX), Math.cos(rotX));
        Matrix3 rotMatrixY = new Matrix3(Math.cos(rotY), 0, Math.sin(rotY), 0, 1, 0, -Math.sin(rotY), 0, Math.cos(rotY));
        Matrix3 rotMatrixZ = new Matrix3(Math.cos(rotZ), -Math.sin(rotZ), 0, Math.sin(rotZ), Math.cos(rotZ), 0, 0, 0, 1);

        return rotMatrixZ.mul(rotMatrixY).mul(rotMatrixX).mul(vector);
    }
}
