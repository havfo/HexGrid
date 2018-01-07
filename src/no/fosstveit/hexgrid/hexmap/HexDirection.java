package no.fosstveit.hexgrid.hexmap;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public enum HexDirection {
    NE, E, SE, SW, W, NW;

    private HexDirection opposite;
    private static HexDirection[] vals = values();

    static {
        NE.opposite = SW;
        E.opposite = W;
        SE.opposite = NW;
        SW.opposite = NE;
        W.opposite = E;
        NW.opposite = SE;
    }

    public HexDirection opposite() {
        return opposite;
    }

    public HexDirection next() {
        if (this.ordinal() == vals.length - 1) {
            return vals[0];
        } else {
            return vals[this.ordinal() + 1];
        }
    }

    public HexDirection previous() {
        if (this.ordinal() == 0) {
            return vals[vals.length - 1];
        } else {
            return vals[this.ordinal() - 1];
        }
    }
}
