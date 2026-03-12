package fpsjframe;

public class TreeAsciiTest {

    public static void main(String[] args) {
        int canvasW = 80, canvasH = 40, treeW = 20, treeH = 10;
        if (args.length >= 2) { canvasW = Integer.parseInt(args[0]); canvasH = Integer.parseInt(args[1]); }
        if (args.length >= 4) { treeW   = Integer.parseInt(args[2]); treeH   = Integer.parseInt(args[3]); }
        else { treeW = canvasW; treeH = canvasH; }
        print(new TreeAscii(canvasW, canvasH, treeW, treeH));
    }

    static void print(TreeAscii tree) {
        System.out.println("TreeAscii [canvas " + tree.W + "x" + tree.H + "]");
        for (int y = 0; y < tree.H; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < tree.W; x++)
                sb.append(tree.isTree(x, y) ? "#" : " ");
            System.out.println(sb);
        }
    }
}