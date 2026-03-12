package fpsjframe;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Envelope — a self-describing grid of digits.
 *
 * cover:    name + legend mapping digits → entity names
 * contents: the raw 2D digit grid
 *
 * Two subtypes enforce which entities are valid:
 *   WorldEnvelope — only biome entities (flatland, bushland, treeland)
 *   ChunkEnvelope — only tile entities  (dirt, bush, tree)
 */
public abstract class Envelope {

    // ── GLOBAL ENTITY REGISTRY ────────────────────────────────────
    public static final Set<String> BIOME_ENTITIES = Set.of("flatland", "bushland", "treeland");
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
                    "Envelope \"" + name + "\": entity \"" + entity + "\" is not valid for this envelope type. " +
                    "Allowed: " + allowedEntities
                );
            legend.put(digit, entity);
        }
    }

    /** What entity does this digit represent? */
    public String read(int digit) {
        return legend.getOrDefault(digit, "unknown");
    }

    /** Entity name at grid position (col, row) */
    public String readAt(int col, int row) {
        return read(contents[row][col]);
    }

    /** Raw digit at (col, row) */
    public int get(int col, int row) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return -1;
        return contents[row][col];
    }

    /** Does this position contain the named entity? */
    public boolean is(int col, int row, String entity) {
        return entity.equals(readAt(col, row));
    }

    // ── SUBTYPES ──────────────────────────────────────────────────

    /** WorldEnvelope — only accepts biome entities (flatland, bushland, treeland) */
    public static class WorldEnvelope extends Envelope {
        public WorldEnvelope(String name, int[][] contents, Object... legendEntries) {
            super(name, contents, BIOME_ENTITIES, legendEntries);
        }
    }

    /** ChunkEnvelope — only accepts tile entities (dirt, bush, tree) */
    public static class ChunkEnvelope extends Envelope {
        public ChunkEnvelope(String name, int[][] contents, Object... legendEntries) {
            super(name, contents, TILE_ENTITIES, legendEntries);
        }
    }
}