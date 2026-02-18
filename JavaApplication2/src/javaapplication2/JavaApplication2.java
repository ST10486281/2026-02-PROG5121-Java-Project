/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package javaapplication2;

import java.util.Scanner;

/**
 *
 * @author lab_services_student
 */
public class JavaApplication2 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        // String myName = "Riko";
        // char in = 'd';
        // int age = 8;
        // double exactAge = 8.95;
        // System.out.print("Hello " + myName);
        // System.out.println("/Please enter you id.");
        // // return void;

        Scanner input = new Scanner(System.in);

        System.out.print("Enter your name: ");
        String name = input.nextLine();

        System.out.println("Hello " + name);
    }

    // int methodExample(){

    // }

}
