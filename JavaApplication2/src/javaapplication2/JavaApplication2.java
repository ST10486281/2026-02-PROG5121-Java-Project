package javaapplication2;

import java.util.Scanner;

public class JavaApplication2 {

    // Properties (instance variables)
    private String name;
    private int age;

 

    public static void main(String[] args) {
        JavaApplication2 app = new JavaApplication2();
       
        Scanner input = new Scanner(System.in);

        System.out.print("Enter your name: ");
        name = input.nextLine();

        System.out.print("Enter your age: ");
        age = input.nextInt();
       
        System.out.println("Hello " + name + "! You are " + age + " years old!");
    }
}