package no.fosstveit.hexgrid.utils;

import java.awt.Color;

/**
 *
 * @author Håvar Aambø Fosstveit
 */
public class Constants {

    public static final int GRASS = 0;
    public static final int MUD = 1;
    public static final int SAND = 2;
    public static final int SNOW = 3;
    public static final int STONE = 4;
    public static final int PLAIN = 5;
    public static final int JUNGLE = 6;
    public static final int HILL = 7;

    public static Color deepColor = new Color(0f, 0f, 0.5f);
    public static Color shallowColor = new Color(25 / 255f, 25 / 255f, 150 / 255f);
    public static Color sandColor = new Color(240 / 255f, 240 / 255f, 64 / 255f);
    public static Color grassColor = new Color(50 / 255f, 220 / 255f, 20 / 255f);
    public static Color forestColor = new Color(16 / 255f, 160 / 255f, 0);
    public static Color rockColor = new Color(0.5f, 0.5f, 0.5f);
    public static Color snowColor = new Color(1f, 1f, 1f);

    public static float deepWater = 0.45f;
    public static float shallowWater = 0.6f;
    public static float sand = 0.65f;
    public static float grass = 0.75f;
    public static float forest = 0.9f;
    public static float rock = 0.97f;
}
