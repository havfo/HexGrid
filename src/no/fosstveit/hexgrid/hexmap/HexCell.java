package no.fosstveit.hexgrid.hexmap;

import com.jme3.math.Vector3f;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public class HexCell {

    private Vector3f position;
    private int terrainTypeIndex;
    private HexCell[] neighbors = new HexCell[6];
    private int elevation;
    private HexCoordinates coordinates;
    private boolean hasIncomingRiver;
    private boolean hasOutgoingRiver;
    private HexDirection incomingRiver;
    private HexDirection outgoingRiver;
    private HexGridChunk parent;

    public HexCell(Vector3f position) {
        this.position = position;
    }

    public int getTerrainTypeIndex() {
        return terrainTypeIndex;
    }

    public void setTerrainTypeIndex(int terrainTypeIndex) {
        this.terrainTypeIndex = terrainTypeIndex;
    }

    public int getElevation() {
        return elevation;
    }

    public void setElevation(int elevation) {
        this.elevation = elevation;
        position.y = elevation * HexMetrics.elevationStep;
    }

    public Vector3f getPosition() {
        return position;
    }

    public HexCell getNeighbor(HexDirection direction) {
        return neighbors[direction.ordinal()];
    }

    public void setNeighbor(HexDirection direction, HexCell cell) {
        neighbors[direction.ordinal()] = cell;
        cell.setOppositeNeighbor(direction.opposite(), this);
    }

    public void setOppositeNeighbor(HexDirection direction, HexCell cell) {
        neighbors[direction.ordinal()] = cell;
    }

    public HexEdgeType getEdgeType(HexDirection direction) {
        return HexMetrics.getEdgeType(elevation, neighbors[direction.ordinal()].getElevation());
    }

    public HexEdgeType getEdgeType(HexCell otherCell) {
        return HexMetrics.getEdgeType(elevation, otherCell.getElevation());
    }

    public HexCoordinates getHexCoordinates() {
        return coordinates;
    }

    public void setHexCoordinates(HexCoordinates coordinates) {
        this.coordinates = coordinates;
    }

    public boolean isHasIncomingRiver() {
        return hasIncomingRiver;
    }

    public void setHasIncomingRiver(boolean hasIncomingRiver) {
        this.hasIncomingRiver = hasIncomingRiver;
    }

    public boolean isHasOutgoingRiver() {
        return hasOutgoingRiver;
    }

    public void setHasOutgoingRiver(boolean hasOutgoingRiver) {
        this.hasOutgoingRiver = hasOutgoingRiver;
    }

    public HexDirection getIncomingRiver() {
        return incomingRiver;
    }

    public void setIncomingRiver(HexDirection incomingRiver) {
        this.incomingRiver = incomingRiver;
    }

    public HexDirection getOutgoingRiver() {
        return outgoingRiver;
    }

    public void setOutgoingRiver(HexDirection outgoingRiver) {
        this.outgoingRiver = outgoingRiver;
    }

    public boolean hasRiver() {
        return hasIncomingRiver || hasOutgoingRiver;
    }

    public boolean hasRiverBeginOrEnd() {
        return hasIncomingRiver != hasOutgoingRiver;
    }

    public boolean hasRiverThroughEdge(HexDirection direction) {
        return hasIncomingRiver && incomingRiver == direction || hasOutgoingRiver && outgoingRiver == direction;
    }

    public void setChunk(HexGridChunk chunk) {
        this.parent = chunk;
    }
    
    public HexGridChunk getChunk() {
        return parent;
    }
}
