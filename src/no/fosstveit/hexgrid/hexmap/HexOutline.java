package no.fosstveit.hexgrid.hexmap;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public class HexOutline extends Mesh {

    List<Vector3f> vertices;
    List<Integer> indices;

    private HexCell cell;

    public HexOutline() {
        vertices = new ArrayList<>();
        indices = new ArrayList<>();
    }
    
    public void setCell(HexCell cell) {
        this.cell = cell;
    }

    public void triangulate() {
        vertices.clear();
        indices.clear();

        for (HexDirection direction : HexDirection.values()) {
            triangulate(direction, cell);
        }
        
        setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices.toArray(new Vector3f[vertices.size()])));
        setBuffer(Type.Index, 1, BufferUtils.createIntBuffer(indices.stream().mapToInt(i -> i).toArray()));
        
        updateBound();
        
        vertices.clear();
        indices.clear();
    }

    private void triangulate(HexDirection direction, HexCell cell) {
        Vector3f center = cell.getPosition().clone();
        center.addLocal(0f, 0.01f, 0f);
        Vector3f v1 = center.add(HexMetrics.getFirstSolidCorner(direction));
        Vector3f v2 = center.add(HexMetrics.getSecondSolidCorner(direction));

        Vector3f iv1 = new Vector3f().interpolateLocal(v1, center, HexMetrics.hexOutlineWidth);
        Vector3f iv2 = new Vector3f().interpolateLocal(v2, center, HexMetrics.hexOutlineWidth);

        EdgeVertices edgeInner = new EdgeVertices(iv1, iv2);

        EdgeVertices edgeOuter = new EdgeVertices(v1, v2);

        triangulateEdgeStrip(edgeInner, edgeOuter);
    }

    private void triangulateEdgeStrip(EdgeVertices e1, EdgeVertices e2) {
        addQuad(e1.getV1(), e1.getV2(), e2.getV1(), e2.getV2());
        addQuad(e1.getV2(), e1.getV3(), e2.getV2(), e2.getV3());
        addQuad(e1.getV3(), e1.getV4(), e2.getV3(), e2.getV4());
    }

    private void addQuad(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4) {
        int vertexIndex = vertices.size();
        vertices.add(v1);
        vertices.add(v2);
        vertices.add(v3);
        vertices.add(v4);
        indices.add(vertexIndex);
        indices.add(vertexIndex + 2);
        indices.add(vertexIndex + 1);
        indices.add(vertexIndex + 1);
        indices.add(vertexIndex + 2);
        indices.add(vertexIndex + 3);
    }
}
