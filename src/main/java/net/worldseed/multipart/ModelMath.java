package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

class Matrix3 {
    double[] x, y, z;

    Matrix3(double x1, double x2, double x3, double y1, double y2, double y3, double z1, double z2, double z3) {
        this.x = new double[]{x1, x2, x3};
        this.y = new double[]{y1, y2, y3};
        this.z = new double[]{z1, z2, z3};
    }

    Matrix3 mul(Matrix3 matrix3) {
        double x1 = x[0] * matrix3.x[0] + x[1] * matrix3.y[0] + x[2] * matrix3.z[0];
        double x2 = x[0] * matrix3.x[1] + x[1] * matrix3.y[1] + x[2] * matrix3.z[1];
        double x3 = x[0] * matrix3.x[2] + x[1] * matrix3.y[2] + x[2] * matrix3.z[2];

        double y1 = y[0] * matrix3.x[0] + y[1] * matrix3.y[0] + y[2] * matrix3.z[0];
        double y2 = y[0] * matrix3.x[1] + y[1] * matrix3.y[1] + y[2] * matrix3.z[1];
        double y3 = y[0] * matrix3.x[2] + y[1] * matrix3.y[2] + y[2] * matrix3.z[2];

        double z1 = z[0] * matrix3.x[0] + z[1] * matrix3.y[0] + z[2] * matrix3.z[0];
        double z2 = z[0] * matrix3.x[1] + z[1] * matrix3.y[1] + z[2] * matrix3.z[1];
        double z3 = z[0] * matrix3.x[2] + z[1] * matrix3.y[2] + z[2] * matrix3.z[2];

        return new Matrix3(x1, x2, x3, y1, y2, y3, z1, z2, z3);
    }

    Point mul(Point vec) {
        double vx = vec.x() * x[0] + vec.y() * x[1] + vec.z() * x[2];
        double vy = vec.x() * y[0] + vec.y() * y[1] + vec.z() * y[2];
        double vz = vec.x() * z[0] + vec.y() * z[1] + vec.z() * z[2];

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
