package net.worldseed.multipart;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

final public class Quaternion {
    private final double x;
    private final double y;
    private final double z;

    private final double w;

    public Quaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Quaternion(Point p) {
        p = ModelMath.toRadians(p);

        double cy = Math.cos(p.z() * 0.5);
        double sy = Math.sin(p.z() * 0.5);
        double cp = Math.cos(p.y() * 0.5);
        double sp = Math.sin(p.y() * 0.5);
        double cr = Math.cos(p.x() * 0.5);
        double sr = Math.sin(p.x() * 0.5);

        w = cr * cp * cy + sr * sp * sy;
        x = sr * cp * cy - cr * sp * sy;
        y = cr * sp * cy + sr * cp * sy;
        z = cr * cp * sy - sr * sp * cy;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public double w() {
        return w;
    }

    public Point toEuler() {
        double t0 = (x + z) * (x - z);        // x^2-z^2
        double t1 = (w + y) * (w - y);        // w^2-y^2
        double xx = 0.5 * (t0 + t1);        // 1/2 x of x'
        double xy = x * y + w * z;            // 1/2 y of x'
        double xz = w * y - x * z;            // 1/2 z of x'
        double t = xx * xx + xy * xy;        // cos(theta)^2
        double yz = 2.0 * (y * z + w * x);      // z of y'

        double vx, vy, vz;

        vz = (float) Math.atan2(xy, xx);    // yaw   (psi)
        vy = (float) Math.atan(xz / Math.sqrt(t)); // pitch (theta)

        if (t != 0)
            vx = (float) Math.atan2(yz, t1 - t0);
        else
            vx = (float) (2.0 * Math.atan2(x, w) - Math.signum(xz) * vz);

        return ModelMath.toDegrees(new Vec(vx, vy, vz));
    }

    public Quaternion multiply(Quaternion q) {
        double w = this.w * q.w - this.x * q.x - this.y * q.y - this.z * q.z;
        double x = this.w * q.x + this.x * q.w + this.y * q.z - this.z * q.y;
        double y = this.w * q.y - this.x * q.z + this.y * q.w + this.z * q.x;
        double z = this.w * q.z + this.x * q.y - this.y * q.x + this.z * q.w;

        return new Quaternion(
                x, y, z, w
        );
    }

    @Override
    public String toString() {
        return "net.worldseed.multipart.Quaternion{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", w=" + w +
                '}';
    }

    Point threeAxisRot(double r11, double r12, double r21, double r31, double r32) {
        double x = Math.atan2(r31, r32);
        double y = Math.asin(r21);
        double z = Math.atan2(r11, r12);
        return new Vec(x, z, y);
    }

    public Point toEulerYZX() {
        Quaternion q = this;
        return ModelMath.toDegrees(threeAxisRot(-2 * (q.x * q.z - q.w * q.y),
                q.w * q.w + q.x * q.x - q.y * q.y - q.z * q.z,
                2 * (q.x * q.y + q.w * q.z),
                -2 * (q.y * q.z - q.w * q.x),
                q.w * q.w - q.x * q.x + q.y * q.y - q.z * q.z));
    }
}