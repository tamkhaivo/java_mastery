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

public class GCOptimizationSweeper {

    static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
    static final int MAX_DATA_SIZE = 200_000;
    static float[] DATA_A = new float[MAX_DATA_SIZE];
    static float[] DATA_B = new float[MAX_DATA_SIZE];
    static volatile float BLACKHOLE = 0.0f;
    static final int ITERATIONS = 5; // Increased for confidence

    // GC STRESS FACTOR: 10KB allocation per request
    static final int ALLOCATION_SIZE = 1024 * 10;

    record StatResult(double mean, double stdDev) {
        public String toString(NumberFormat nf) {
            return String.format("%s ±%s", nf.format(mean), nf.format(stdDev));
        }
    }

    record RunMetrics(double reqPerSec, double p99LatencyUs, double gigaOps, double peakMemoryMb) {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== GC Optimization Sweeper (Allocating 10KB/req) ===");
        String gcName = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
                .get(0).getName();
        System.out.println("Active Garbage Collector: " + gcName);
        System.out.println("Vector Width: " + SPECIES.length() + " floats");

        // Setup Data
        Random r = new Random();
        for (int i = 0; i < MAX_DATA_SIZE; i++) {
            DATA_A[i] = r.nextFloat();
            DATA_B[i] = r.nextFloat();
        }

        int[] concurrencySteps = { 10_000, 50_000 };
        int[] dataSteps = { 1_000 };

        // Warmup
        System.out.print("Warming up JVM...");
        runBurst(1000, 1000, true);
        runBurst(1000, 1000, false);
        System.out.println(" Done.\n");

        NumberFormat nfInt = NumberFormat.getNumberInstance(Locale.US);
        nfInt.setMaximumFractionDigits(0);

        NumberFormat nfLat = NumberFormat.getNumberInstance(Locale.US);
        nfLat.setMaximumFractionDigits(0);

        NumberFormat nfOps = NumberFormat.getNumberInstance(Locale.US);
        nfOps.setMaximumFractionDigits(1);

        NumberFormat nfMem = NumberFormat.getNumberInstance(Locale.US);
        nfMem.setMaximumFractionDigits(1);

        System.out.printf("%-7s | %-18s | %-18s | %-18s | %-18s | %-14s | %-14s | %-14s | %-14s%n",
                "Threads", "Leg(Req/s)", "Mod(Req/s)", "Leg(GOps/s)", "Mod(GOps/s)", "Leg P99(us)", "Mod P99(us)",
                "Leg Mem(MB)", "Mod Mem(MB)");
        System.out.println("-".repeat(160));

        for (int dataSize : dataSteps) {
            for (int concurrency : concurrencySteps) {

                // Measure Legacy (Traditional Threads)
                List<RunMetrics> legRuns = measure(concurrency, dataSize, true);
                StatResult legRps = calcStats(legRuns, RunMetrics::reqPerSec);
                StatResult legLat = calcStats(legRuns, RunMetrics::p99LatencyUs);
                StatResult legOps = calcStats(legRuns, RunMetrics::gigaOps);
                StatResult legMem = calcStats(legRuns, RunMetrics::peakMemoryMb);

                // Measure Modern (Virtual Threads)
                List<RunMetrics> modRuns = measure(concurrency, dataSize, false);
                StatResult modRps = calcStats(modRuns, RunMetrics::reqPerSec);
                StatResult modLat = calcStats(modRuns, RunMetrics::p99LatencyUs);
                StatResult modOps = calcStats(modRuns, RunMetrics::gigaOps);
                StatResult modMem = calcStats(modRuns, RunMetrics::peakMemoryMb);

                System.out.printf("%-7d | %-18s | %-18s | %-18s | %-18s | %-14s | %-14s | %-14s | %-14s%n",
                        concurrency,
                        legRps.toString(nfInt), modRps.toString(nfInt),
                        legOps.toString(nfOps), modOps.toString(nfOps),
                        legLat.toString(nfLat), modLat.toString(nfLat),
                        legMem.toString(nfMem), modMem.toString(nfMem));

                System.gc();
                Thread.sleep(200);
            }
        }
    }

    static List<RunMetrics> measure(int concurrency, int dataSize, boolean isLegacy) {
        List<RunMetrics> results = new ArrayList<>();
        for (int i = 0; i < ITERATIONS; i++) {
            results.add(runBurst(concurrency, dataSize, isLegacy));
        }
        return results;
    }

    static StatResult calcStats(List<RunMetrics> runs, java.util.function.ToDoubleFunction<RunMetrics> mapper) {
        DoubleSummaryStatistics ss = runs.stream().mapToDouble(mapper).summaryStatistics();
        double mean = ss.getAverage();
        double var = runs.stream().mapToDouble(r -> Math.pow(mapper.applyAsDouble(r) - mean, 2)).sum() / runs.size();
        return new StatResult(mean, Math.sqrt(var));
    }

    static RunMetrics runBurst(int concurrency, int dataSize, boolean isLegacy) {
        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();

        // Start Memory Tracker
        MemoryTracker memoryTracker = new MemoryTracker();
        memoryTracker.start();

        long start = System.nanoTime();

        ExecutorService executor = isLegacy
                ? Executors.newFixedThreadPool(100)
                : Executors.newVirtualThreadPerTaskExecutor();

        try (executor) {
            for (int i = 0; i < concurrency; i++) {
                executor.submit(() -> {
                    long t0 = System.nanoTime();
                    float res;
                    if (isLegacy)
                        res = calculateScalar(DATA_A, DATA_B, dataSize);
                    else
                        res = calculateVector(DATA_A, DATA_B, dataSize);

                    // MEMORY ALLOCATION (Garbage Creation)
                    byte[] garbage = new byte[ALLOCATION_SIZE];
                    consume(res + garbage[0]);

                    latencies.add((System.nanoTime() - t0) / 1000);
                });
            }
        }
        long durationNs = System.nanoTime() - start;

        // Stop Memory Tracker
        memoryTracker.stopTracking();
        double peakMemoryMb = memoryTracker.getPeakMemoryMb();

        double rps = concurrency / (durationNs / 1_000_000_000.0);

        // Calculate GigaOps: (RPS * DataSize * 2 ops/float) / 1e9
        // 2 ops/float comes from 1 multiply + 1 add per element
        double gigaOps = (rps * dataSize * 2) / 1_000_000_000.0;

        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        double p99 = sorted.isEmpty() ? 0 : sorted.get((int) (sorted.size() * 0.99));

        return new RunMetrics(rps, p99, gigaOps, peakMemoryMb);
    }

    static class MemoryTracker extends Thread {
        private volatile boolean running = true;
        private volatile long peakMemoryBytes = 0;

        public void run() {
            while (running) {
                long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                if (used > peakMemoryBytes) {
                    peakMemoryBytes = used;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public void stopTracking() {
            running = false;
            try {
                join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public double getPeakMemoryMb() {
            return peakMemoryBytes / (1024.0 * 1024.0);
        }
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