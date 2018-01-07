package no.fosstveit.hexgrid.objects;

import no.fosstveit.hexgrid.enums.Terrains;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public class Tile {

    private Terrains terrain;

    private int x;
    private int y;

    private int continentID = -1;
    private byte landValue = 0;

    private boolean special;
    private boolean hut = false;

    public Tile(Terrains terrain, int x, int y, boolean special) {
        setTerrain(terrain);
        setX(x);
        setY(y);
        setSpecial(special);
    }

    public Terrains getTerrain() {
        return terrain;
    }

    public void setTerrain(Terrains terrain) {
        if (terrain == null) {
            throw new IllegalArgumentException("Tile terrain cannot be empty");
        }

        this.terrain = terrain;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    public int getContinentID() {
        return continentID;
    }

    public void setContinentID(int continentID) {
        this.continentID = continentID;
    }

    public byte getLandValue() {
        return landValue;
    }

    public void setLandValue(byte landValue) {
        this.landValue = landValue;
    }

    public boolean isSpecial() {
        return special;
    }

    public void setSpecial(boolean special) {
        this.special = special;
    }

    public void setHut(boolean hut) {
        this.hut = hut;
    }

    public boolean hasHut() {
        return hut;
    }
}
