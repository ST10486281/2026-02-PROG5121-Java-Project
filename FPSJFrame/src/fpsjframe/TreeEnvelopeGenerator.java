package fpsjframe;

public class TreeEnvelopeGenerator {

    public static String generate(long seed, int canvasW, int canvasH, int treeW, int treeH, char airChar, char treeChar) {
        TreeAscii t = new TreeAscii(canvasW, canvasH, treeW, treeH, airChar, treeChar, seed);

        StringBuilder sb = new StringBuilder();
        sb.append("name: objectTree\n");
        sb.append("type: object\n");
        sb.append("legend: " + airChar + "=air " + treeChar + "=tree\n");
        sb.append("---\n");

        // single slice: one row per Z, one char per X
        for (int z = 0; z < canvasH; z++) {
            sb.append(new String(t.grid[z]));
            sb.append('\n');
        }

        return sb.toString();
    }

    // Defaults: 20x20 canvas, seed 42, standard air/tree chars
    public static String generate(long seed) {
        return generate(seed, 20, 20, 20, 20, 'O', '#');
    }

    public static void main(String[] args) {
        long seed    = args.length > 0 ? Long.parseLong(args[0])  : 42;
        int canvasW  = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int canvasH  = args.length > 2 ? Integer.parseInt(args[2]) : 20;
        int treeW    = args.length > 3 ? Integer.parseInt(args[3]) : canvasW;
        int treeH    = args.length > 4 ? Integer.parseInt(args[4]) : canvasH;
        char airChar  = args.length > 5 ? args[5].charAt(0) : 'O';
        char treeChar = args.length > 6 ? args[6].charAt(0) : '#';

        System.out.print(generate(seed, canvasW, canvasH, treeW, treeH, airChar, treeChar));
    }
}