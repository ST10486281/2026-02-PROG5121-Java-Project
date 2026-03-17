package fpsjframe;
public class SphereEnvelopeGeneratorTest {
    public static void main(String[] args) {
        int sizeX  = 20, sizeY = 20, sizeZ = 20, slices = 20;
        char airChar = 'O', solidChar = '#';

        System.out.println(SphereEnvelopeGenerator.generate(sizeX, sizeY, sizeZ, slices, airChar, solidChar));
    }
}