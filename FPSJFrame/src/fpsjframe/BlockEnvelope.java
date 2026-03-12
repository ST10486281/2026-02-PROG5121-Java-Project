package fpsjframe;

import java.io.*;
import java.util.*;

/**
 * BlockEnvelope — a 3D block defined as 8 vertical column slices, left to right.
 *
 * File format:
 *   Each === section = one left-to-right column slice (col 0=leftmost, col 7=rightmost)
 *   Each line within = one vertical level (row 0=top, row 7=bottom)
 *   Each char on that line = depth cell in that row (front to back)
 *   '1' or '#' = solid, '0' = air
 *
 * Example minecraft tree (section = one left-right slice):
 *   11111111   <- row 0 (top): all 8 depth positions solid = full canopy row
 *   11111111
 *   11111111
 *   00000000   <- row 3: all air
 *   000##000   <- row 4: only middle depth positions have trunk
 *   ...
 *
 * isSolid(col, row, depth): is the voxel at left-right=col, vertical=row, depth=depth solid?
 * For 2.5D rendering: isSolid(col, depth) checks if the ray's depth position hits solid at this row,
 * then renders the full vertical span of solid rows in that column.
 */
public class BlockEnvelope {

    public static final int SIZE = 8;

    public final String name;
    // voxels[col][row][depth] — true = solid
    public final boolean[][][] voxels;

    private BlockEnvelope(String name, boolean[][][] voxels) {
        this.name   = name;
        this.voxels = voxels;
    }

    /** Is voxel at (col, row, depth) solid? */
    public boolean isSolid(int col, int row, int depth) {
        if (col<0||col>=SIZE||row<0||row>=SIZE||depth<0||depth>=SIZE) return false;
        return voxels[col][row][depth];
    }

    /** Does this column slice at this depth have ANY solid voxel (used for hit detection)? */
    public boolean hasAnySolid(int col, int depth) {
        if (col<0||col>=SIZE||depth<0||depth>=SIZE) return false;
        for (int row=0; row<SIZE; row++) if (voxels[col][row][depth]) return true;
        return false;
    }

    /** Get the topmost and bottommost solid row for column+depth (for rendering span). */
    public int[] solidRowSpan(int col, int depth) {
        int top=-1, bot=-1;
        for (int row=0; row<SIZE; row++) {
            if (voxels[col][row][depth]) {
                if (top==-1) top=row;
                bot=row;
            }
        }
        return new int[]{top, bot};
    }

    public static BlockEnvelope load(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String name = null;
            // sections[col] -> list of lines (each line = one row, chars = depth)
            List<List<String>> sections = new ArrayList<>();
            List<String> current = new ArrayList<>();
            boolean inGrid = false;

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!inGrid) {
                    if (line.equals("---")) { inGrid = true; continue; }
                    if (line.startsWith("name:")) name = line.substring(5).trim();
                } else {
                    if (line.equals("===")) {
                        sections.add(new ArrayList<>(current));
                        current.clear();
                    } else if (!line.isEmpty()) {
                        current.add(line);
                    }
                }
            }
            if (!current.isEmpty()) sections.add(current);

            boolean[][][] vox = new boolean[SIZE][SIZE][SIZE];
            for (int col=0; col<Math.min(sections.size(),SIZE); col++) {
                List<String> rows = sections.get(col);
                for (int row=0; row<Math.min(rows.size(),SIZE); row++) {
                    String ln = rows.get(row);
                    for (int depth=0; depth<Math.min(ln.length(),SIZE); depth++) {
                        char c = ln.charAt(depth);
                        vox[col][row][depth] = (c=='1'||c=='#');
                    }
                }
            }
            return new BlockEnvelope(name, vox);
        }
    }

    public static BlockEnvelope makeBush() {
        // round blob — solid middle columns and rows
        String[] rows = { "00000000","00111100","01111110","01111110",
                          "01111110","01111110","00111100","00000000" };
        return fromCols("bushBlock", new String[][]{rows,rows,rows,rows,rows,rows,rows,rows});
    }

    public static BlockEnvelope makeTree() {
        String[] canopy = {"11111111","11111111","11111111","00000000","00000000","00000000","00000000","00000000"};
        String[] trunk  = {"11111111","11111111","11111111","00011000","00011000","00011000","00011000","00000000"};
        return fromCols("treeBlock", new String[][]{canopy,canopy,canopy,trunk,trunk,canopy,canopy,canopy});
    }

    private static BlockEnvelope fromCols(String name, String[][] colRows) {
        boolean[][][] vox = new boolean[SIZE][SIZE][SIZE];
        for (int col=0; col<SIZE; col++)
            for (int row=0; row<SIZE; row++)
                for (int depth=0; depth<SIZE; depth++)
                    vox[col][row][depth] = colRows[col][row].charAt(depth)=='1'||colRows[col][row].charAt(depth)=='#';
        return new BlockEnvelope(name, vox);
    }
}