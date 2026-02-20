package javaapplication2;

import java.util.Scanner;

public class JavaApplication2 {

    // Properties (instance variables)
    private String name;
    private int age;

    private Scanner input = new Scanner(System.in);

    public void UserInputCode() {

        System.out.print("Enter your name: ");
        name = input.nextLine();

        System.out.print("Enter your age: ");
        age = input.nextInt();

        System.out.println("Hello " + name + "! You are " + age + " years old!");
    }

    public static void main(String[] args) {
        JavaApplication2 app = new JavaApplication2();
        app.UserInputCode();
    }
}