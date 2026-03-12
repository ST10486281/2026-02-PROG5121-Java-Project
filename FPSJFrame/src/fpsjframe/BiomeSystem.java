package fpsjframe;

import java.io.IOException;

public class BiomeSystem {

    // ── HIERARCHY ─────────────────────────────────────────────────
    public static final int CELLS_PER_BLOCK  = 8;   // 8 cells = 2m
    public static final int BLOCKS_PER_CHUNK = 5;   // 5 blocks = 10m
    public static final int CHUNKS_PER_WORLD = 2;   // 2 chunks = 20m per axis

    public static final int CELLS_PER_CHUNK  = BLOCKS_PER_CHUNK * CELLS_PER_BLOCK; // 40
    public static final int WORLD_COLS       = CHUNKS_PER_WORLD;
    public static final int WORLD_ROWS       = CHUNKS_PER_WORLD;
    public static final int WORLD_W          = WORLD_COLS * CELLS_PER_CHUNK; // 80
    public static final int WORLD_H          = WORLD_ROWS * CELLS_PER_CHUNK; // 80

    // legacy alias used by minimap
    public static final int CHUNK_SIZE       = CELLS_PER_CHUNK;

    // ── ENVELOPES ────────────────────────────────────────────────
    public final Envelope.WorldEnvelope worldMap;
    public final Envelope.ChunkEnvelope chunkFlat, chunkBush, chunkTree;
    public final BlockEnvelope bushBlock, treeBlock;

    // ── WORLD CELL GRID (expanded from chunks+blocks) ────────────
    // cellGrid[wy][wx] = entity name at that cell
    private final String[][] cellGrid;
    // blockOriginX/Y[wy][wx] = world origin of the block this cell belongs to (-1 if none)
    private final int[][] blockOriginX, blockOriginY;

    public BiomeSystem(String mapsDir) {
        try {
            worldMap  = (Envelope.WorldEnvelope) Envelope.load(mapsDir + "/worldMap.envelope");
            chunkFlat = (Envelope.ChunkEnvelope) Envelope.load(mapsDir + "/chunkFlat.envelope");
            chunkBush = (Envelope.ChunkEnvelope) Envelope.load(mapsDir + "/chunkBush.envelope");
            chunkTree = (Envelope.ChunkEnvelope) Envelope.load(mapsDir + "/chunkTree.envelope");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load map files from: " + mapsDir, e);
        }

        BlockEnvelope bb = BlockEnvelope.makeBush(), tb = BlockEnvelope.makeTree();
        try { bb = BlockEnvelope.load(mapsDir + "/bushBlock.envelope"); } catch (IOException ignored) {}
        try { tb = BlockEnvelope.load(mapsDir + "/treeBlock.envelope"); } catch (IOException ignored) {}
        bushBlock = bb;
        treeBlock = tb;

        // ── BUILD CELL GRID ───────────────────────────────────────
        cellGrid = new String[WORLD_H][WORLD_W];
        blockOriginX = new int[WORLD_H][WORLD_W];
        blockOriginY = new int[WORLD_H][WORLD_W];
        for (int wy = 0; wy < WORLD_H; wy++)
            for (int wx = 0; wx < WORLD_W; wx++) {
                cellGrid[wy][wx] = "air";
                blockOriginX[wy][wx] = -1;
                blockOriginY[wy][wx] = -1;
            }

        // for each chunk in world
        for (int chunkRow = 0; chunkRow < WORLD_ROWS; chunkRow++) {
            for (int chunkCol = 0; chunkCol < WORLD_COLS; chunkCol++) {
                Envelope.ChunkEnvelope chunk = getChunkEnvelope(chunkCol, chunkRow);
                int chunkOriginX = chunkCol * CELLS_PER_CHUNK;
                int chunkOriginY = chunkRow * CELLS_PER_CHUNK;

                // for each block position in chunk (5x5)
                for (int by = 0; by < BLOCKS_PER_CHUNK; by++) {
                    for (int bx = 0; bx < BLOCKS_PER_CHUNK; bx++) {
                        String entity = chunk.readAt(bx, by);
                        if (entity.equals("air")) continue;

                        // block origin in cell space
                        int boX = chunkOriginX + bx * CELLS_PER_BLOCK;
                        int boY = chunkOriginY + by * CELLS_PER_BLOCK;

                        // get the block's footprint envelope
                        BlockEnvelope block = entity.equals("bushBlock") ? bushBlock
                                            : entity.equals("treeBlock") ? treeBlock
                                            : null;

                        // stamp 8x8 footprint into cell grid
                        for (int cy = 0; cy < CELLS_PER_BLOCK; cy++) {
                            for (int cx = 0; cx < CELLS_PER_BLOCK; cx++) {
                                int wx = boX + cx;
                                int wy2 = boY + cy;
                                if (wx >= WORLD_W || wy2 >= WORLD_H) continue;
                                boolean solid = false;
                                if (block != null) {
                                    for (int depth = 0; depth < CELLS_PER_BLOCK && !solid; depth++)
                                        for (int row = 0; row < CELLS_PER_BLOCK && !solid; row++)
                                            if (block.isSolid(cx, row, depth)) solid = true;
                                }
                                if (solid) {
                                    cellGrid[wy2][wx] = entity;
                                    blockOriginX[wy2][wx] = boX;
                                    blockOriginY[wy2][wx] = boY;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Envelope.ChunkEnvelope getChunkEnvelope(int col, int row) {
        switch (worldMap.readAt(col, row)) {
            case "bushland": return chunkBush;
            case "treeland": return chunkTree;
            default:         return chunkFlat;
        }
    }

    public String getEntity(int wx, int wy) {
        if (wx < 0 || wx >= WORLD_W || wy < 0 || wy >= WORLD_H) return "treeBlock";
        return cellGrid[wy][wx];
    }

    public int getRawCell(int wx, int wy) {
        String e = getEntity(wx, wy);
        return e.equals("air") ? (int)'0' : (int)'1';
    }

    public int getCell(int wx, int wy) {
        switch (getEntity(wx, wy)) {
            case "bushBlock": return 1;
            case "treeBlock": return 2;
            default:          return 0;
        }
    }

    public int[] getCellBlockOrigin(int wx, int wy) {
        if (wx < 0 || wx >= WORLD_W || wy < 0 || wy >= WORLD_H) return new int[]{-1,-1};
        return new int[]{blockOriginX[wy][wx], blockOriginY[wy][wx]};
    }

    public BlockEnvelope getBlock(int wx, int wy) {
        switch (getEntity(wx, wy)) {
            case "bushBlock": return bushBlock;
            case "treeBlock": return treeBlock;
            default:          return null;
        }
    }

    public boolean isSolid(double wx, double wy) {
        String e = getEntity((int) wx, (int) wy);
        return !e.equals("air");
    }

    /** 3-axis solid check used by the voxel raycaster. */
    public boolean isSolidXYZ(int wx, int y, int wz) {
        BlockEnvelope block = getBlock(wx, wz);
        if (block == null) return false;
        int[] origin = getCellBlockOrigin(wx, wz);
        if (origin[0] < 0) return false;
        int cx = wx - origin[0];
        int cz = wz - origin[1];
        cx = Math.max(0, Math.min(CELLS_PER_BLOCK - 1, cx));
        cz = Math.max(0, Math.min(CELLS_PER_BLOCK - 1, cz));
        for (int depth = 0; depth < CELLS_PER_BLOCK; depth++)
            if (block.isSolid(cx, y, depth)) return true;
        return false;
    }

    public String getBiomeName(int col, int row) {
        return worldMap.readAt(col, row);
    }
}