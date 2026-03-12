package fpsjframe;

public class TreeAsciiTest {

    public static void main(String[] args) {
        TreeAscii tree = new TreeAscii(40, 20);
        print(tree);
    }

    static void print(TreeAscii tree) {
        System.out.println("TreeAscii [" + tree.W + "x" + tree.H + "]");
        for (int y = 0; y < tree.H; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < tree.W; x++)
                sb.append(tree.isTree(x, y) ? "#" : " ");
            System.out.println(sb);
        }
    }
}