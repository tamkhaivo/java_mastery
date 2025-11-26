package module1;

public class JITDemo {
    public static void main(String[] args) {
        long start = System.nanoTime();
        
        // The "Hot Loop"
        // We run this enough times to trigger C1 (Tier 3) and C2 (Tier 4) compilers
        for (int i = 0; i < 50_000; i++) {
            performCalculation(i);
        }

        long end = System.nanoTime();
        System.out.println("Done in " + (end - start) / 1_000_000 + " ms");
    }

    // This method is the candidate for compilation
    public static int performCalculation(int input) {
        int result = 0;
        for (int j = 0; j < 1000; j++) {
            result += (input * j) % 13;
        }
        return result;
    }
}
