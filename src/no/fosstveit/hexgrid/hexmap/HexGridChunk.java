package no.fosstveit.hexgrid.hexmap;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import no.fosstveit.hexgrid.utils.OpenSimplexNoise;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public class HexGridChunk extends Mesh {

    private final HexMap parent;
    private final OpenSimplexNoise noise;
    private final Random rand = new Random(System.currentTimeMillis());

    List<Vector3f> vertices;
    List<Integer> indices;
    List<ColorRGBA> colors;
    List<Vector3f> terrainTypes;
    List<Vector3f> normals;
    List<Vector2f> textures;

    private HexCell[] cells;

    private final ColorRGBA color1 = new ColorRGBA(1f, 0f, 0f, 1f);
    private final ColorRGBA color2 = new ColorRGBA(0f, 1f, 0f, 1f);
    private final ColorRGBA color3 = new ColorRGBA(0f, 0f, 1f, 1f);

    public HexGridChunk(HexMap parent, OpenSimplexNoise noise) {
        this.parent = parent;
        this.noise = noise;

        vertices = new ArrayList<>();
        indices = new ArrayList<>();
        colors = new ArrayList<>();
        terrainTypes = new ArrayList<>();
        normals = new ArrayList<>();
        textures = new ArrayList<>();

        cells = new HexCell[HexMetrics.chunkSizeX * HexMetrics.chunkSizeZ];
    }

    public void addCell(int index, HexCell cell) {
        cells[index] = cell;
        cell.setChunk(this);
    }

    public void triangulateCells() {
        triangulate(cells);

        setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices.toArray(new Vector3f[vertices.size()])));
        setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(colors.toArray(new ColorRGBA[colors.size()])));
        setBuffer(Type.Index, 1, BufferUtils.createIntBuffer(indices.stream().mapToInt(i -> i).toArray()));
        setBuffer(Type.TexCoord2, 3, BufferUtils.createFloatBuffer(terrainTypes.toArray(new Vector3f[terrainTypes.size()])));
        setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals.toArray(new Vector3f[normals.size()])));
        // setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(textures.toArray(new Vector2f[textures.size()])));
        updateBound();

        vertices.clear();
        indices.clear();
        colors.clear();
        terrainTypes.clear();
        normals.clear();
        textures.clear();
    }

    private void triangulate(HexCell[] cells) {
        vertices.clear();
        indices.clear();
        colors.clear();
        terrainTypes.clear();
        normals.clear();
        textures.clear();

        for (HexCell cell : cells) {
            triangulate(cell);
        }
    }

    private void triangulate(HexCell cell) {
        for (HexDirection direction : HexDirection.values()) {
            triangulate(direction, cell);
        }
    }

    private void triangulate(HexDirection direction, HexCell cell) {
        Vector3f center = cell.getPosition();
        Vector3f v1 = center.add(HexMetrics.getFirstSolidCorner(direction));
        Vector3f v2 = center.add(HexMetrics.getSecondSolidCorner(direction));

        /* if (cell.getElevation() == 0 &&
                (cell.getNeighbor(direction) == null || cell.getNeighbor(direction).getElevation() == 0) &&
                (cell.getNeighbor(direction.next()) == null || cell.getNeighbor(direction.next()).getElevation() == 0) &&
                (cell.getNeighbor(direction.previous()) == null || cell.getNeighbor(direction.previous()).getElevation() == 0)) {
            addTriangle(center, center.add(HexMetrics.corners[direction.ordinal()]), center.add(HexMetrics.corners[direction.ordinal() + 1]));
            addTriangleColor(color1);
            Vector3f types = new Vector3f(cell.getTerrainTypeIndex(), cell.getTerrainTypeIndex(), cell.getTerrainTypeIndex());
            addTriangleTerrainTypes(types);
        } else */ if (HexMetrics.subdivideInnerHex && cell.getElevation() > 0) {
            Vector3f iv1 = new Vector3f().interpolateLocal(v1, center, HexMetrics.innerHexSize);
            Vector3f iv2 = new Vector3f().interpolateLocal(v2, center, HexMetrics.innerHexSize);

            EdgeVertices edgeInner = new EdgeVertices(iv1, iv2);
            // perturbCellHeight(center, edgeInner, cell);

            EdgeVertices edgeOuter = new EdgeVertices(v1, v2);

            triangulateEdgeFan(center, edgeInner, cell);
            triangulateEdgeStrip(edgeInner, color1, cell.getTerrainTypeIndex(), edgeOuter, color1, cell.getTerrainTypeIndex());

            if (direction.ordinal() <= HexDirection.SE.ordinal() && HexMetrics.createBridges) {
                triangulateConnection(direction, cell, edgeOuter);
            }
        } else {
            EdgeVertices edgeOuter = new EdgeVertices(v1, v2);

            triangulateEdgeFan(center, edgeOuter, cell);

            if (direction.ordinal() <= HexDirection.SE.ordinal() && HexMetrics.createBridges) {
                triangulateConnection(direction, cell, edgeOuter);
            }
        }
    }

    /* 
    private void perturbCellHeight(Vector3f center, EdgeVertices edge, HexCell cell) {
        if (cell.getElevation() == 3) { // Mountain
            
            Vector3f v1 = edge.getV1();
            Vector3f v2 = edge.getV2();
            Vector3f v3 = edge.getV3();
            Vector3f v4 = edge.getV4();
            
            float elevationStep = HexMetrics.mountainHeight / 2f;
            
            v1.y = v1.y + elevationStep + (rand.nextFloat() - 0.5f);
            v2.y = v2.y + elevationStep + (rand.nextFloat() - 0.5f);
            v3.y = v3.y + elevationStep + (rand.nextFloat() - 0.5f);
            v4.y = v4.y + elevationStep + (rand.nextFloat() - 0.5f);
        }
    } */
    
    private void triangulateEdgeFan(Vector3f center, EdgeVertices edge, HexCell cell) {
        addTriangle(center, edge.getV1(), edge.getV2());
        addTriangleColor(color1);
        addTriangle(center, edge.getV2(), edge.getV3());
        addTriangleColor(color1);
        addTriangle(center, edge.getV3(), edge.getV4());
        addTriangleColor(color1);

        Vector3f types = new Vector3f(cell.getTerrainTypeIndex(), cell.getTerrainTypeIndex(), cell.getTerrainTypeIndex());
        addTriangleTerrainTypes(types);
        addTriangleTerrainTypes(types);
        addTriangleTerrainTypes(types);
    }

    private void triangulateEdgeStrip(EdgeVertices e1, ColorRGBA c1, float type1, EdgeVertices e2, ColorRGBA c2, float type2) {
        addQuad(e1.getV1(), e1.getV2(), e2.getV1(), e2.getV2());
        addQuadColor(c1, c2);
        addQuad(e1.getV2(), e1.getV3(), e2.getV2(), e2.getV3());
        addQuadColor(c1, c2);
        addQuad(e1.getV3(), e1.getV4(), e2.getV3(), e2.getV4());
        addQuadColor(c1, c2);

        Vector3f types = new Vector3f(type1, type2, type1);
        addQuadTerrainTypes(types);
        addQuadTerrainTypes(types);
        addQuadTerrainTypes(types);
    }

    private void triangulateConnection(HexDirection direction, HexCell cell, EdgeVertices e1) {
        HexCell neighbor = cell.getNeighbor(direction);

        if (neighbor == null) {
            return;
        }

        Vector3f bridge = HexMetrics.getBridge(direction);
        bridge.setY(neighbor.getPosition().getY() - cell.getPosition().getY());
        EdgeVertices e2 = new EdgeVertices(e1.getV1().add(bridge), e1.getV4().add(bridge));

        if (cell.getEdgeType(direction) == HexEdgeType.SLOPE && HexMetrics.createTerraces) {
            triangulateEdgeTerraces(e1, cell, e2, neighbor);
        } else {
            triangulateEdgeStrip(e1, color1, cell.getTerrainTypeIndex(), e2, color2, neighbor.getTerrainTypeIndex());
        }

        // Corner triangles
        HexCell nextNeighbor = cell.getNeighbor(direction.next());
        if (direction.ordinal() <= HexDirection.E.ordinal() && nextNeighbor != null) {
            Vector3f v5 = e1.getV4().add(HexMetrics.getBridge(direction.next()));
            v5.y = nextNeighbor.getElevation() * HexMetrics.elevationStep;
            if (cell.getElevation() <= neighbor.getElevation()) {
                if (cell.getElevation() <= nextNeighbor.getElevation()) {
                    triangulateCorner(e1.getV4(), cell, e2.getV4(), neighbor, v5, nextNeighbor);
                } else {
                    triangulateCorner(v5, nextNeighbor, e1.getV4(), cell, e2.getV4(), neighbor);
                }
            } else if (neighbor.getElevation() <= nextNeighbor.getElevation()) {
                triangulateCorner(e2.getV4(), neighbor, v5, nextNeighbor, e1.getV4(), cell);
            } else {
                triangulateCorner(v5, nextNeighbor, e1.getV4(), cell, e2.getV4(), neighbor);
            }
        }
    }

    private void triangulateEdgeTerraces(EdgeVertices begin, HexCell beginCell, EdgeVertices end, HexCell endCell) {
        EdgeVertices e2 = EdgeVertices.terraceLerp(begin, end, 1);
        ColorRGBA c2 = HexMetrics.terraceLerp(color1, color2, 1);
        float t1 = beginCell.getTerrainTypeIndex();
        float t2 = endCell.getTerrainTypeIndex();

        triangulateEdgeStrip(begin, color1, t1, e2, c2, t2);

        for (int i = 2; i < HexMetrics.terraceSteps; i++) {
            EdgeVertices e1 = e2;
            ColorRGBA c1 = c2;
            e2 = EdgeVertices.terraceLerp(begin, end, i);
            c2 = HexMetrics.terraceLerp(color1, color2, i);
            triangulateEdgeStrip(e1, c1, t1, e2, c2, t2);
        }

        triangulateEdgeStrip(e2, c2, t1, end, color2, t2);
    }

    private void triangulateCorner(Vector3f bottom, HexCell bottomCell, Vector3f left, HexCell leftCell, Vector3f right, HexCell rightCell) {
        HexEdgeType leftEdgeType = bottomCell.getEdgeType(leftCell);
        HexEdgeType rightEdgeType = bottomCell.getEdgeType(rightCell);

        if (leftEdgeType == HexEdgeType.SLOPE && HexMetrics.createTerraces) {
            if (rightEdgeType == HexEdgeType.SLOPE) {
                triangulateCornerTerraces(bottom, bottomCell, left, leftCell, right, rightCell);
            } else if (rightEdgeType == HexEdgeType.FLAT) {
                triangulateCornerTerraces(left, leftCell, right, rightCell, bottom, bottomCell);
            } else {
                triangulateCornerTerracesCliff(bottom, bottomCell, left, leftCell, right, rightCell);
            }
        } else if (rightEdgeType == HexEdgeType.SLOPE && HexMetrics.createTerraces) {
            if (leftEdgeType == HexEdgeType.FLAT) {
                triangulateCornerTerraces(right, rightCell, bottom, bottomCell, left, leftCell);
            } else {
                triangulateCornerCliffTerraces(bottom, bottomCell, left, leftCell, right, rightCell);
            }
        } else if (leftCell.getEdgeType(rightCell) == HexEdgeType.SLOPE && HexMetrics.createTerraces) {
            if (leftCell.getElevation() < rightCell.getElevation()) {
                triangulateCornerCliffTerraces(right, rightCell, bottom, bottomCell, left, leftCell);
            } else {
                triangulateCornerTerracesCliff(left, leftCell, right, rightCell, bottom, bottomCell);
            }
        } else {
            addTriangle(bottom, left, right);
            addTriangleColor(color1, color2, color3);

            Vector3f types = new Vector3f(bottomCell.getTerrainTypeIndex(), leftCell.getTerrainTypeIndex(), rightCell.getTerrainTypeIndex());
            addTriangleTerrainTypes(types);
        }
    }

    private void triangulateCornerTerraces(Vector3f begin, HexCell beginCell, Vector3f left, HexCell leftCell, Vector3f right, HexCell rightCell) {
        Vector3f v3 = HexMetrics.terraceLerp(begin, left, 1);
        Vector3f v4 = HexMetrics.terraceLerp(begin, right, 1);
        ColorRGBA c3 = HexMetrics.terraceLerp(color1, color2, 1);
        ColorRGBA c4 = HexMetrics.terraceLerp(color1, color3, 1);
        Vector3f types = new Vector3f(beginCell.getTerrainTypeIndex(), leftCell.getTerrainTypeIndex(), rightCell.getTerrainTypeIndex());

        addTriangle(begin, v3, v4);
        addTriangleColor(color1, c3, c4);
        addTriangleTerrainTypes(types);

        for (int i = 2; i < HexMetrics.terraceSteps; i++) {
            Vector3f v1 = v3;
            Vector3f v2 = v4;
            ColorRGBA c1 = c3;
            ColorRGBA c2 = c4;
            v3 = HexMetrics.terraceLerp(begin, left, i);
            v4 = HexMetrics.terraceLerp(begin, right, i);
            c3 = HexMetrics.terraceLerp(color1, color2, i);
            c4 = HexMetrics.terraceLerp(color1, color3, i);
            addQuad(v1, v2, v3, v4);
            addQuadColor(c1, c2, c3, c4);
            addQuadTerrainTypes(types);
        }

        addQuad(v3, v4, left, right);
        addQuadColor(c3, c4, color2, color3);
        addQuadTerrainTypes(types);
    }

    private void triangulateCornerTerracesCliff(Vector3f begin, HexCell beginCell, Vector3f left, HexCell leftCell, Vector3f right, HexCell rightCell) {
        float b = 1f / (rightCell.getElevation() - beginCell.getElevation());

        if (b < 0) {
            b = -b;
        }

        Vector3f boundary = new Vector3f().interpolateLocal(perturb(begin), perturb(right), b);
        ColorRGBA boundaryColor = new ColorRGBA().interpolateLocal(color1, color3, b);
        Vector3f types = new Vector3f(beginCell.getTerrainTypeIndex(), leftCell.getTerrainTypeIndex(), rightCell.getTerrainTypeIndex());

        triangulateBoundaryTriangle(begin, color1, left, color2, boundary, boundaryColor, types);

        if (leftCell.getEdgeType(rightCell) == HexEdgeType.SLOPE) {
            triangulateBoundaryTriangle(left, color2, right, color3, boundary, boundaryColor, types);
        } else {
            addTriangleUnperturbed(perturb(left), perturb(right), boundary);
            addTriangleColor(color2, color3, boundaryColor);
            addTriangleTerrainTypes(types);
        }
    }

    private void triangulateCornerCliffTerraces(Vector3f begin, HexCell beginCell, Vector3f left, HexCell leftCell, Vector3f right, HexCell rightCell) {
        float b = 1f / (leftCell.getElevation() - beginCell.getElevation());

        if (b < 0) {
            b = -b;
        }

        Vector3f boundary = new Vector3f().interpolateLocal(perturb(begin), perturb(left), b);
        ColorRGBA boundaryColor = new ColorRGBA().interpolateLocal(color1, color2, b);
        Vector3f types = new Vector3f(beginCell.getTerrainTypeIndex(), leftCell.getTerrainTypeIndex(), rightCell.getTerrainTypeIndex());

        triangulateBoundaryTriangle(right, color3, begin, color1, boundary, boundaryColor, types);

        if (leftCell.getEdgeType(rightCell) == HexEdgeType.SLOPE) {
            triangulateBoundaryTriangle(left, color2, right, color3, boundary, boundaryColor, types);
        } else {
            addTriangleUnperturbed(perturb(left), perturb(right), boundary);
            addTriangleColor(color2, color3, boundaryColor);
            addTriangleTerrainTypes(types);
        }
    }

    private void triangulateBoundaryTriangle(Vector3f begin, ColorRGBA beginColor, Vector3f left, ColorRGBA leftColor, Vector3f boundary, ColorRGBA boundaryColor, Vector3f types) {
        Vector3f v2 = perturb(HexMetrics.terraceLerp(begin, left, 1));
        ColorRGBA c2 = HexMetrics.terraceLerp(beginColor, leftColor, 1);

        addTriangleUnperturbed(perturb(begin), v2, boundary);
        addTriangleColor(beginColor, c2, boundaryColor);
        addTriangleTerrainTypes(types);

        for (int i = 2; i < HexMetrics.terraceSteps; i++) {
            Vector3f v1 = v2;
            ColorRGBA c1 = c2;
            v2 = perturb(HexMetrics.terraceLerp(begin, left, i));
            c2 = HexMetrics.terraceLerp(beginColor, leftColor, i);
            addTriangleUnperturbed(v1, v2, boundary);
            addTriangleColor(c1, c2, boundaryColor);
            addTriangleTerrainTypes(types);
        }

        addTriangleUnperturbed(v2, perturb(left), boundary);
        addTriangleColor(c2, leftColor, boundaryColor);
        addTriangleTerrainTypes(types);
    }

    private void addTriangle(Vector3f v1, Vector3f v2, Vector3f v3) {
        int vertexIndex = vertices.size();
        vertices.add(perturb(v1));
        vertices.add(perturb(v2));
        vertices.add(perturb(v3));
        indices.add(vertexIndex);
        indices.add(vertexIndex + 1);
        indices.add(vertexIndex + 2);
        textures.add(new Vector2f(0, 0));
        textures.add(new Vector2f(0, 1));
        textures.add(new Vector2f(1, 0));
        normals.add(Vector3f.UNIT_Y);
        normals.add(Vector3f.UNIT_Y);
        normals.add(Vector3f.UNIT_Y);

        /* Triangle t = new Triangle(v1, v2, v3);
        t.calculateNormal();
        Vector3f n = t.getNormal();
        
        normals.add(n);
        normals.add(n);
        normals.add(n); */
    }

    private void addTriangleUnperturbed(Vector3f v1, Vector3f v2, Vector3f v3) {
        int vertexIndex = vertices.size();
        vertices.add(v1);
        vertices.add(v2);
        vertices.add(v3);
        indices.add(vertexIndex);
        indices.add(vertexIndex + 1);
        indices.add(vertexIndex + 2);
        textures.add(new Vector2f(0, 0));
        textures.add(new Vector2f(0, 1));
        textures.add(new Vector2f(1, 0));
        normals.add(Vector3f.UNIT_Y);
        normals.add(Vector3f.UNIT_Y);
        normals.add(Vector3f.UNIT_Y);

        /* Triangle t = new Triangle(v1, v2, v3);
        t.calculateNormal();
        Vector3f n = t.getNormal();
        
        normals.add(n);
        normals.add(n);
        normals.add(n); */
    }

    private void addTriangleColor(ColorRGBA color) {
        colors.add(color);
        colors.add(color);
        colors.add(color);
    }

    private void addTriangleColor(ColorRGBA c1, ColorRGBA c2, ColorRGBA c3) {
        colors.add(c1);
        colors.add(c2);
        colors.add(c3);
    }

    private void addTriangleTerrainTypes(Vector3f types) {
        terrainTypes.add(types);
        terrainTypes.add(types);
        terrainTypes.add(types);
    }

    private void addQuad(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4) {
        int vertexIndex = vertices.size();
        vertices.add(perturb(v1));
        vertices.add(perturb(v2));
        vertices.add(perturb(v3));
        vertices.add(perturb(v4));
        indices.add(vertexIndex);
        indices.add(vertexIndex + 2);
        indices.add(vertexIndex + 1);
        indices.add(vertexIndex + 1);
        indices.add(vertexIndex + 2);
        indices.add(vertexIndex + 3);
        textures.add(new Vector2f(1, 0));
        textures.add(new Vector2f(0, 0));
        textures.add(new Vector2f(0, 1));
        textures.add(new Vector2f(1, 1));

        normals.add(Vector3f.UNIT_Y);
        normals.add(Vector3f.UNIT_Y);
        normals.add(Vector3f.UNIT_Y);
        normals.add(Vector3f.UNIT_Y);
        normals.add(Vector3f.UNIT_Y);
        normals.add(Vector3f.UNIT_Y);

        /* Triangle t = new Triangle(v1, v3, v2);
        t.calculateNormal();
        Vector3f n = t.getNormal();

        normals.add(n);
        normals.add(n);
        normals.add(n);
        
        Triangle t2 = new Triangle(v2, v3, v4);
        t2.calculateNormal();
        Vector3f n2 = t2.getNormal();
        
        normals.add(n2);
        normals.add(n2);
        normals.add(n2); */
    }

    private void addQuadColor(ColorRGBA c1, ColorRGBA c2) {
        colors.add(c1);
        colors.add(c1);
        colors.add(c2);
        colors.add(c2);
    }

    private void addQuadColor(ColorRGBA c1, ColorRGBA c2, ColorRGBA c3, ColorRGBA c4) {
        colors.add(c1);
        colors.add(c2);
        colors.add(c3);
        colors.add(c4);
    }

    private void addQuadTerrainTypes(Vector3f types) {
        terrainTypes.add(types);
        terrainTypes.add(types);
        terrainTypes.add(types);
        terrainTypes.add(types);
    }

    private Vector3f perturb(Vector3f position) {
        Vector3f ret = position.clone();

        double xRandom = HexMetrics.noiseAmplitude * (noise.eval(position.getX() / HexMetrics.noiseScale, position.getZ() / HexMetrics.noiseScale, 0f) * 2f - 1f);
        double zRandom = HexMetrics.noiseAmplitude * (noise.eval(position.getX() / HexMetrics.noiseScale, 0f, position.getZ() / HexMetrics.noiseScale) * 2f - 1f);

        ret.setX(position.getX() + (float) xRandom);
        ret.setZ(position.getZ() + (float) zRandom);

        return ret;
    }

    public HexCell[] getCells() {
        return cells;
    }

    public void setCells(HexCell[] cells) {
        this.cells = cells;
    }
}
