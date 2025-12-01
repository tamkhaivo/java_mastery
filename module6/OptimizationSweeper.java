package module6;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.DoubleSummaryStatistics;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OptimizationSweeper {

    static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
    static final int MAX_DATA_SIZE = 200_000;
    static float[] DATA_A = new float[MAX_DATA_SIZE];
    static float[] DATA_B = new float[MAX_DATA_SIZE];
    static volatile float BLACKHOLE = 0.0f;
    static final int ITERATIONS = 3; // Reduced slightly because latency tracking adds overhead

    record StatResult(double mean, double stdDev) {
        public String toString(NumberFormat nf) {
            return String.format("%s ±%s", nf.format(mean), nf.format(stdDev));
        }
    }

    record RunMetrics(double reqPerSec, double p99LatencyUs) {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Hardware Optimization Sweeper (Throughput + Latency) ===");
        System.out.println("Vector Width: " + SPECIES.length() + " floats");

        // Setup Data
        Random r = new Random();
        for (int i = 0; i < MAX_DATA_SIZE; i++) {
            DATA_A[i] = r.nextFloat();
            DATA_B[i] = r.nextFloat();
        }

        int[] concurrencySteps = { 1_000, 10_000, 100_000, 200_000 }; // Kept smaller for latency tracking
        int[] dataSteps = { 1_000, 100_000 };

        // Warmup
        System.out.print("Warming up JVM...");
        runBurst(1000, 1000, true);
        runBurst(1000, 1000, false);
        System.out.println(" Done.\n");

        NumberFormat nfInt = NumberFormat.getNumberInstance(Locale.US);
        nfInt.setMaximumFractionDigits(0);

        NumberFormat nfLat = NumberFormat.getNumberInstance(Locale.US); // For Latency (microseconds)
        nfLat.setMaximumFractionDigits(0);

        // Header
        System.out.printf("%-7s | %-7s | %-20s | %-20s | %-16s | %-16s%n",
                "Threads", "Data", "Leg(Req/s)", "Mod(Req/s)", "Leg P99(us)", "Mod P99(us)");
        System.out.println("-".repeat(105));

        for (int dataSize : dataSteps) {
            for (int concurrency : concurrencySteps) {

                // Measure Legacy
                StatResult legRps = measureThroughput(concurrency, dataSize, true);
                StatResult legLat = measureLatency(concurrency, dataSize, true);

                // Measure Modern
                StatResult modRps = measureThroughput(concurrency, dataSize, false);
                StatResult modLat = measureLatency(concurrency, dataSize, false);

                System.out.printf("%-7d | %-7d | %-20s | %-20s | %-16s | %-16s%n",
                        concurrency, dataSize,
                        legRps.toString(nfInt), modRps.toString(nfInt),
                        legLat.toString(nfLat), modLat.toString(nfLat));

                System.gc();
                Thread.sleep(100);
            }
            System.out.println("-".repeat(105));
        }
    }

    static StatResult measureThroughput(int concurrency, int dataSize, boolean isLegacy) {
        List<Double> samples = new ArrayList<>();
        for (int i = 0; i < ITERATIONS; i++) {
            samples.add(runBurst(concurrency, dataSize, isLegacy).reqPerSec);
        }
        return calculateStats(samples);
    }

    static StatResult measureLatency(int concurrency, int dataSize, boolean isLegacy) {
        List<Double> samples = new ArrayList<>();
        for (int i = 0; i < ITERATIONS; i++) {
            samples.add(runBurst(concurrency, dataSize, isLegacy).p99LatencyUs);
        }
        return calculateStats(samples);
    }

    static StatResult calculateStats(List<Double> vals) {
        DoubleSummaryStatistics ss = vals.stream().mapToDouble(d -> d).summaryStatistics();
        double mean = ss.getAverage();
        double var = vals.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / vals.size();
        return new StatResult(mean, Math.sqrt(var));
    }

    static RunMetrics runBurst(int concurrency, int dataSize, boolean isLegacy) {
        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        long start = System.nanoTime();

        ExecutorService executor = isLegacy
                ? Executors.newFixedThreadPool(100)
                : Executors.newVirtualThreadPerTaskExecutor();

        try (executor) {
            for (int i = 0; i < concurrency; i++) {
                executor.submit(() -> {
                    long t0 = System.nanoTime();
                    if (isLegacy)
                        consume(calculateScalar(DATA_A, DATA_B, dataSize));
                    else
                        consume(calculateVector(DATA_A, DATA_B, dataSize));
                    latencies.add((System.nanoTime() - t0) / 1000); // Microseconds
                });
            }
        }
        long durationNs = System.nanoTime() - start;
        double rps = concurrency / (durationNs / 1_000_000_000.0);

        // Calculate P99
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        double p99 = sorted.get((int) (sorted.size() * 0.99));

        return new RunMetrics(rps, p99);
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
