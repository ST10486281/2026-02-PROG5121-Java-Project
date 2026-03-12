package fpsjframe;

public class BiomeSystem {

    // ── WORLD MAP ─────────────────────────────────────────────────
    // 6x6 grid of biome chunks. Each chunk = 10x10 world units.
    // 1 = Flatland, 2 = Bushland, 3 = Treeland
    public static final int WORLD_COLS = 6, WORLD_ROWS = 6;
    public static final int CHUNK_SIZE = 10;
    public static final int WORLD_W = WORLD_COLS * CHUNK_SIZE; // 60
    public static final int WORLD_H = WORLD_ROWS * CHUNK_SIZE; // 60

    private int[][] worldMap = {
        { 3, 1, 2, 2, 3, 3 },
        { 1, 1, 2, 2, 3, 3 },
        { 1, 2, 2, 3, 3, 2 },
        { 2, 2, 3, 3, 2, 1 },
        { 3, 3, 2, 2, 1, 1 },
        { 3, 3, 2, 1, 1, 3 },
    };

    // ── BIOME CHUNK MAPS ──────────────────────────────────────────
    // Each is a 10x10 tile layout for that biome type.
    // 0 = dirt (open), 1 = bush (solid), 2 = tree (solid)

    // Flatland — just dirt
    private int[][] chunkFlat = {
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
    };

    // Bushland — dirt with sparse bushes
    private int[][] chunkBush = {
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
    };

    // Treeland — dirt, some bushes, and trees
    private int[][] chunkTree = {
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
    };

    // ── LOOKUP ────────────────────────────────────────────────────

    public int getBiome(int col, int row) {
        if (col < 0 || col >= WORLD_COLS || row < 0 || row >= WORLD_ROWS) return 0;
        return worldMap[row][col];
    }

    // Returns cell type at world position: 0=dirt, 1=bush, 2=tree
    public int getCell(int wx, int wy) {
        if (wx < 0 || wx >= WORLD_W || wy < 0 || wy >= WORLD_H) return 2; // boundary = solid
        int col = wx / CHUNK_SIZE, row = wy / CHUNK_SIZE;
        int tx  = wx % CHUNK_SIZE, ty  = wy % CHUNK_SIZE;
        int biome = worldMap[row][col];
        int[][] chunk = (biome == 1) ? chunkFlat : (biome == 2) ? chunkBush : chunkTree;
        return chunk[ty][tx];
    }

    public boolean isSolid(double wx, double wy) {
        return getCell((int) wx, (int) wy) != 0;
    }
}