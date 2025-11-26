package module4;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;
import java.util.Arrays;
import java.util.Random;

public class VectorDemo {
    // The "Species" determines the width (e.g., 256-bit, 512-bit) based on your
    // specific CPU
    static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    public static void main(String[] args) {
        System.out.println("=== Vector API (SIMD) Demo ===");
        System.out.println("CPU Vector Width: " + SPECIES.length() + " ints per cycle");

        int size = 10_000_000;
        int[] a = new int[size];
        int[] b = new int[size];
        int[] resultVector = new int[size];
        int[] resultScalar = new int[size];

        Random rand = new Random(42);
        for (int i = 0; i < size; i++) {
            a[i] = rand.nextInt(100);
            b[i] = rand.nextInt(100);
        }

        System.out.println("Data size: " + size + " elements");

        // 1. Standard Loop (Scalar)
        long start = System.nanoTime();
        scalarAdd(a, b, resultScalar);
        long end = System.nanoTime();
        System.out.println("Scalar Loop Time: " + (end - start) / 1_000_000 + " ms");

        // 2. Vector API (SIMD)
        start = System.nanoTime();
        vectorAdd(a, b, resultVector);
        end = System.nanoTime();
        System.out.println("Vector API Time:  " + (end - start) / 1_000_000 + " ms");

        // Verify correctness
        if (Arrays.equals(resultScalar, resultVector)) {
            System.out.println("Success: Results match.");
        } else {
            System.out.println("Error: Results do not match!");
        }
    }

    // Traditional Java Loop
    static void scalarAdd(int[] a, int[] b, int[] res) {
        for (int i = 0; i < a.length; i++) {
            res[i] = a[i] + b[i];
        }
    }

    // SIMD Vector Loop
    static void vectorAdd(int[] a, int[] b, int[] res) {
        int i = 0;
        // 1. DETERMINE THE WIDTH
        // How many numbers can YOUR specific CPU handle at once?
        // On modern Intel/AMD, this is usually 8 integers (256-bit AVX2) or 16 integers
        // (512-bit AVX512).
        // On Apple Silicon (M1/M2/M3), this is usually 4 integers (128-bit NEON).

        // 2. CALCULATE THE LIMIT (The "Loop Bound")
        // If array length is 100, and SPECIES width is 8.
        // loopBound(100) returns 96.
        // Why? Because 96 is divisible by 8. We can safely vector-process up to index
        // 96.
        // The remaining 4 items (96, 97, 98, 99) must be handled separately.
        int upperBound = SPECIES.loopBound(a.length);

        // 3. THE HIGHWAY LOOP
        // Instead of i++ (increment by 1), we do i += SPECIES.length() (increment by 8
        // or 16).
        for (; i < upperBound; i += SPECIES.length()) {

            // A. LOAD "A"
            // Go to array 'a', start at index 'i', and grab the next 8 integers AT ONCE
            // Result: A vector holding [a[i], a[i+1], ... a[i+7]]
            var va = IntVector.fromArray(SPECIES, a, i);

            // B. LOAD "B"
            // Go to array 'b', start at index 'i', and grab the next 8 integers AT ONCE
            // Result: A vector holding [b[i], b[i+1], ... b[i+7]]
            var vb = IntVector.fromArray(SPECIES, b, i);

            // C. ADD
            // This is the magic. ONE CPU instruction adds all 8 pairs simultaneously.
            // [a0+b0, a1+b1, ... a7+b7]
            var vc = va.add(vb);

            // D. STORE
            // Write the 8 results into the 'res' array starting at index 'i'.
            vc.intoArray(res, i);
        }

        // 4. THE TAIL LOOP (Cleanup)
        // We stopped at 96. We still need to process 96, 97, 98, 99.
        // Vectors can't handle partial chunks easily, so we fall back to a standard
        // loop.
        for (; i < a.length; i++) {
            res[i] = a[i] + b[i];
        }
    }
}
