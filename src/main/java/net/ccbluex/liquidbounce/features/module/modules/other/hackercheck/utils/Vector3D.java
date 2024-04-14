package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

public class Vector3D {

    public final double x;
    public final double y;
    public final double z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Vector3D getVectToEntity(Entity from, Entity to) {
        return new Vector3D(to.posX - from.posX, to.posY - from.posY, to.posZ - from.posZ);
    }

    public static Vector3D getPlayersEyePos(EntityPlayer player) {
        return new Vector3D(player.posX, player.posY + player.getEyeHeight(), player.posZ);
    }

    public static Vector3D getPlayersLookVec(EntityPlayer player) {
        return getVectorFromRotation(player.rotationPitch, player.rotationYawHead);
    }

    /**
     * Creates a normalized Vector3D from pitch and yaw
     */
    public static Vector3D getVectorFromRotation(float pitch, float yaw) {
        final float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        final float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        final float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        final float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vector3D(f1 * f2, f3, f * f2);
    }

    /**
     * Returns true is the coordinates x, y, z are within the AxisAlignedBB provided
     */
    public boolean isVectInside(AxisAlignedBB bb) {
        return (this.x > bb.minX && this.x < bb.maxX) && (this.y > bb.minY && this.y < bb.maxY) && (this.z > bb.minZ && this.z < bb.maxZ);
    }

    public Vector3D addVector(double x1, double y1, double z1) {
        return new Vector3D(this.x + x1, this.y + y1, this.z + z1);
    }

    /**
     * Returns the norm of the vector.
     */
    public double norm() {
        return MathHelper.sqrt_double(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public double normSquared() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public double dotProduct(Vector3D otherVector) {
        return this.x * otherVector.x + this.y * otherVector.y + this.z * otherVector.z;
    }

    /**
     * Returns the absolute angle in between
     * the 2Dvector resulting of the projection of this onto the XZ plane and
     * the 2Dvector resulting of the projection of otherVector onto the XZ plane
     * Result is in degrees in between 0 and 180
     */
    public double getXZAngleDiffWithVector(Vector3D otherVector) {
        final double den = Math.sqrt((this.x * this.x + this.z * this.z) * (otherVector.x * otherVector.x + otherVector.z * otherVector.z));
        if (den < 1.0000000116860974E-7D) {
            return 0D;
        }
        final double cos = (this.x * otherVector.x + this.z * otherVector.z) / den;
        if (cos > 1) {
            return 0D;
        } else if (cos < -1) {
            return 180D;
        }
        return Math.toDegrees(Math.acos(cos));
    }

    /**
     * Returns the 2D vector resulting of the projection of this onto the XZ plane
     */
    public Vector2D getProjectionInXZPlane() {
        return new Vector2D(this.x, this.z);
    }

    /**
     * Returns the length of the 2D vector resulting of the projection of this onto the XZ plane
     */
    public double normInXZPlane() {
        return this.getProjectionInXZPlane().norm();
    }

    /**
     * Returns the absolute angle in between this and otherVector
     * in the plane formed by those two vectors
     * Result is in degres
     */
    public double getAngleWithVector(Vector3D otherVector) {
        final double den = Math.sqrt((this.x * this.x + this.y * this.y + this.z * this.z) *
                (otherVector.x * otherVector.x + otherVector.y * otherVector.y + otherVector.z * otherVector.z));
        if (den < 1.0000000116860974E-7D) {
            return 0D;
        }
        final double cos = this.dotProduct(otherVector) / den;
        if (cos > 1D) {
            return 0D;
        } else if (cos < -1D) {
            return 180D;
        }
        return Math.toDegrees(Math.acos(cos));
    }

    /**
     * Returns a new Vector3D that is the result of this * d
     */
    public Vector3D mulitply(double d) {
        return new Vector3D(this.x * d, this.y * d, this.z * d);
    }

    @Override
    public String toString() {
        return "{" + String.format("%.4f", this.x) +
                ", " + String.format("%.4f", this.y) +
                ", " + String.format("%.4f", this.z) +
                '}';
    }

}
