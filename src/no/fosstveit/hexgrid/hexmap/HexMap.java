package no.fosstveit.hexgrid.hexmap;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeMap;
import no.fosstveit.hexgrid.builders.TileBuilder;
import no.fosstveit.hexgrid.enums.Terrains;
import no.fosstveit.hexgrid.objects.Tile;
import no.fosstveit.hexgrid.utils.Constants;
import no.fosstveit.hexgrid.utils.OpenSimplexNoise;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public class HexMap {

    private static final String TAG = HexMap.class.getName();

    public static Tile[][] mapTiles;
    // Width = X , height = Y
    private int landMass = 1;
    private int temperature = 1;
    private int climate = 1;
    private int age = 1;
    private int mapSeed;

    private final int cellCountX;
    private final int cellCountZ;

    private final int chunkCountX;
    private final int chunkCountZ;

    private HexCell[] cells;
    HexGridChunk[] chunks;

    private Random rand = new Random(System.currentTimeMillis());
    private OpenSimplexNoise noise = new OpenSimplexNoise(System.currentTimeMillis());

    public HexMap(int chunkCountX, int chunkCountZ) {
        this.chunkCountX = chunkCountX;
        this.chunkCountZ = chunkCountZ;

        this.cellCountX = chunkCountX * HexMetrics.chunkSizeX;
        this.cellCountZ = chunkCountZ * HexMetrics.chunkSizeZ;
        
        mapSeed = rand.nextInt(16);
        generateMap();

        createChunks();
        createCells();
        triangulate();
    }

    private void createChunks() {
        chunks = new HexGridChunk[chunkCountX * chunkCountZ];

        for (int z = 0, i = 0; z < chunkCountZ; z++) {
            for (int x = 0; x < chunkCountX; x++) {
                chunks[i++] = new HexGridChunk(this, noise);
            }
        }
    }

    private void createCells() {
        cells = new HexCell[cellCountZ * cellCountX];

        for (int z = 0, i = 0; z < cellCountZ; z++) {
            for (int x = 0; x < cellCountX; x++) {
                createCell(x, z, i++);
            }
        }
    }

    public HexCell getCell(Vector3f position) {
        // position = transform.InverseTransformPoint(position);
        HexCoordinates coordinates = HexCoordinates.fromPosition(position);
        int index = coordinates.getX() + coordinates.getZ() * cellCountX + coordinates.getZ() / 2;
        return cells[index];
    }

    public HexCell getCell(HexCoordinates coordinates) {
        int z = coordinates.getZ();
        if (z < 0 || z >= cellCountZ) {
            return null;
        }
        int x = coordinates.getX() + z / 2;
        if (x < 0 || x >= cellCountX) {
            return null;
        }
        return cells[x + z * cellCountX];
    }

    private void createCell(int x, int z, int i) {
        Vector3f position = new Vector3f((x + z * 0.5f - z / 2) * (HexMetrics.innerRadius * 2f), 0f, z * (HexMetrics.outerRadius * 1.5f));

        cells[i] = new HexCell(position);
        cells[i].setHexCoordinates(HexCoordinates.fromOffsetCoordinates(x, z));

        if (HexMetrics.createBridges) {
            switch (mapTiles[x][z].getTerrain()) {
                case MOUNTAINS:
                    cells[i].setElevation(3);
                    break;
                case HILLS:
                    cells[i].setElevation(2);
                    break;
                case OCEAN:
                    cells[i].setElevation(0);
                    break;
                default:
                    cells[i].setElevation(1);
                    break;
            }
        }

        switch (mapTiles[x][z].getTerrain()) {
            case MOUNTAINS:
                cells[i].setTerrainTypeIndex(Constants.STONE);
                break;
            case HILLS:
                cells[i].setTerrainTypeIndex(Constants.HILL);
                break;
            case SWAMP:
                cells[i].setTerrainTypeIndex(Constants.MUD);
                break;
            case DESERT:
                cells[i].setTerrainTypeIndex(Constants.SAND);
                break;
            case OCEAN:
                cells[i].setTerrainTypeIndex(Constants.SAND);
                break;
            case ARCTIC:
            case TUNDRA:
                cells[i].setTerrainTypeIndex(Constants.SNOW);
                break;
            case PLAINS:
                cells[i].setTerrainTypeIndex(Constants.PLAIN);
                break;
            case JUNGLE:
                cells[i].setTerrainTypeIndex(Constants.JUNGLE);
                break;
            default:
                cells[i].setTerrainTypeIndex(Constants.GRASS);
                break;
        }

        

        if (x > 0) {
            cells[i].setNeighbor(HexDirection.W, cells[i - 1]);
        }

        if (z > 0) {
            if ((z & 1) == 0) {
                cells[i].setNeighbor(HexDirection.SE, cells[i - cellCountX]);
                if (x > 0) {
                    cells[i].setNeighbor(HexDirection.SW, cells[i - cellCountX - 1]);
                }
            } else {
                cells[i].setNeighbor(HexDirection.SW, cells[i - cellCountX]);
                if (x < cellCountX - 1) {
                    cells[i].setNeighbor(HexDirection.SE, cells[i - cellCountX + 1]);
                }
            }
        }

        addCellToChunk(x, z, cells[i]);
    }

    private void addCellToChunk(int x, int z, HexCell cell) {
        int chunkX = x / HexMetrics.chunkSizeX;
        int chunkZ = z / HexMetrics.chunkSizeZ;
        HexGridChunk chunk = chunks[chunkX + chunkZ * chunkCountX];

        int localX = x - chunkX * HexMetrics.chunkSizeX;
        int localZ = z - chunkZ * HexMetrics.chunkSizeZ;
        chunk.addCell(localX + localZ * HexMetrics.chunkSizeX, cell);
    }

    private void triangulate() {
        for (HexGridChunk chunk : chunks) {
            chunk.triangulateCells();
        }
    }

    public HexGridChunk[] getChunks() {
        return chunks;
    }

    private void generateMap() {
        mapTiles = new Tile[cellCountX][cellCountZ];

        System.out.println("Generating land mass");
        int[][] elevation = generateLandMass();
        System.out.println("Temperature adjustements");
        int[][] latitude = temperatureAdjustements();

        System.out.println("Merge elevation and temperature");
        mergeElevationAndLatitude(elevation, latitude);
        elevation = null;
        latitude = null;

        System.out.println("Climate adjustments");
        climateAdjustments();
        System.out.println("Age adjustment");
        ageAdjustments();
        System.out.println("Create rivers");
        createRivers();

        System.out.println("Calculate continent sizes");
        calculateContinentSize();
        System.out.println("Create poles");
        createPoles();
        System.out.println("Place huts");
        placeHuts();
        System.out.println("Calculate land value");
        calculateLandValue();

        System.out.println("Map created!");

        // printMap();
    }

    private void printMap() {
        for (int y = 0; y < cellCountZ; y++) {
            for (int x = 0; x < cellCountX; x++) {
                switch (mapTiles[x][y].getTerrain()) {
                    case OCEAN:
                        System.out.print(" \t");
                        break;
                    case JUNGLE:
                        System.out.print("∓\t");
                        break;
                    case MOUNTAINS:
                        System.out.print("⋀\t");
                        break;
                    case HILLS:
                        System.out.print("⋂\t");
                        break;
                    case RIVER:
                        System.out.print("≀\t");
                        break;
                    case GRASSLAND:
                        System.out.print("∴\t");
                        break;
                    case PLAINS:
                        System.out.print("∷\t");
                        break;
                    case ARCTIC:
                        System.out.print("⋇\t");
                        break;
                    case TUNDRA:
                        System.out.print("∗\t");
                        break;
                    case DESERT:
                        System.out.print("≎\t");
                        break;
                    case FOREST:
                        System.out.print("⋔\t");
                        break;
                    case SWAMP:
                        System.out.print("⋕\t");
                        break;
                    default:
                        System.out.print("#\t");
                        break;
                }
            }
            System.out.println();
        }
    }

    private void calculateLandValue() {
        for (int y = 2; y < cellCountZ - 2; y++) {
            for (int x = 2; x < cellCountX - 2; x++) {
                // If the square's terrain type is not Plains, Grassland or River, then its land value is 0
                if (mapTiles[x][y].getTerrain() != Terrains.PLAINS && mapTiles[x][y].getTerrain() != Terrains.GRASSLAND && mapTiles[x][y].getTerrain() != Terrains.RIVER) {
                    continue;
                }

                // for each 'city square' neighbouring the map square (i.e. each square following the city area pattern,
                // including the map square itself, so totally 21 'neighbours'), compute the following neighbour value (initially 0):
                int landValue = 0;
                for (int yy = -2; yy <= 2; yy++) {
                    for (int xx = -2; xx <= 2; xx++) {
                        // Skip the corners of the square to create a city area pattern
                        if (Math.abs(xx) == 2 && Math.abs(yy) == 2) {
                            continue;
                        }

                        // initial value is 0
                        int val = 0;

                        Tile tile = mapTiles[x + xx][y + yy];
                        if (tile.isSpecial() && (tile.getTerrain() == Terrains.GRASSLAND || tile.getTerrain() == Terrains.RIVER)) {
                            // If the neighbour square type is Grassland special or River special, add 2,
                            // then add the non-special Grassland or River terrain type score to the neighbour value
                            val += 2;
                            if (tile.getTerrain() == Terrains.RIVER) {
                                val += Terrains.RIVER.getScore();
                            } else {
                                val += Terrains.GRASSLAND.getScore();
                            }
                        } else {
                            // Else add neighbour's terrain type score to the neighbour value
                            val += tile.getTerrain().getScore();
                        }

                        // If the neighbour square is in the map square inner circle, i.e. one of the 8 neighbours immediatly
                        // surrounding the map square, then multiply the neighbour value by 2
                        if (Math.abs(xx) <= 1 && Math.abs(yy) <= 1 && (xx != 0 || yy != 0)) {
                            val *= 2;
                        }

                        // If the neighbour square is the North square (relative offset 0,-1), then multiply the neighbour value by 2 ;
                        // note: I actually think that this is a bug, and that the intention was rather to multiply by 2 if the 'neighbour'
                        // was the central map square itself... the actual CIV code for this is to check if the 'neighbour index' is '0';
                        // the neighbour index is used to retrieve the neighbour's relative offset coordinates (x,y) from the central square,
                        // and the central square itself is actually the last in the list (index 20), the first one (index 0) being
                        // the North neighbour; another '7x7 neighbour pattern' table found in CIV code does indeed set the central square
                        // at index 0, and this why I believe ths is a programmer's mistake...
                        if (xx == 0 && yy == -1) {
                            val *= 2;
                        }

                        // Add the neighbour's value to the map square total value and loop to next neighbour
                        landValue += val;
                    }
                }

                // After all neighbours are processed, if the central map square's terrain type is non-special Grassland or River,
                // subtract 16 from total land value
                if (!mapTiles[x][y].isSpecial() && (mapTiles[x][y].getTerrain() == Terrains.GRASSLAND || mapTiles[x][y].getTerrain() == Terrains.RIVER)) {
                    landValue -= 16;
                }

                landValue -= 120; // Substract 120 (0x78) from the total land value,
                boolean negative = (landValue < 0); // and remember its sign
                landValue = Math.abs(landValue); // Set the land value to the absolute land value (i.e. negate it if it is negative)
                landValue /= 8; // Divide the land value by 8
                if (negative) {
                    landValue = 1 - landValue; // If the land value was negative 3 steps before, then negate the land value and add 1
                }

                // Adjust the land value to the range [1..15]
                if (landValue < 1) {
                    landValue = 1;
                }

                if (landValue > 15) {
                    landValue = 15;
                }

                landValue /= 2; // Divide the land value by 2
                landValue += 8; // And finally, add 8 to the land value
                mapTiles[x][y].setLandValue((byte) landValue);
            }
        }
    }

    private void placeHuts() {
        for (int y = 0; y < cellCountZ; y++) {
            for (int x = 0; x < cellCountX; x++) {
                if (mapTiles[x][y].getTerrain() == Terrains.OCEAN) {
                    continue;
                }

                mapTiles[x][y].setHut(tileHasHut(x, y));
            }
        }
    }

    private boolean tileHasHut(int x, int y) {
        if (y < 2 || y > (cellCountZ - 3)) {
            return false;
        }

        return modGrid(x, y) == ((x / 4) * 13 + (y / 4) * 11 + mapSeed + 8) % 32;
    }

    private void createPoles() {
        for (int x = 0; x < cellCountX; x++) {
            mapTiles[x][0] = new TileBuilder().x(x).y(0).terrain(Terrains.ARCTIC).buildTile();
            mapTiles[x][cellCountZ - 1] = new TileBuilder().x(x).y(cellCountZ - 1).terrain(Terrains.ARCTIC).buildTile();
        }

        for (int i = 0; i < (cellCountX / 4); i++) {
            int x = rand.nextInt(cellCountX);
            mapTiles[x][0] = new TileBuilder().x(x).y(0).terrain(Terrains.TUNDRA).buildTile();
            x = rand.nextInt(cellCountX);
            mapTiles[x][cellCountZ - 1] = new TileBuilder().x(x).y(cellCountZ - 1).terrain(Terrains.TUNDRA).buildTile();
            x = rand.nextInt(cellCountX);
            mapTiles[x][1] = new TileBuilder().x(x).y(1).terrain(Terrains.TUNDRA).buildTile();
            x = rand.nextInt(cellCountX);
            mapTiles[x][cellCountZ - 2] = new TileBuilder().x(x).y(cellCountZ - 2).terrain(Terrains.TUNDRA).buildTile();
        }
    }

    private int getContinentSize(int continentID) {
        int count = 0;

        for (int x = 0; x < cellCountX; x++) {
            for (int y = 0; y < cellCountZ; y++) {
                count += (mapTiles[x][y].getContinentID() == continentID) ? 1 : 0;
            }
        }

        return count;
    }

    private void mergeContinents(int fromID, int toID) {
        for (int x = 0; x < cellCountX; x++) {
            for (int y = 0; y < cellCountZ; y++) {
                if (mapTiles[x][y].getContinentID() == fromID) {
                    mapTiles[x][y].setContinentID(toID);
                }
            }
        }
    }

    private void calculateContinentSize() {
        // Initial continents
        byte continentId = 0;
        for (int y = 0; y < cellCountZ; y++) {
            for (int x = 0; x < cellCountX; x++) {
                Tile tile = getMapTile(x, y);
                Tile north = getMapTile(x, y - 1);
                Tile west = getMapTile(x - 1, y);

                if (north != null && ((north.getTerrain() == Terrains.OCEAN) && (tile.getTerrain() == Terrains.OCEAN)) && north.getContinentID() > 0) {
                    tile.setContinentID(north.getContinentID());
                } else if (west != null && ((west.getTerrain() == Terrains.OCEAN) && (tile.getTerrain() == Terrains.OCEAN)) && west.getContinentID() > 0) {
                    tile.setContinentID(west.getContinentID());
                } else {
                    tile.setContinentID(++continentId);
                }

                if (north == null || west == null) {
                    continue;
                }

                if ((north.getTerrain() == Terrains.OCEAN) != (west.getTerrain() == Terrains.OCEAN)) {
                    continue;
                }

                // Merge continents
                if (north.getContinentID() != west.getContinentID() && north.getContinentID() > 0 && west.getContinentID() > 0) {
                    int northCount = getContinentSize(north.getContinentID());
                    int westCount = getContinentSize(west.getContinentID());

                    if (northCount > westCount) {
                        mergeContinents(west.getContinentID(), north.getContinentID());
                    } else {
                        mergeContinents(north.getContinentID(), west.getContinentID());
                    }
                }
            }
        }

        for (int x = 0; x < cellCountX; x++) {
            for (int y = 0; y < cellCountZ; y++) {
                Tile tile = getMapTile(x, y);
                Tile north = getMapTile(x, y - 1);
                Tile west = getMapTile(x - 1, y);

                if (north == null || west == null) {
                    continue;
                }

                if ((north.getTerrain() == Terrains.OCEAN) != (west.getTerrain() == Terrains.OCEAN)) {
                    continue;
                }

                // Merge continents
                if (north.getContinentID() != west.getContinentID() && north.getContinentID() > 0 && west.getContinentID() > 0) {
                    int northCount = getContinentSize(north.getContinentID());
                    int westCount = getContinentSize(west.getContinentID());

                    if (northCount > westCount) {
                        mergeContinents(west.getContinentID(), north.getContinentID());
                    } else {
                        mergeContinents(north.getContinentID(), west.getContinentID());
                    }
                }
            }
        }

        // Sort continents by size (descending) and with ceiling of size 14, all other continents are 15
        TreeMap<Integer, Integer> continentSizes = new TreeMap<Integer, Integer>(
                new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        });

        ArrayList<Integer> continentIDs = new ArrayList<Integer>();
        for (int x = 0; x < cellCountX; x++) {
            for (int y = 0; y < cellCountZ; y++) {
                if (!continentIDs.contains(mapTiles[x][y].getContinentID())) {
                    continentIDs.add(mapTiles[x][y].getContinentID());
                }
            }
        }

        for (int ids : continentIDs) {
            continentSizes.put(getContinentSize(ids), ids);
        }

        if (continentSizes.size() >= 15) {
            int count = 0;
            for (java.util.Map.Entry<Integer, Integer> entry : continentSizes.entrySet()) {
                if (count >= 15) {
                    mergeContinents(entry.getValue(), 15);
                }
                count++;
            }
        }
    }

    public static Tile[][] deepCopy(Tile[][] original) {
        if (original == null) {
            return null;
        }

        final Tile[][] result = new Tile[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = Arrays.copyOf(original[i], original[i].length);
        }

        return result;
    }

    private void createRivers() {
        int rivers = 0;
        for (int i = 0; i < 256 && rivers < ((climate + landMass) * 2) + 6; i++) {
            Tile[][] tilesBackup = deepCopy(mapTiles);

            int riverLength = 0;
            int varA = rand.nextInt(4) * 2;
            boolean nearOcean = false;

            Tile tile = null;
            while (tile == null) {
                int x = rand.nextInt(cellCountX);
                int y = rand.nextInt(cellCountZ);

                if (mapTiles[x][y].getTerrain() == Terrains.HILLS) {
                    tile = mapTiles[x][y];
                }
            }

            do {
                mapTiles[tile.getX()][tile.getY()] = new TileBuilder().x(tile.getX()).y(tile.getY()).terrain(Terrains.RIVER).buildTile();
                int varB = varA;
                int varC = rand.nextInt(2);
                varA = (((varC - riverLength % 2) * 2 + varA) & 0x07);
                varB = 7 - varB;

                riverLength++;

                nearOcean = nearOcean(tile.getX(), tile.getY());
                switch (varA) {
                    case 0:
                    case 1:
                        tile = mapTiles[tile.getX()][tile.getY() - 1];
                        break;
                    case 2:
                    case 3:
                        tile = mapTiles[tile.getX() + 1][tile.getY()];
                        break;
                    case 4:
                    case 5:
                        tile = mapTiles[tile.getX()][tile.getY() + 1];
                        break;
                    case 6:
                    case 7:
                        tile = mapTiles[tile.getX() - 1][tile.getY()];
                        break;
                }
            } while (!nearOcean && ((tile.getTerrain() != Terrains.OCEAN) && (tile.getTerrain() != Terrains.RIVER) && (tile.getTerrain() != Terrains.MOUNTAINS)));

            if ((nearOcean || (tile.getTerrain() == Terrains.RIVER)) && riverLength > 5) {
                rivers++;
                Tile[][] mapPart = getMapTile(tile.getX() - 3, tile.getY(), 7, 7);
                for (int x = 0; x < 7; x++) {
                    for (int y = 0; y < 7; y++) {
                        if (mapPart[x][y] == null) {
                            continue;
                        }

                        int xx = mapPart[x][y].getX();
                        int yy = mapPart[x][y].getY();

                        if (mapTiles[xx][yy].getTerrain() == Terrains.FOREST) {
                            mapTiles[xx][yy] = new TileBuilder().x(tile.getX()).y(tile.getY()).special(tileIsSpecial(x, y)).terrain(Terrains.JUNGLE).buildTile();
                        }
                    }
                }
            } else {
                mapTiles = tilesBackup.clone();
            }
        }
    }

    private Tile getMapTile(int x, int y) {
        if (y < 0 || y >= cellCountZ) {
            return null;
        }

        while (x < 0) {
            x += cellCountX;
        }

        x = (x % cellCountX);

        return mapTiles[x][y];
    }

    public Tile[][] getMapTile(int x, int y, int cellCountX, int cellCountZ) {
        if (cellCountX < 0) {
            cellCountX = Math.abs(cellCountX);
            x -= cellCountX;
        }

        if (cellCountZ < 0) {
            cellCountZ = Math.abs(cellCountZ);
            y -= cellCountZ;
        }

        Tile[][] output = new Tile[cellCountX][cellCountZ];

        for (int yy = y; yy < y + cellCountZ; yy++) {
            for (int xx = x; xx < x + cellCountX; xx++) {
                output[xx - x][yy - y] = getMapTile(xx, yy);
            }
        }

        return output;
    }

    private boolean nearOcean(int x, int y) {
        for (int relY = -1; relY <= 1; relY++) {
            for (int relX = -1; relX <= 1; relX++) {
                if (Math.abs(relX) == Math.abs(relY)) {
                    continue;
                }

                if (mapTiles[x + relX][y + relY].getTerrain() == Terrains.OCEAN) {
                    return true;
                }
            }
        }

        return false;
    }

    private void ageAdjustments() {
        int x = 0;
        int y = 0;
        int ageRepeat = (int) (((float) 800 * (1 + age) / (80 * 50)) * (cellCountX * cellCountZ));
        for (int i = 0; i < ageRepeat; i++) {
            if (i % 2 == 0) {
                x = rand.nextInt(cellCountX);
                y = rand.nextInt(cellCountZ);
            } else {
                switch (rand.nextInt(8)) {
                    case 0:
                        x--;
                        y--;
                        break;
                    case 1:
                        y--;
                        break;
                    case 2:
                        x++;
                        y--;
                        break;
                    case 3:
                        x--;
                        break;
                    case 4:
                        x++;
                        break;
                    case 5:
                        x--;
                        y++;
                        break;
                    case 6:
                        y++;
                        break;
                    default:
                        x++;
                        y++;
                        break;
                }

                if (x < 0) {
                    x = 1;
                }

                if (y < 0) {
                    y = 1;
                }

                if (x >= cellCountX) {
                    x = cellCountX - 2;
                }

                if (y >= cellCountZ) {
                    y = cellCountZ - 2;
                }
            }

            boolean special = tileIsSpecial(x, y);
            switch (mapTiles[x][y].getTerrain()) {
                case FOREST:
                    mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.JUNGLE).buildTile();
                    break;
                case SWAMP:
                    mapTiles[x][y] = new TileBuilder().x(x).y(y).terrain(Terrains.GRASSLAND).buildTile();
                    break;
                case PLAINS:
                    mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.HILLS).buildTile();
                    break;
                case TUNDRA:
                    mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.HILLS).buildTile();
                    break;
                case RIVER:
                    mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.FOREST).buildTile();
                    break;
                // case Terrain.Grassland1:
                case GRASSLAND:
                    mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.FOREST).buildTile();
                    break;
                case JUNGLE:
                    mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.SWAMP).buildTile();
                    break;
                case HILLS:
                    mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.MOUNTAINS).buildTile();
                    break;
                case MOUNTAINS:
                    if ((x == 0 || mapTiles[x - 1][y - 1].getTerrain() != Terrains.OCEAN)
                            && (y == 0 || mapTiles[x + 1][y - 1].getTerrain() != Terrains.OCEAN)
                            && (x == (cellCountX - 1) || mapTiles[x + 1][y + 1].getTerrain() != Terrains.OCEAN)
                            && (y == (cellCountZ - 1) || mapTiles[x - 1][y + 1].getTerrain() != Terrains.OCEAN)) {
                        mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.OCEAN).buildTile();
                    }
                    break;
                case DESERT:
                    mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.PLAINS).buildTile();
                    break;
                case ARCTIC:
                    mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.MOUNTAINS).buildTile();
                    break;
            }
        }
    }

    private void climateAdjustments() {
        int wetness, latitude;

        for (int y = 0; y < cellCountZ; y++) {
            int yy = (int) (((float) y / cellCountZ) * 50);

            wetness = 0;
            latitude = Math.abs(25 - yy);

            for (int x = 0; x < cellCountX; x++) {
                if (mapTiles[x][y].getTerrain() == Terrains.OCEAN) {
                    // wetness yield
                    int wy = (latitude - 12);

                    if (wy < 0) {
                        wy = -wy;
                    }

                    wy += (climate * 4);

                    if (wy > wetness) {
                        wetness++;
                    }
                } else if (wetness > 0) {
                    boolean special = tileIsSpecial(x, y);
                    int rainfall = rand.nextInt(7 - (climate * 2));
                    wetness -= rainfall;

                    switch (mapTiles[x][y].getTerrain()) {
                        case PLAINS:
                            // mapTiles[x][y] = new Grassland(x, y);
                            mapTiles[x][y] = new TileBuilder().x(x).y(y).terrain(Terrains.GRASSLAND).buildTile();
                            break;
                        case TUNDRA:
                            // mapTiles[x][y] = new Arctic(x, y, special);
                            mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.ARCTIC).buildTile();
                            break;
                        case HILLS:
                            // mapTiles[x][y] = new Forest(x, y, special);
                            mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.FOREST).buildTile();
                            break;
                        case DESERT:
                            // mapTiles[x][y] = new Plains(x, y, special);
                            mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.PLAINS).buildTile();
                            break;
                        case MOUNTAINS:
                            wetness -= 3;
                            break;
                    }
                }
            }

            wetness = 0;
            latitude = Math.abs(25 - yy);

            // reset row wetness to 0
            for (int x = cellCountX - 1; x >= 0; x--) {
                if (mapTiles[x][y].getTerrain() == Terrains.OCEAN) {
                    // wetness yield
                    int wy = (latitude / 2) + climate;
                    if (wy > wetness) {
                        wetness++;
                    }
                } else if (wetness > 0) {
                    boolean special = tileIsSpecial(x, y);
                    int rainfall = rand.nextInt(7 - (climate * 2));
                    wetness -= rainfall;

                    switch (mapTiles[x][y].getTerrain()) {
                        case SWAMP:
                            mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.FOREST).buildTile();
                            break;
                        case PLAINS:
                            mapTiles[x][y] = new TileBuilder().x(x).y(y).terrain(Terrains.GRASSLAND).buildTile();
                            break;
                        // case GRASSLAND1:
                        case GRASSLAND:
                            mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.JUNGLE).buildTile();
                            break;
                        case HILLS:
                            mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.FOREST).buildTile();
                            break;
                        case MOUNTAINS:
                            mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.FOREST).buildTile();
                            wetness -= 3;
                            break;
                        case DESERT:
                            mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.PLAINS).buildTile();
                            break;
                    }
                }
            }
        }
    }

    private boolean tileIsSpecial(int x, int y) {
        if (y < 2 || y > (cellCountZ - 3)) {
            return false;
        }

        return modGrid(x, y) == ((x / 4) * 13 + (y / 4) * 11 + mapSeed) % 16;
    }

    private int modGrid(int x, int y) {
        return (x % 4) * 4 + (y % 4);
    }

    private void mergeElevationAndLatitude(int[][] elevation, int[][] latitude) {
        for (int y = 0; y < cellCountZ; y++) {
            for (int x = 0; x < cellCountX; x++) {
                boolean special = tileIsSpecial(x, y);
                switch (elevation[x][y]) {
                    case 0:
                        // mapTiles[x][y] = new Ocean(x, y, special);
                        mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.OCEAN).buildTile();
                        break;
                    case 1: {
                        switch (latitude[x][y]) {
                            case 0:
                                // _tiles[x, y] = new Desert(x, y, special);
                                mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.DESERT).buildTile();
                                break;
                            case 1:
                                // _tiles[x, y] = new Plains(x, y, special);
                                mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.PLAINS).buildTile();
                                break;
                            case 2:
                                // _tiles[x, y] = new Tundra(x, y, special);
                                mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.TUNDRA).buildTile();
                                break;
                            case 3:
                                // _tiles[x, y] = new Arctic(x, y, special);
                                mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.ARCTIC).buildTile();
                                break;
                        }
                    }
                    break;
                    case 2:
                        // _tiles[x, y] =new Hills(x, y, special);
                        mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.HILLS).buildTile();
                        break;
                    default:
                        // _tiles[x, y] =new Mountains(x, y, special);
                        mapTiles[x][y] = new TileBuilder().x(x).y(y).special(special).terrain(Terrains.MOUNTAINS).buildTile();
                        break;
                }
            }
        }
    }

    private int[][] temperatureAdjustements() {
        int[][] latitude = new int[cellCountX][cellCountZ];

        for (int y = 0; y < cellCountZ; y++) {
            for (int x = 0; x < cellCountX; x++) {
                int l = (int) (((float) y / cellCountZ) * 50) - 29;
                l += rand.nextInt(7);

                if (l < 0) {
                    l = -l;
                }

                l += (1 - temperature);

                l = (l / 6) + 1;

                switch (l) {
                    case 0:
                    case 1:
                        latitude[x][y] = 0;
                        break;
                    case 2:
                    case 3:
                        latitude[x][y] = 1;
                        break;
                    case 4:
                    case 5:
                        latitude[x][y] = 2;
                        break;
                    case 6:
                    default:
                        latitude[x][y] = 3;
                        break;
                }
            }
        }

        return latitude;
    }

    private int calculateLandMassSize(int[][] elevation) {
        int result = 0;

        for (int i = 0; i < elevation.length; i++) {
            for (int j = 0; j < elevation[i].length; j++) {
                result += (elevation[i][j] > 0) ? 1 : 0;
            }
        }

        return result;
    }

    private int[][] generateLandMass() {
        int[][] elevation = new int[cellCountX][cellCountZ];
        int landMassSize = (int) ((cellCountZ * cellCountX) / 12.5) * (landMass + 2);

        while (calculateLandMassSize(elevation) < landMassSize) {
            boolean[][] chunk = generateLandChunk();
            for (int y = 0; y < cellCountZ; y++) {
                for (int x = 0; x < cellCountX; x++) {
                    elevation[x][y] += (chunk[x][y]) ? 1 : 0;
                }
            }
        }

        // remove narrow passages
        for (int y = 0; y < (cellCountZ - 1); y++) {
            for (int x = 0; x < (cellCountX - 1); x++) {
                if ((elevation[x][y] > 0 && elevation[x + 1][y + 1] > 0) && (elevation[x + 1][y] == 0 && elevation[x][y + 1] == 0)) {
                    elevation[x + 1][y]++;
                    elevation[x][y + 1]++;
                } else if ((elevation[x][y] == 0 && elevation[x + 1][y + 1] == 0) && (elevation[x + 1][y] > 0 && elevation[x][y + 1] > 0)) {
                    elevation[x + 1][y + 1]++;
                }
            }
        }

        return elevation;
    }

    private boolean[][] generateLandChunk() {
        boolean[][] stencil = new boolean[cellCountX][cellCountZ];

        int x = rand.nextInt(cellCountX - 8) + 4;
        int y = rand.nextInt(cellCountZ - 16) + 8;

        int pathLength = rand.nextInt(63) + 1;

        for (int i = 0; i < pathLength; i++) {
            stencil[x][y] = true;
            stencil[x + 1][y] = true;
            stencil[x][y + 1] = true;
            switch (rand.nextInt(4)) {
                case 0:
                    y--;
                    break;
                case 1:
                    x++;
                    break;
                case 2:
                    y++;
                    break;
                default:
                    x--;
                    break;
            }

            if (x < 3 || y < 3 || x > (cellCountX - 4) || y > (cellCountZ - 5)) {
                break;
            }
        }

        return stencil;
    }
}
