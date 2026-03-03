import java.util.Scanner;

public class Class2 {

    static final double MAX_VALUE = 100;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter first number: ");
        double num1 = scanner.nextDouble();

        System.out.print("Enter second number: ");
        double num2 = scanner.nextDouble();

        displayResult("Addition", num1 + num2);
        displayResult("Subtraction", num1 - num2);
        displayResult("Multiplication", num1 * num2);

        if (num2 != 0) {
            displayResult("Division", num1 / num2);
            displayResult("Modulus", num1 % num2);
        } else {
            System.out.println("Division & Modulus: Cannot divide by zero");
        }

        scanner.close();
    }

    public static void displayResult(String operation, double result) {
        System.out.println(operation + ": " + result);

        if (result > MAX_VALUE) {
            System.out.println("Result is too high (exceeds " + MAX_VALUE + ")");
        }
    }
}