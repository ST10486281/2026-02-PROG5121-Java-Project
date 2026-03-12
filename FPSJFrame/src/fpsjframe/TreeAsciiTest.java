package fpsjframe;
public class TreeAsciiTest {
    public static void main(String[] args) {
        int canvasW = 20, canvasH = 20, treeW = 20, treeH = 20;
        // if (args.length >= 2) { canvasW = Integer.parseInt(args[0]); canvasH = Integer.parseInt(args[1]); }
        // if (args.length >= 4) { treeW   = Integer.parseInt(args[2]); treeH   = Integer.parseInt(args[3]); }

        // print(new TreeAscii(canvasW, canvasH, treeW, treeH, '░', '█'));
        printEnvelope(42, canvasW, canvasH, treeW, treeH, '░', '█');
    }

    static void print(TreeAscii tree) {
        for (int y = 0; y < tree.H; y++)
            System.out.println(new String(tree.grid[y]));
        System.out.println();
    }

    static void printEnvelope(long seed, int canvasW, int canvasH, int treeW, int treeH, char airChar, char treeChar) {
        System.out.println(TreeEnvelopeGenerator.generate(seed, canvasW, canvasH, treeW, treeH, airChar, treeChar));
    }
    // ██▓▒░
}