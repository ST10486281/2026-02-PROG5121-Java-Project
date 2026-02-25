import java.util.Scanner;

public class JavaApplication2 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        double total = 0;

        for (int i = 1; i <= 3; i++) {
            System.out.println("Item " + i);

            System.out.print("Enter price: ");
            double price = scanner.nextDouble();

            System.out.print("Enter quantity: ");
            int quantity = scanner.nextInt();

            double itemTotal = price * quantity;
            total += itemTotal;

            System.out.println("Item " + i + " total: R" + itemTotal);
            System.out.println();
        }

        System.out.println("----- Checkout Breakdown -----");
        System.out.println("Grand Total: R" + total);

        scanner.close();
    }
}