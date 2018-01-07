package no.fosstveit.hexgrid.enums;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public enum Terrains {
	// Name, assets, movement cost, shields, gold, science, food
	GRASSLAND("Grassland", "no.fosstveit.civ.assets.grassland", 1, 2, 0, 0, 2, 2),
	PLAINS("Plains", "no.fosstveit.civ.assets.plains", 1, 2, 0, 0, 2, 2),
	HILLS("Hills", "no.fosstveit.civ.assets.hills", 1, 1, 2, 1, 2, 2),
	RIVER("River", "no.fosstveit.civ.assets.river", 1, 2, 0, 0, 2, 2),
	MOUNTAINS("Mountains", "no.fosstveit.civ.assets.mountains", 2, 0, 2, 1, 2, 2),
	DESERT("Desert", "no.fosstveit.civ.assets.dessert", 1, 2, 0, 0, 2, 2),
	JUNGLE("Jungle", "no.fosstveit.civ.assets.jungle", 2, 2, 0, 0, 2, 2),
	SWAMP("Swamp", "no.fosstveit.civ.assets.swamp", 2, 2, 0, 0, 2, 2),
	FOREST("Forest", "no.fosstveit.civ.assets.forest", 1, 2, 0, 0, 2, 2),
	TUNDRA("Tundra", "no.fosstveit.civ.assets.tundra", 1, 2, 0, 0, 2, 2),
	ARCTIC("Arctic", "no.fosstveit.civ.assets.arctic", 1, 2, 0, 0, 2, 2),
	OCEAN("Ocean", "no.fosstveit.civ.assets.ocean", 1, 2, 0, 0, 2, 2);
	
	private final String name;
	private final String assets;
	private final int movementCost;
	private final int shields;
	private final int gold;
	private final int science;
	private final int score;
	private final int food;
	
	private Terrains(String name, String assets, int movementCost, int shields, int gold, int science, int score, int food) {
		this.name = name;
		this.assets = assets;
		this.movementCost = movementCost;
		this.shields = shields;
		this.gold = gold;
		this.science = science;
		this.score = score;
		this.food = food;
	}

	public String getName() {
		return name;
	}

	public String getAssets() {
		return assets;
	}

	public int getMovementCost() {
		return movementCost;
	}

	public int getShields() {
		return shields;
	}

	public int getGold() {
		return gold;
	}

	public int getScience() {
		return science;
	}

	public int getScore() {
		return score;
	}

	public int getFood() {
		return food;
	}
}
