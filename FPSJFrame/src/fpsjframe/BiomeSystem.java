package fpsjframe;

import java.io.IOException;

public class BiomeSystem {

    public static final int WORLD_COLS = 6, WORLD_ROWS = 6;
    public static final int CHUNK_SIZE = 10;
    public static final int WORLD_W = WORLD_COLS * CHUNK_SIZE;
    public static final int WORLD_H = WORLD_ROWS * CHUNK_SIZE;

    public final Envelope.WorldEnvelope worldMap;
    public final Envelope.ChunkEnvelope chunkFlat, chunkBush, chunkTree, chunkTest;

    public BiomeSystem(String mapsDir) {
        try {
            worldMap  = (Envelope.WorldEnvelope) Envelope.load(mapsDir + "/worldMap.envelope");
            chunkFlat = (Envelope.ChunkEnvelope) Envelope.load(mapsDir + "/chunkFlat.envelope");
            chunkBush = (Envelope.ChunkEnvelope) Envelope.load(mapsDir + "/chunkBush.envelope");
            chunkTree = (Envelope.ChunkEnvelope) Envelope.load(mapsDir + "/chunkTree.envelope");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load map files from: " + mapsDir, e);
        }

        // chunkTest: dynamic — generated from TreeAscii, not a file
        // air='0'->0=dirt, tree='1'->1=bush
        TreeAscii ta = new TreeAscii(CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE, '0', '1');
        chunkTest = new Envelope.ChunkEnvelope("chunkTest", ta.grid, 0, "dirt", 1, "bush");
    }

    public Envelope getChunk(int worldCol, int worldRow) {
        switch (worldMap.readAt(worldCol, worldRow)) {
            case "bushland": return chunkBush;
            case "treeland": return chunkTree;
            case "testland": return chunkTest;
            default:         return chunkFlat;
        }
    }

    public String getEntity(int wx, int wy) {
        if (wx < 0 || wx >= WORLD_W || wy < 0 || wy >= WORLD_H) return "tree";
        return getChunk(wx / CHUNK_SIZE, wy / CHUNK_SIZE).readAt(wx % CHUNK_SIZE, wy % CHUNK_SIZE);
    }

    public int getCell(int wx, int wy) {
        if (wx < 0 || wx >= WORLD_W || wy < 0 || wy >= WORLD_H) return 2;
        return getChunk(wx / CHUNK_SIZE, wy / CHUNK_SIZE).get(wx % CHUNK_SIZE, wy % CHUNK_SIZE);
    }

    public boolean isSolid(double wx, double wy) {
        String e = getEntity((int) wx, (int) wy);
        return e.equals("bush") || e.equals("tree");
    }

    public String getBiomeName(int col, int row) {
        return worldMap.readAt(col, row);
    }
}