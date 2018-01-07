package no.fosstveit.hexgrid.hexmap;

import com.jme3.math.Vector3f;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public class HexCoordinates {

    private int x;
    private int z;

    public HexCoordinates(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int getY() {
        return (-x - z);
    }

    public static HexCoordinates fromOffsetCoordinates(int x, int z) {
        return new HexCoordinates(x - z / 2, z);
    }

    public static HexCoordinates fromPosition(Vector3f position) {
        float x = position.x / (HexMetrics.innerRadius * 2f);
        float y = -x;

        float offset = position.z / (HexMetrics.outerRadius * 3f);
        x -= offset;
        y -= offset;

        int iX = roundEven(x);
        int iY = roundEven(y);
        int iZ = roundEven(-x - y);

        if (iX + iY + iZ != 0) {
            float dX = Math.abs(x - iX);
            float dY = Math.abs(y - iY);
            float dZ = Math.abs(-x - y - iZ);

            if (dX > dY && dX > dZ) {
                iX = -iY - iZ;
            } else if (dZ > dY) {
                iZ = -iX - iY;
            }
        }

        return new HexCoordinates(iX, iZ);
    }

    public int distanceTo(HexCoordinates other) {
        return ((getX() < other.getX() ? other.getX() - getX() : getX() - other.getX())
                + (getY() < other.getY() ? other.getY() - getY() : getY() - other.getY())
                + (getZ() < other.getZ() ? other.getZ() - getZ() : getZ() - other.getZ())) / 2;
    }
    
    private static int roundEven(double d) {
        if (d % 2 == 0.5) {
            return (int) Math.floor(d);
        } else {
            return (int) Math.round(d);
        }
    }
}
