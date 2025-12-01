package module6;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class OptimizationSweeper {

    static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
    static final int MAX_DATA_SIZE = 200_000;
    static float[] DATA_A = new float[MAX_DATA_SIZE];
    static float[] DATA_B = new float[MAX_DATA_SIZE];
    static volatile float BLACKHOLE = 0.0f;

    record Result(int concurrency, int dataSize,
            double legacyReqPerSec, double modernReqPerSec,
            double legacyGigaOps, double modernGigaOps,
            double speedup) {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Hardware Optimization Sweeper (Legacy vs Modern) ===");
        System.out.println("Mapping Throughput (Req/s) and Compute Power (GigaOps/s)...");
        System.out.println("Vector Width: " + SPECIES.length() + " floats");

        // Setup Data
        Random r = new Random();
        for (int i = 0; i < MAX_DATA_SIZE; i++) {
            DATA_A[i] = r.nextFloat();
            DATA_B[i] = r.nextFloat();
        }

        int[] concurrencySteps = { 1_000, 10_000, 50_000, 100_000, 200_000, 500_000 };
        int[] dataSteps = { 100, 10_000, 100_000, 200_000, 500_000 };

        // Warmup
        System.out.print("Warming up JVM...");
        runBurst(5000, 1000, true); // Legacy warmup
        runBurst(5000, 1000, false); // Modern warmup
        System.out.println(" Done.\n");

        List<Result> results = new ArrayList<>();

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(0);

        // Table Header
        System.out.printf("%-8s | %-8s | %-13s | %-13s | %-13s | %-13s | %-7s%n",
                "Threads", "Data", "Leg(Req/s)", "Mod(Req/s)", "Leg(GigaOps)", "Mod(GigaOps)", "Gain");
        System.out.println("-".repeat(90));

        Result bestCompute = new Result(0, 0, 0, 0, 0, 0, 0);

        for (int dataSize : dataSteps) {
            for (int concurrency : concurrencySteps) {

                // 1. Run Legacy
                double legacyRps = runBurst(concurrency, dataSize, true);
                double legacyOps = calculateGigaOps(legacyRps, dataSize);
                System.gc();
                Thread.sleep(50);

                // 2. Run Modern
                double modernRps = runBurst(concurrency, dataSize, false);
                double modernOps = calculateGigaOps(modernRps, dataSize);
                System.gc();
                Thread.sleep(50);

                double speedup = legacyRps > 0 ? modernRps / legacyRps : 0;

                Result res = new Result(concurrency, dataSize, legacyRps, modernRps, legacyOps, modernOps, speedup);
                results.add(res);

                if (res.modernGigaOps > bestCompute.modernGigaOps)
                    bestCompute = res;

                System.out.printf("% -8d | %-8d | %13s | %13s | %13.2f | %13.2f | %6.2fx%n",
                        concurrency, dataSize,
                        nf.format(legacyRps), nf.format(modernRps),
                        legacyOps, modernOps,
                        speedup);
            }
            System.out.println("-".repeat(90));
        }

        System.out.println("\n=== POWER ANALYSIS ===");
        System.out.printf("Peak Compute Power: %.2f GigaOps/s\n", bestCompute.modernGigaOps);
        System.out.printf("Achieved at: %d Concurrent Requests with %d Data Size\n", bestCompute.concurrency,
                bestCompute.dataSize);

        System.out.println("\nInterpretation:");
        System.out.println("1. GigaOps = Billions of Math Operations per Second.");
        System.out.println("2. If 'Mod(GigaOps)' is higher, the Vector API is effectively using SIMD instructions.");
        System.out.println("3. If 'Leg(GigaOps)' flatlines, the CPU cores are saturated or threads are blocking.");
    }

    static double calculateGigaOps(double reqPerSec, int dataSize) {
        // 2 FLOPs per data point (Multiply + Add)
        return (reqPerSec * dataSize * 2) / 1_000_000_000.0;
    }

    static double runBurst(int concurrency, int dataSize, boolean isLegacy) {
        long start = System.nanoTime();

        ExecutorService executor = isLegacy
                ? Executors.newFixedThreadPool(100)
                : Executors.newVirtualThreadPerTaskExecutor();

        try (executor) {
            for (int i = 0; i < concurrency; i++) {
                if (isLegacy) {
                    executor.submit(() -> consume(calculateScalar(DATA_A, DATA_B, dataSize)));
                } else {
                    executor.submit(() -> consume(calculateVector(DATA_A, DATA_B, dataSize)));
                }
            }
        }
        long ns = System.nanoTime() - start;
        return concurrency / (ns / 1_000_000_000.0);
    }

    static void consume(float result) {
        BLACKHOLE = result;
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
