package no.fosstveit.hexgrid.builders;

import no.fosstveit.hexgrid.objects.Tile;
import no.fosstveit.hexgrid.enums.Terrains;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public class TileBuilder {
	private Terrains terrain;
	private int x;
	private int y;
	private boolean special;
	
	public TileBuilder() {
	}
	
	public Tile buildTile() {
		return new Tile(terrain, x, y, special);
	}
	
	public TileBuilder terrain(Terrains terrain) {
		this.terrain = terrain;
		return this;
	}

	public TileBuilder x(int x) {
		this.x = x;
		return this;
	}

	public TileBuilder y(int y) {
		this.y = y;
		return this;
	}

	public TileBuilder special(boolean special) {
		this.special = special;
		return this;
	}
}
