package no.fosstveit.hexgrid.hexmap;

import com.jme3.math.Vector3f;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public class EdgeVertices {

    private Vector3f v1, v2, v3, v4;

    public EdgeVertices(Vector3f corner1, Vector3f corner2) {
        v1 = corner1;
        v2 = new Vector3f().interpolateLocal(corner1, corner2, 1f / 3f);
        v3 = new Vector3f().interpolateLocal(corner1, corner2, 2f / 3f);
        v4 = corner2;
    }
    
    public EdgeVertices() {
    }

    public Vector3f getV1() {
        return v1;
    }

    public void setV1(Vector3f v1) {
        this.v1 = v1;
    }

    public Vector3f getV2() {
        return v2;
    }

    public void setV2(Vector3f v2) {
        this.v2 = v2;
    }

    public Vector3f getV3() {
        return v3;
    }

    public void setV3(Vector3f v3) {
        this.v3 = v3;
    }

    public Vector3f getV4() {
        return v4;
    }

    public void setV4(Vector3f v4) {
        this.v4 = v4;
    }

    public static EdgeVertices terraceLerp(EdgeVertices a, EdgeVertices b, int step) {
        EdgeVertices result = new EdgeVertices();
        result.setV1(HexMetrics.terraceLerp(a.v1, b.v1, step));
        result.setV2(HexMetrics.terraceLerp(a.v2, b.v2, step));
        result.setV3(HexMetrics.terraceLerp(a.v3, b.v3, step));
        result.setV4(HexMetrics.terraceLerp(a.v4, b.v4, step));
        return result;
    }
}
