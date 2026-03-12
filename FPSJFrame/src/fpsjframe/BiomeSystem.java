package fpsjframe;

/**
 * BiomeSystem — world and biome data stored as Envelopes.
 *
 * Global entity registry (what every digit can mean across all envelopes):
 *   "dirt"      — open ground, walkable
 *   "bush"      — bush obstacle, solid
 *   "tree"      — tree obstacle, solid
 *   "flatland"  — biome: open flat terrain
 *   "bushland"  — biome: scattered bushes
 *   "treeland"  — biome: trees and bushes
 */
public class BiomeSystem {

    public static final int WORLD_COLS = 6, WORLD_ROWS = 6;
    public static final int CHUNK_SIZE = 10;
    public static final int WORLD_W = WORLD_COLS * CHUNK_SIZE; // 60
    public static final int WORLD_H = WORLD_ROWS * CHUNK_SIZE; // 60

    // ── WORLD MAP ENVELOPE ────────────────────────────────────────
    // cover legend: 0="flatland", 1="bushland", 2="treeland"
    public final Envelope.WorldEnvelope worldMap = new Envelope.WorldEnvelope(
        "worldMap",
        new int[][] {
            { 0, 0, 1, 1, 2, 2 },
            { 0, 0, 1, 1, 2, 2 },
            { 0, 1, 1, 2, 2, 1 },
            { 1, 1, 2, 2, 1, 0 },
            { 2, 2, 1, 1, 0, 0 },
            { 2, 2, 1, 0, 0, 0 },
        },
        0, "flatland",
        1, "bushland",
        2, "treeland"
    );

    // ── BIOME CHUNK ENVELOPES ─────────────────────────────────────
    // cover legend: 0="dirt", 1="bush", 2="tree"

    // Flatland — all dirt, nothing solid
    public final Envelope.ChunkEnvelope chunkFlat = new Envelope.ChunkEnvelope(
        "chunkFlat",
        new int[][] {
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        },
        0, "dirt",
        1, "bush",
        2, "tree"
    );

    // Bushland — dirt with sparse bushes
    public final Envelope.ChunkEnvelope chunkBush = new Envelope.ChunkEnvelope(
        "chunkBush",
        new int[][] {
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 1, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 1, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 1, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        },
        0, "dirt",
        1, "bush",
        2, "tree"
    );

    // Treeland — dirt, some bushes, and trees
    public final Envelope.ChunkEnvelope chunkTree = new Envelope.ChunkEnvelope(
        "chunkTree",
        new int[][] {
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 2, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 2 },
            { 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 2, 0, 0 },
        },
        0, "dirt",
        1, "bush",
        2, "tree"
    );

    // ── LOOKUP ────────────────────────────────────────────────────

    /** Get the biome envelope for a world chunk position */
    public Envelope getChunk(int worldCol, int worldRow) {
        String biome = worldMap.readAt(worldCol, worldRow);
        switch (biome) {
            case "bushland": return chunkBush;
            case "treeland": return chunkTree;
            default:         return chunkFlat;
        }
    }

    /** Entity name at world position (wx, wy) */
    public String getEntity(int wx, int wy) {
        if (wx < 0 || wx >= WORLD_W || wy < 0 || wy >= WORLD_H) return "tree"; // boundary
        int col = wx / CHUNK_SIZE, row = wy / CHUNK_SIZE;
        int tx  = wx % CHUNK_SIZE, ty  = wy % CHUNK_SIZE;
        return getChunk(col, row).readAt(tx, ty);
    }

    /** Raw cell value at world position */
    public int getCell(int wx, int wy) {
        if (wx < 0 || wx >= WORLD_W || wy < 0 || wy >= WORLD_H) return 2;
        int col = wx / CHUNK_SIZE, row = wy / CHUNK_SIZE;
        int tx  = wx % CHUNK_SIZE, ty  = wy % CHUNK_SIZE;
        return getChunk(col, row).get(tx, ty);
    }

    /** Is this world position solid (blocks movement)? */
    public boolean isSolid(double wx, double wy) {
        String e = getEntity((int) wx, (int) wy);
        return e.equals("bush") || e.equals("tree");
    }

    /** Biome name at a world chunk coordinate */
    public String getBiomeName(int col, int row) {
        return worldMap.readAt(col, row);
    }
}