package fpsjframe;

import java.io.*;
import java.util.*;

/**
 * BlockEnvelope — a 3D block defined as 8 vertical columns, each 8 cells tall.
 * Columns run left-to-right (col 0 = leftmost face).
 * Each column is 8 rows top-to-bottom.
 *
 * File format (.envelope):
 *   name: bushBlock
 *   type: block
 *   legend: 0=air 1=dirt
 *   ---
 *   00000000   <- column 0, rows 0-7 (top to bottom)
 *   ...
 *   ===        <- separator between columns
 *   00011000   <- column 1
 *   ...
 */
public class BlockEnvelope {

    public static final int SIZE = 8; // fixed 8x8 per column

    public final String name;
    // columns[col][row] — col=0..7, row=0..7
    public final boolean[][] columns; // true = solid (dirt), false = air

    private BlockEnvelope(String name, boolean[][] columns) {
        this.name    = name;
        this.columns = columns;
    }

    /** Is cell (col, row) solid? */
    public boolean isSolid(int col, int row) {
        if (col < 0 || col >= SIZE || row < 0 || row >= SIZE) return false;
        return columns[col][row];
    }

    /** How many rows are solid in a given column (used to derive height). */
    public int solidRows(int col) {
        if (col < 0 || col >= SIZE) return 0;
        int count = 0;
        for (int r = 0; r < SIZE; r++) if (columns[col][r]) count++;
        return count;
    }

    /** Load a block envelope from a .envelope file. */
    public static BlockEnvelope load(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String name = null;
            char airChar = '0', solidChar = '1';
            List<boolean[]> cols = new ArrayList<>();
            List<String> currentColLines = new ArrayList<>();
            boolean inGrid = false;

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!inGrid) {
                    if (line.equals("---")) { inGrid = true; continue; }
                    if (line.startsWith("name:")) name = line.substring(5).trim();
                    if (line.startsWith("legend:")) {
                        for (String token : line.substring(7).trim().split("\\s+")) {
                            String[] kv = token.split("=");
                            char k = kv[0].charAt(0);
                            if (kv[1].equals("air"))  airChar   = k;
                            if (kv[1].equals("dirt")) solidChar = k;
                        }
                    }
                } else {
                    if (line.equals("===") || line.isEmpty()) {
                        if (!currentColLines.isEmpty()) {
                            cols.add(parseColumn(currentColLines, solidChar));
                            currentColLines.clear();
                        }
                    } else {
                        currentColLines.add(line);
                    }
                }
            }
            // flush last column
            if (!currentColLines.isEmpty())
                cols.add(parseColumn(currentColLines, solidChar));

            // pad to SIZE columns if fewer provided
            while (cols.size() < SIZE) cols.add(new boolean[SIZE]);

            return new BlockEnvelope(name, cols.toArray(new boolean[0][]));
        }
    }

    private static boolean[] parseColumn(List<String> lines, char solidChar) {
        boolean[] col = new boolean[SIZE];
        for (int r = 0; r < Math.min(lines.size(), SIZE); r++)
            if (r < lines.size() && lines.get(r).length() > 0)
                col[r] = lines.get(r).charAt(0) == solidChar;
        return col;
    }

    /** Hardcoded bush block — roughly round blob shape. */
    public static BlockEnvelope makeBush() {
        // Each column: which of 8 rows are solid (top=0, bottom=7)
        String[] cols = {
            "00011000",
            "00111100",
            "01111110",
            "01111110",
            "01111110",
            "01111110",
            "00111100",
            "00011000",
        };
        return fromStrings("bushBlock", cols);
    }

    /** Hardcoded tree block — tall trunk with canopy. */
    public static BlockEnvelope makeTree() {
        String[] cols = {
            "00011000",
            "00111100",
            "01111110",
            "11111111",
            "11111111",
            "01111110",
            "00111100",
            "00011000",
        };
        return fromStrings("treeBlock", cols);
    }

    private static BlockEnvelope fromStrings(String name, String[] colStrs) {
        boolean[][] cols = new boolean[SIZE][SIZE];
        for (int c = 0; c < SIZE; c++)
            for (int r = 0; r < SIZE; r++)
                cols[c][r] = colStrs[c].charAt(r) == '1';
        return new BlockEnvelope(name, cols);
    }
}