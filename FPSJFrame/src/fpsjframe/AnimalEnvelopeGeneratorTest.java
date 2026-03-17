package fpsjframe;

import fpsjframe.animalgeneration.AnimalLoader;
import fpsjframe.animalgeneration.AnimalNode;

public class AnimalEnvelopeGeneratorTest {
    public static void main(String[] args) {
        int sizeX = 20, sizeY = 20, sizeZ = 20, slices = 20;
        try {
            AnimalNode root = AnimalLoader.load("fpsjframe/animals/goat.txt");
            System.out.println(AnimalEnvelopeGenerator.generate(root, sizeX, sizeY, sizeZ, slices, "objectGoat"));
        } catch (Exception e) {
            System.err.println("Failed to load animal: " + e.getMessage());
        }
    }
}