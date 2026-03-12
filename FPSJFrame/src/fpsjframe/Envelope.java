package fpsjframe;

import java.io.*;
import java.util.*;

/**
 * Envelope — a self-describing grid of digits.
 *
 * File format (.envelope):
 *   name: <name>
 *   type: world | chunk
 *   legend: 0=entity 1=entity ...
 *   ---
 *   <grid rows, one per line, digits only>
 *
 * Two subtypes enforce which entities are valid:
 *   WorldEnvelope — only biome entities (flatland, bushland, treeland)
 *   ChunkEnvelope — only tile entities  (dirt, bush, tree)
 */
public abstract class Envelope {

    // ── GLOBAL ENTITY REGISTRY ────────────────────────────────────
    public static final Set<String> BIOME_ENTITIES = Set.of("flatland", "bushland", "treeland", "testland");
    public static final Set<String> TILE_ENTITIES  = Set.of("dirt", "bush", "tree");

    // ── COVER ─────────────────────────────────────────────────────
    public final String name;
    public final int rows, cols;
    private final Map<Integer, String> legend = new HashMap<>();

    // ── CONTENTS ──────────────────────────────────────────────────
    public final int[][] contents;

    protected Envelope(String name, int[][] contents, Set<String> allowedEntities, Object... legendEntries) {
        this.name     = name;
        this.contents = contents;
        this.rows     = contents.length;
        this.cols     = contents[0].length;
        for (int i = 0; i < legendEntries.length - 1; i += 2) {
            int    digit  = (Integer) legendEntries[i];
            String entity = (String)  legendEntries[i + 1];
            if (!allowedEntities.contains(entity))
                throw new IllegalArgumentException(
                    "Envelope \"" + name + "\": entity \"" + entity + "\" not valid. Allowed: " + allowedEntities
                );
            legend.put(digit, entity);
        }
    }

    // constructor used by loader (legend already parsed)
    protected Envelope(String name, int[][] contents, Set<String> allowedEntities, Map<Integer,String> parsedLegend) {
        this.name     = name;
        this.contents = contents;
        this.rows     = contents.length;
        this.cols     = contents[0].length;
        for (Map.Entry<Integer,String> e : parsedLegend.entrySet()) {
            if (!allowedEntities.contains(e.getValue()))
                throw new IllegalArgumentException(
                    "Envelope \"" + name + "\": entity \"" + e.getValue() + "\" not valid. Allowed: " + allowedEntities
                );
            legend.put(e.getKey(), e.getValue());
        }
    }

    // ── API ───────────────────────────────────────────────────────

    public String read(int digit)            { return legend.getOrDefault(digit, "unknown"); }
    public String readAt(int col, int row)   { return read(contents[row][col]); }
    public int    get(int col, int row)      { return (row<0||row>=rows||col<0||col>=cols) ? -1 : contents[row][col]; }
    public boolean is(int col, int row, String entity) { return entity.equals(readAt(col, row)); }

    // ── FILE LOADER ───────────────────────────────────────────────

    /**
     * Load an .envelope file from the given path.
     * Returns a WorldEnvelope or ChunkEnvelope depending on the type: header.
     */
    public static Envelope load(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String name = null, type = null;
            Map<Integer, String> legend = new HashMap<>();
            List<int[]> gridRows = new ArrayList<>();
            boolean inGrid = false;

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (!inGrid) {
                    if (line.equals("---")) { inGrid = true; continue; }
                    if (line.startsWith("name:"))   name = line.substring(5).trim();
                    if (line.startsWith("type:"))   type = line.substring(5).trim();
                    if (line.startsWith("legend:")) {
                        for (String token : line.substring(7).trim().split("\\s+")) {
                            String[] kv = token.split("=");
                            legend.put((int) kv[0].charAt(0), kv[1]);
                        }
                    }
                } else {
                    int[] row = new int[line.length()];
                    for (int i = 0; i < line.length(); i++)
                        row[i] = (int) line.charAt(i);
                    gridRows.add(row);
                }
            }

            int[][] contents = gridRows.toArray(new int[0][]);
            if ("world".equals(type)) return new WorldEnvelope(name, contents, legend);
            if ("chunk".equals(type)) return new ChunkEnvelope(name, contents, legend);
            throw new IOException("Unknown envelope type: " + type);
        }
    }

    // ── SUBTYPES ──────────────────────────────────────────────────

    public static class WorldEnvelope extends Envelope {
        public WorldEnvelope(String name, int[][] contents, Object... legendEntries) {
            super(name, contents, BIOME_ENTITIES, legendEntries);
        }
        WorldEnvelope(String name, int[][] contents, Map<Integer,String> legend) {
            super(name, contents, BIOME_ENTITIES, legend);
        }
    }

    public static class ChunkEnvelope extends Envelope {
        public ChunkEnvelope(String name, int[][] contents, Object... legendEntries) {
            super(name, contents, TILE_ENTITIES, legendEntries);
        }
        ChunkEnvelope(String name, int[][] contents, Map<Integer,String> legend) {
            super(name, contents, TILE_ENTITIES, legend);
        }
        /** Accept a char[][] directly — char digit value: '0'->0, '1'->1, etc. */
        public ChunkEnvelope(String name, char[][] charGrid, Object... legendEntries) {
            super(name, toInt(charGrid), TILE_ENTITIES, legendEntries);
        }
        private static int[][] toInt(char[][] g) {
            int[][] out = new int[g.length][g[0].length];
            for (int y = 0; y < g.length; y++)
                for (int x = 0; x < g[0].length; x++)
                    out[y][x] = g[y][x] - '0';
            return out;
        }
    }
}