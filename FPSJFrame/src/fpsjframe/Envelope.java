package fpsjframe;

import java.util.HashMap;
import java.util.Map;

/**
 * Envelope — a self-describing grid of digits.
 *
 * cover:    metadata — name + legend mapping digits → entity names
 * contents: the raw 2D digit grid
 *
 * Digits in contents mean nothing on their own — the cover tells you
 * what each digit represents in this particular envelope.
 */
public class Envelope {

    // ── COVER ─────────────────────────────────────────────────────
    public final String name;
    public final int rows, cols;
    private final Map<Integer, String> legend = new HashMap<>();

    // ── CONTENTS ──────────────────────────────────────────────────
    public final int[][] contents;

    public Envelope(String name, int[][] contents, Object... legendEntries) {
        this.name     = name;
        this.contents = contents;
        this.rows     = contents.length;
        this.cols     = contents[0].length;
        // legendEntries: alternating int digit, String entityName
        for (int i = 0; i < legendEntries.length - 1; i += 2)
            legend.put((Integer) legendEntries[i], (String) legendEntries[i + 1]);
    }

    /** What entity does this digit represent in this envelope? */
    public String read(int digit) {
        return legend.getOrDefault(digit, "unknown");
    }

    /** Read the entity name at grid position (col, row) */
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
}