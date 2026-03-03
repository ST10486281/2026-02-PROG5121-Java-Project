import java.util.ArrayList;
import java.util.Scanner;

public class JavaApplication2 {

    static class Item {
        double price;
        int quantity;

        Item(double price, int quantity) {
            this.price = price;
            this.quantity = quantity;
        }

        double getTotal() {
            return price * quantity;
        }
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        ArrayList<Item> items = new ArrayList<>();
        double grandTotal = 0;
        String choice;

        do {
            System.out.print("Enter price: ");
            double price = scanner.nextDouble();

            System.out.print("Enter quantity: ");
            int quantity = scanner.nextInt();

            items.add(new Item(price, quantity));

            System.out.print("Add another item? (yes/no): ");
            choice = scanner.next();

        } while (choice.equalsIgnoreCase("yes"));

        System.out.println("----- Checkout Breakdown -----");

        for (Item item : items) {
            System.out.println("Price: R" + item.price +
                    " | Quantity: " + item.quantity +
                    " | Total: R" + item.getTotal());

            grandTotal += item.getTotal();
        }

        System.out.println("Grand Total: R" + grandTotal);

        scanner.close();
    }
}
