package no.fosstveit.hexgrid.hexmap;

import com.jme3.math.Vector3f;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public class HexVertex {
    private Vector3f vertex;
    private int index;
    
    public HexVertex(Vector3f vertex, int index) {
        this.vertex = vertex;
        this.index = index;
    }

    public Vector3f getVertex() {
        return vertex;
    }

    public void setVertex(Vector3f vertex) {
        this.vertex = vertex;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
