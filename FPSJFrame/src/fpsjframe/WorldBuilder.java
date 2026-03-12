package fpsjframe;

import java.awt.Color;
import java.io.*;
import java.util.*;

/**
 * WorldBuilder
 *
 * Parses .envelope files and expands the full world hierarchy into a flat 3-D
 * cell grid that the renderer queries with isSolid(x, y, z) / getColor(x, y, z).
 *
 * Hierarchy
 *   World  : 2 × 2 chunks          (worldMap.envelope)
 *   Chunk  : N × N objects         (e.g. chunkFlat.envelope → 10 × 10 objects)
 *   Object : 8 × 8 × 8 cells       (e.g. objectTree.envelope)
 *   Cell   : 25 × 25 × 25 cm  →  1 logical unit
 *
 * Usage
 *   WorldBuilder wb = new WorldBuilder("path/to/envelopes");
 *   boolean hit = wb.isSolid(x, y, z);
 *   Color   col = wb.getColor(x, y, z);
 */
public class WorldBuilder {

    // ── public world dimensions ───────────────────────────────────────────────

    public final int worldCellsX;
    public final int worldCellsZ;
    public static final int WORLD_HEIGHT = 8;

    // ── flat cell arrays ──────────────────────────────────────────────────────

    public final boolean[][][] solid;      // solid[x][y][z]
    public final Color[][][] cellColor;    // cellColor[x][y][z]

    // ── colour palette ────────────────────────────────────────────────────────

    private static final Color COL_DIRT       = new Color(139, 115,  85);
    private static final Color COL_BUSH_BODY  = new Color( 60, 120,  40);
    private static final Color COL_BUSH_STEM  = new Color( 90,  70,  30);
    private static final Color COL_TREE_LEAF  = new Color( 34,  90,  30);
    private static final Color COL_TREE_TRUNK = new Color( 90,  60,  30);
    private static final Color COL_DEFAULT    = new Color(160, 160, 160);

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @param envelopeDir  directory containing all .envelope files
     */
    public WorldBuilder(String envelopeDir) throws IOException {

        // ── Step 1: load all envelope files ──────────────────────────────────

        Map<String, ObjectShape> objectShapes = new HashMap<>();
        Map<String, ChunkLayout> chunkLayouts = new HashMap<>();
        String[][] worldNames = null;

        File dir = new File(envelopeDir);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".envelope"));
        if (files == null || files.length == 0)
            throw new IOException("No .envelope files found in: " + envelopeDir);

        for (File f : files) {
            List<String> lines = readLines(f);
            String type = header(lines, "type");
            switch (type) {
                case "object": {
                    ObjectShape s = parseObject(lines);
                    objectShapes.put(s.name, s);
                    break;
                }
                case "chunk": {
                    ChunkLayout c = parseChunk(lines);
                    chunkLayouts.put(c.name, c);
                    break;
                }
                case "world": {
                    worldNames = parseWorldNames(lines);
                    break;
                }
            }
        }

        if (worldNames == null)
            throw new IOException("No world envelope found.");

        // ── Step 2: resolve world name grid → ChunkLayout grid ───────────────

        int worldRows = worldNames.length;
        int worldCols = worldNames[0].length;

        ChunkLayout[][] worldGrid = new ChunkLayout[worldRows][worldCols];
        for (int r = 0; r < worldRows; r++) {
            for (int c = 0; c < worldCols; c++) {
                ChunkLayout cl = chunkLayouts.get(worldNames[r][c]);
                if (cl == null)
                    throw new IOException("Chunk not found: " + worldNames[r][c]);
                worldGrid[r][c] = cl;
            }
        }

        // ── Step 3: compute total cell dimensions ─────────────────────────────

        ChunkLayout ref    = worldGrid[0][0];
        int cellsPerChunkX = ref.cols * 8;
        int cellsPerChunkZ = ref.rows * 8;

        worldCellsX = worldCols * cellsPerChunkX;
        worldCellsZ = worldRows * cellsPerChunkZ;

        solid     = new boolean[worldCellsX][WORLD_HEIGHT][worldCellsZ];
        cellColor = new Color  [worldCellsX][WORLD_HEIGHT][worldCellsZ];

        // ── Step 4: stamp every object into the flat grid ─────────────────────

        for (int chunkRow = 0; chunkRow < worldRows; chunkRow++) {
            for (int chunkCol = 0; chunkCol < worldCols; chunkCol++) {

                ChunkLayout chunk = worldGrid[chunkRow][chunkCol];
                int chunkOriginX  = chunkCol * cellsPerChunkX;
                int chunkOriginZ  = chunkRow * cellsPerChunkZ;

                for (int objCol = 0; objCol < chunk.cols; objCol++) {
                    for (int objRow = 0; objRow < chunk.rows; objRow++) {

                        ObjectShape obj = objectShapes.get(chunk.objects[objCol][objRow]);
                        if (obj == null) continue;

                        int ox = chunkOriginX + objCol * 8;
                        int oz = chunkOriginZ + objRow * 8;
                        stampObject(obj, ox, oz);
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public query API
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isSolid(int x, int y, int z) {
        if (outOfBounds(x, y, z)) return false;
        return solid[x][y][z];
    }

    public Color getColor(int x, int y, int z) {
        if (outOfBounds(x, y, z)) return COL_DEFAULT;
        Color c = cellColor[x][y][z];
        return c != null ? c : COL_DEFAULT;
    }

    private boolean outOfBounds(int x, int y, int z) {
        return x < 0 || x >= worldCellsX
            || y < 0 || y >= WORLD_HEIGHT
            || z < 0 || z >= worldCellsZ;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal data holders
    // ─────────────────────────────────────────────────────────────────────────

    /** Parsed object envelope — 8×8×8 voxel shape. */
    private static class ObjectShape {
        final String name;
        final boolean[][][] voxels = new boolean[8][8][8];  // [x][y][z]
        ObjectShape(String name) { this.name = name; }
    }

    /** Parsed chunk envelope — cols×rows grid of object names. */
    private static class ChunkLayout {
        final String name;
        final int cols, rows;
        final String[][] objects;   // objects[col][row] → object name

        ChunkLayout(String name, int cols, int rows, String[][] objects) {
            this.name    = name;
            this.cols    = cols;
            this.rows    = rows;
            this.objects = objects;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Envelope parsers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parse an object envelope.
     *
     * Grid layout (after "---"):
     *   8 slices separated by "===" → slice index = X axis (0 = left)
     *   Each slice has 8 rows      → row 0 in file = top = y 7 (inverted)
     *   Each row has 8 chars       → char index = Z axis (0 = front)
     *   '#' (or any non-air legend value) = solid
     */
    private static ObjectShape parseObject(List<String> lines) {
        String name = header(lines, "name");
        Map<Character, String> legend = parseLegend(header(lines, "legend"));
        ObjectShape shape = new ObjectShape(name);

        List<List<String>> slices = splitIntoSlices(gridLines(lines));

        for (int x = 0; x < Math.min(8, slices.size()); x++) {
            List<String> rows = slices.get(x);
            for (int row = 0; row < Math.min(8, rows.size()); row++) {
                int y    = 7 - row;           // row 0 = top = y 7
                String l = rows.get(row);
                for (int z = 0; z < Math.min(8, l.length()); z++) {
                    String meaning = legend.getOrDefault(l.charAt(z), "air");
                    shape.voxels[x][y][z] = !meaning.equals("air");
                }
            }
        }
        return shape;
    }

    /**
     * Parse a chunk envelope.
     *
     * Grid layout (after "---"):
     *   Each row  → Z axis (row 0 = z 0)
     *   Each char → X axis (col 0 = x 0)
     *   Char maps via legend to an object name.
     */
    private static ChunkLayout parseChunk(List<String> lines) {
        String name = header(lines, "name");
        Map<Character, String> legend = parseLegend(header(lines, "legend"));
        List<String> grid = gridLines(lines);

        int rows = grid.size();
        int cols = grid.stream().mapToInt(String::length).max().orElse(0);

        String[][] objects = new String[cols][rows];
        for (int row = 0; row < rows; row++) {
            String l = grid.get(row);
            for (int col = 0; col < cols; col++) {
                char ch = (col < l.length()) ? l.charAt(col) : '0';
                objects[col][row] = legend.getOrDefault(ch, "objectDirt");
            }
        }
        return new ChunkLayout(name, cols, rows, objects);
    }

    /**
     * Parse a world envelope.
     * Returns names[row][col] — chunk name for each world slot.
     */
    private static String[][] parseWorldNames(List<String> lines) {
        Map<Character, String> legend = parseLegend(header(lines, "legend"));
        List<String> grid = gridLines(lines);

        int rows = grid.size();
        int cols = grid.stream().mapToInt(String::length).max().orElse(0);

        String[][] names = new String[rows][cols];
        for (int row = 0; row < rows; row++) {
            String l = grid.get(row);
            for (int col = 0; col < cols; col++) {
                char ch = (col < l.length()) ? l.charAt(col) : 'a';
                names[row][col] = legend.getOrDefault(ch, "chunkFlat");
            }
        }
        return names;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stamp & colour
    // ─────────────────────────────────────────────────────────────────────────

    private void stampObject(ObjectShape obj, int ox, int oz) {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                for (int z = 0; z < 8; z++) {
                    if (!obj.voxels[x][y][z]) continue;
                    int wx = ox + x;
                    int wz = oz + z;
                    if (wx >= worldCellsX || wz >= worldCellsZ) continue;
                    solid    [wx][y][wz] = true;
                    cellColor[wx][y][wz] = pickColor(obj.name, x, y, z);
                }
            }
        }
    }

    /** Choose a display colour based on object type and local voxel position. */
    private static Color pickColor(String objName, int x, int y, int z) {
        switch (objName) {
            case "objectDirt":
                return COL_DIRT;

            case "objectBush":
                // bottom two rows are the thin stem; above is the leafy body
                return (y <= 1) ? COL_BUSH_STEM : COL_BUSH_BODY;

            case "objectTree":
                // central 2×2 columns at lower half = trunk; rest = canopy
                boolean isTrunk = (x >= 3 && x <= 4 && z >= 3 && z <= 4 && y < 5);
                return isTrunk ? COL_TREE_TRUNK : COL_TREE_LEAF;

            default:
                return COL_DEFAULT;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Low-level text utilities
    // ─────────────────────────────────────────────────────────────────────────

    private static List<String> readLines(File f) throws IOException {
        List<String> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String l;
            while ((l = br.readLine()) != null) out.add(l.trim());
        }
        return out;
    }

    /** Extract the value from a "key: value" header line. */
    private static String header(List<String> lines, String key) {
        String prefix = key + ":";
        for (String l : lines)
            if (l.startsWith(prefix)) return l.substring(prefix.length()).trim();
        return "";
    }

    /** Parse "a=objectDirt b=objectBush" into a char → name map. */
    private static Map<Character, String> parseLegend(String raw) {
        Map<Character, String> map = new HashMap<>();
        for (String token : raw.split("\\s+")) {
            if (token.length() >= 3 && token.charAt(1) == '=')
                map.put(token.charAt(0), token.substring(2));
        }
        return map;
    }

    /** Return only the lines that appear after the "---" separator. */
    private static List<String> gridLines(List<String> lines) {
        List<String> out = new ArrayList<>();
        boolean inGrid = false;
        for (String l : lines) {
            if (l.equals("---")) { inGrid = true; continue; }
            if (inGrid) out.add(l);
        }
        return out;
    }

    /** Split a flat line list into slices divided by "===" markers. */
    private static List<List<String>> splitIntoSlices(List<String> lines) {
        List<List<String>> slices  = new ArrayList<>();
        List<String>       current = new ArrayList<>();
        for (String l : lines) {
            if (l.equals("===")) {
                slices.add(current);
                current = new ArrayList<>();
            } else {
                current.add(l);
            }
        }
        if (!current.isEmpty()) slices.add(current);
        return slices;
    }
}