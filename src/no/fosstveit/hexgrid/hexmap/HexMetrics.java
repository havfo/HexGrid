package no.fosstveit.hexgrid.hexmap;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public class HexMetrics {

    public static final float outerRadius = 10f;
    public static final float innerRadius = outerRadius * 0.866025404f;
    public static final boolean subdivideInnerHex = true;
    public static final float innerHexSize = 1f / 2f;
    public static final float hexOutlineWidth = 1f / 5f;
    public static final float mountainHeight = 3f;

    public static float solidFactor = 0.7f;
    public static float blendFactor = 1f - solidFactor;

    public static float elevationStep = 2f;
    public static int terracesPerSlope = 2;
    public static boolean createTerraces = false;
    public static int terraceSteps = terracesPerSlope * 2 + 1;
    public static float horizontalTerraceStepSize = 1f / terraceSteps;
    public static float verticalTerraceStepSize = 1f / (terracesPerSlope + 1);

    public static float noiseScale = 1f;
    public static float noiseAmplitude = 0.5f;

    public static boolean createBridges = true;

    public static int chunkSizeX = 5;
    public static int chunkSizeZ = 5;

    public static Vector3f[] corners = {
        new Vector3f(0f, 0f, outerRadius),
        new Vector3f(innerRadius, 0f, 0.5f * outerRadius),
        new Vector3f(innerRadius, 0f, -0.5f * outerRadius),
        new Vector3f(0f, 0f, -outerRadius),
        new Vector3f(-innerRadius, 0f, -0.5f * outerRadius),
        new Vector3f(-innerRadius, 0f, 0.5f * outerRadius),
        new Vector3f(0f, 0f, outerRadius)
    };

    public static Vector3f getFirstCorner(HexDirection direction) {
        return corners[direction.ordinal()];
    }

    public static Vector3f getSecondCorner(HexDirection direction) {
        return corners[direction.ordinal() + 1];
    }

    public static Vector3f getFirstSolidCorner(HexDirection direction) {
        return (createBridges) ? corners[direction.ordinal()].mult(solidFactor) : corners[direction.ordinal()];
    }

    public static Vector3f getSecondSolidCorner(HexDirection direction) {
        return (createBridges) ? corners[direction.ordinal() + 1].mult(solidFactor) : corners[direction.ordinal() + 1];
    }

    public static Vector3f getBridge(HexDirection direction) {
        return (createBridges) ? (corners[direction.ordinal()].add(corners[direction.ordinal() + 1])).mult(blendFactor) : (corners[direction.ordinal()].add(corners[direction.ordinal() + 1])).mult(0f);
    }

    public static Vector3f terraceLerp(Vector3f a, Vector3f b, int step) {
        Vector3f ret = a.clone();
        float h = step * HexMetrics.horizontalTerraceStepSize;
        ret.x += (b.x - a.x) * h;
        ret.z += (b.z - a.z) * h;
        float v = ((step + 1) / 2) * HexMetrics.verticalTerraceStepSize;
        ret.y += (b.y - a.y) * v;
        return ret;
    }

    public static ColorRGBA terraceLerp(ColorRGBA a, ColorRGBA b, int step) {
        float h = step * HexMetrics.horizontalTerraceStepSize;
        return new ColorRGBA().interpolateLocal(a, b, h);
    }

    public static HexEdgeType getEdgeType(int elevation1, int elevation2) {
        if (elevation1 == elevation2) {
            return HexEdgeType.FLAT;
        }
        int delta = elevation2 - elevation1;
        if (delta == 1 || delta == -1) {
            return HexEdgeType.SLOPE;
        }
        return HexEdgeType.CLIFF;
    }
}
