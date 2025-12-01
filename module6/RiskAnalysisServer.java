package module6;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;

public class RiskAnalysisServer {

    static final int MAX_DATA_SIZE = 100_000;
    static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
    static final int ITERATIONS = 5; // 5 Solid iterations

    // Pre-computed data
    static float[] DATA_A = new float[MAX_DATA_SIZE];
    static float[] DATA_B = new float[MAX_DATA_SIZE];

    // BLACKHOLE to prevent Dead Code Elimination (JIT optimization)
    static volatile float BLACKHOLE = 0.0f;

    record Scenario(String name, int numRequests, int dataSize, int poolSize) {
    }

    static class Stats {
        double mean;
        double stdDev;

        public Stats(List<Double> vals) {
            DoubleSummaryStatistics ss = vals.stream().mapToDouble(d -> d).summaryStatistics();
            this.mean = ss.getAverage();
            double var = vals.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / vals.size();
            this.stdDev = Math.sqrt(var);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Module 6: The 3x3 Matrix (Corrected for DCE) ===");
        System.out.println("Vector Species: " + SPECIES.toString());

        // Setup Data
        Random r = new Random();
        for (int i = 0; i < MAX_DATA_SIZE; i++) {
            DATA_A[i] = r.nextFloat();
            DATA_B[i] = r.nextFloat();
        }

        // Define the 3x3 Matrix
        int[] reqLevels = { 1_000, 10_000, 50_000 }; // Low, Med, High Concurrency
        int[] dataLevels = { 1_000, 10_000, 100_000 }; // Small, Med, Large Data
        String[] reqLabels = { "Lo-Conc", "Med-Conc", "Hi-Conc" };
        String[] dataLabels = { "Sm-Data", "Med-Data", "Lg-Data" };

        List<Scenario> scenarios = new ArrayList<>();
        for (int d = 0; d < 3; d++) {
            for (int c = 0; c < 3; c++) {
                String name = String.format("%s/%s", reqLabels[c], dataLabels[d]);
                // Standard Pool of 100 for all tests to isolate Concurrency vs Data
                scenarios.add(new Scenario(name, reqLevels[c], dataLevels[d], 100));
            }
        }

        // Warmup
        System.out.print("Warming up...");
        for (int i = 0; i < 3; i++) {
            runIteration(Executors.newFixedThreadPool(10), () -> consume(calculateScalar(DATA_A, DATA_B, 1000)), 100);
            runIteration(Executors.newVirtualThreadPerTaskExecutor(),
                    () -> consume(calculateVector(DATA_A, DATA_B, 1000)), 100);
        }
        System.out.println(" Done.\n");

        // Print Header
        System.out.printf("%-18s | %-7s | %-7s | %-20s | %-20s | %-8s%n",
                "Scenario", "Reqs", "Data", "Legacy (req/s)", "Modern (req/s)", "Speedup");
        System.out.println("-".repeat(100));

        for (Scenario s : scenarios) {
            Stats leg = measure(s, true);
            Stats mod = measure(s, false);

            double speedup = mod.mean / leg.mean;

            System.out.printf("%-18s | %-7d | %-7d | %,10.0f ±%,9.0f | %,10.0f ±%,9.0f | %6.2fx%n",
                    s.name, s.numRequests, s.dataSize,
                    leg.mean, leg.stdDev,
                    mod.mean, mod.stdDev,
                    speedup);

            System.gc();
            Thread.sleep(50);
        }
    }

    static void consume(float result) {
        BLACKHOLE = result; // Volatile write prevents optimizing away calculation
    }

    static Stats measure(Scenario s, boolean legacy) {
        List<Double> results = new ArrayList<>();
        for (int i = 0; i < ITERATIONS; i++) {
            ExecutorService exec = legacy ? Executors.newFixedThreadPool(s.poolSize)
                    : Executors.newVirtualThreadPerTaskExecutor();
            Runnable task = legacy
                    ? () -> consume(calculateScalar(DATA_A, DATA_B, s.dataSize))
                    : () -> consume(calculateVector(DATA_A, DATA_B, s.dataSize));

            results.add(runIteration(exec, task, s.numRequests));
        }
        return new Stats(results);
    }

    static double runIteration(ExecutorService executor, Runnable task, int requests) {
        long start = System.nanoTime();
        try (executor) {
            for (int i = 0; i < requests; i++) {
                executor.submit(task);
            }
        }
        long ns = System.nanoTime() - start;
        return requests / (ns / 1_000_000_000.0);
    }

    static float calculateScalar(float[] a, float[] b, int length) {
        float sum = 0.0f;
        for (int i = 0; i < length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    static float calculateVector(float[] a, float[] b, int length) {
        var sumVector = FloatVector.zero(SPECIES);
        int upperBound = SPECIES.loopBound(length);
        int i = 0;
        for (; i < upperBound; i += SPECIES.length()) {
            var va = FloatVector.fromArray(SPECIES, a, i);
            var vb = FloatVector.fromArray(SPECIES, b, i);
            sumVector = sumVector.add(va.mul(vb));
        }
        float sum = sumVector.reduceLanes(jdk.incubator.vector.VectorOperators.ADD);
        for (; i < length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }
}