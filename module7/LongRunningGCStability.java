package module7;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;

/**
 * Module 7: Advanced GC Optimizations - Stability Demo
 * 
 * This program simulates a long-running system under constant load to
 * demonstrate
 * the behavior of Latency, Throughput, and Memory Footprint.
 * 
 * Concepts:
 * 1. Stability: The system should maintain predictable performance metrics over
 * time.
 * 2. Memory Footprint: The heap usage should oscillate (sawtooth) but not leak.
 * 3. Latency: GC pauses will manifest as latency spikes.
 */
public class LongRunningGCStability {

    // Configuration
    private static int DURATION_SECONDS = 60 * 10; // Default 10 minutes
    private static String CSV_FILE = null;
    private static String GC_MODE = "Unknown";
    private static final int THREAD_COUNT = 1000;
    private static final int ALLOCATION_SIZE_BYTES = 1024 * 1024 * 1; // 1MB per request
    private static final int CPU_WORK_LOOPS = 1000;

    // Metrics
    private static final AtomicLong totalRequests = new AtomicLong(0);
    private static final ConcurrentLinkedQueue<Long> latencySamples = new ConcurrentLinkedQueue<>();
    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0) {
            try {
                DURATION_SECONDS = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid duration argument. Using default.");
            }
        }
        if (args.length > 1) {
            CSV_FILE = args[1];
        }
        if (args.length > 2) {
            GC_MODE = args[2];
        }
        System.out.println("=== Module 7: Long Running GC Stability Demo ===");
        printGCDetails();
        System.out.printf("Configuration: %d Threads, %d MB Alloc/Req, %d Seconds, Mode: %s%n",
                THREAD_COUNT, ALLOCATION_SIZE_BYTES / (1024 * 1024), DURATION_SECONDS, GC_MODE);
        if (CSV_FILE != null) {
            System.out.println("Exporting metrics to: " + CSV_FILE);
        }
        System.out.println("Starting workload...");
        System.out.println("-".repeat(100));
        System.out.printf("%-8s | %-15s | %-15s | %-15s | %-15s%n",
                "Time(s)", "Throughput(req/s)", "Latency P99(ms)", "Heap Used(MB)", "GC Time(ms)");
        System.out.println("-".repeat(100));

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        // Start Reporter Thread
        Thread reporter = new Thread(LongRunningGCStability::runReporter);
        reporter.setDaemon(true);
        reporter.start();

        // Start Workload
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                while (running) {
                    performTask();
                }
            });
        }

        // Run for duration
        Thread.sleep(DURATION_SECONDS * 1000L);
        running = false;

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("-".repeat(100));
        System.out.println("Test Complete.");
    }

    private static void performTask() {
        long start = System.nanoTime();

        // 1. Simulate CPU Work
        double result = 0;
        for (int i = 0; i < CPU_WORK_LOOPS; i++) {
            result += Math.sin(i) * Math.cos(i);
        }

        // 2. Simulate Memory Allocation (Short-lived objects)
        byte[] data = new byte[ALLOCATION_SIZE_BYTES];
        data[0] = (byte) result; // Prevent optimization

        // 3. Record Metrics
        long latencyNs = System.nanoTime() - start;
        latencySamples.add(latencyNs);
        totalRequests.incrementAndGet();

        // Small sleep to prevent overwhelming the system completely if CPU work is too
        // fast
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runReporter() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        long lastRequests = 0;
        long startTime = System.currentTimeMillis();
        long nextTarget = startTime + 1000;
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(2);

        PrintWriter writer = null;
        try {
            if (CSV_FILE != null) {
                File f = new File(CSV_FILE);
                boolean writeHeader = !f.exists() || f.length() == 0;
                writer = new PrintWriter(new FileWriter(f, true));
                if (writeHeader) {
                    writer.println("Time,Throughput,LatencyP99,HeapUsed,GCTime,Mode");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (PrintWriter csvWriter = writer) {
            while (running) {
                long now = System.currentTimeMillis();
                long delay = nextTarget - now;
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                nextTarget += 1000;

                long currentTime = System.currentTimeMillis();
                long elapsedSeconds = (currentTime - startTime) / 1000;

                // Throughput
                long currentRequests = totalRequests.get();
                long requestsInInterval = currentRequests - lastRequests;
                lastRequests = currentRequests;

                // Latency P99
                List<Long> snapshot = new ArrayList<>();
                Long sample;
                while ((sample = latencySamples.poll()) != null) {
                    snapshot.add(sample);
                }
                Collections.sort(snapshot);
                double p99Ms = 0;
                if (!snapshot.isEmpty()) {
                    int index = (int) (snapshot.size() * 0.99);
                    p99Ms = snapshot.get(index) / 1_000_000.0;
                }

                // Memory
                long heapUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
                double heapUsedMb = heapUsed / (1024.0 * 1024.0);

                // GC Time (Cumulative)
                long gcTime = 0;
                for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
                    gcTime += gcBean.getCollectionTime();
                }

                System.out.printf("%-8d | %-15d | %-15s | %-15s | %-15d%n",
                        elapsedSeconds, requestsInInterval, nf.format(p99Ms), nf.format(heapUsedMb), gcTime);

                if (csvWriter != null) {
                    csvWriter.printf(Locale.US, "%d,%d,%.2f,%.2f,%d,%s%n", elapsedSeconds, requestsInInterval, p99Ms,
                            heapUsedMb, gcTime, GC_MODE);
                    csvWriter.flush();
                }
            }
        }
    }

    private static void printGCDetails() {
        System.out.println("Detected Garbage Collectors:");
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.println(" - " + gcBean.getName());
        }
    }
}
