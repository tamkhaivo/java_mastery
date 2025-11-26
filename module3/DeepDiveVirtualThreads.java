package module3;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class DeepDiveVirtualThreads {
    public static void main(String[] args) throws Exception {
        // A thread-safe set to capture the unique names of the actual OS threads
        // (Carriers)
        Set<String> carrierThreads = ConcurrentHashMap.newKeySet();

        System.out.println("=== M:N Model Demonstration ===");
        System.out.println("1. Launching 100 Virtual Threads...");
        System.out.println("2. Each thread will capture its 'Carrier' name and then sleep.");

        // We use the try-with-resources block to auto-close the executor
        // This line ensures all 100 threads finish before we proceed
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 1000).forEach(i -> {
                executor.submit(() -> {
                    // A. CAPTURE THE CARRIER
                    // VirtualThread.toString() format:
                    // "VirtualThread[#21]/runnable@ForkJoinPool-1-worker-1"
                    String threadInfo = Thread.currentThread().toString();
                    String carrier = extractCarrier(threadInfo);

                    carrierThreads.add(carrier);

                    try {
                        // B. UNMOUNT (Simulate I/O)
                        // When we sleep here, the OS thread (ForkJoinPool-worker) does NOT sleep.
                        // It dumps this virtual thread on the heap and goes to find another virtual
                        // thread to run.
                        Thread.sleep(Duration.ofMillis(50));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            });
        }

        System.out.println("\n=== The Result (The 'N' in M:N) ===");
        System.out.println("Total Virtual Threads (M): 100");
        System.out.println("Unique OS Carriers used (N): " + carrierThreads.size());
        System.out.println("\nCarrier Names:");
        carrierThreads.forEach(name -> System.out.println(" -> " + name));

        System.out.println("\n[Success] Proof that 100 tasks shared a small pool of OS threads.");
    }

    private static String extractCarrier(String threadString) {
        // We look for the '@' symbol which denotes the mounting point
        int atIndex = threadString.lastIndexOf("@");
        if (atIndex != -1) {
            return threadString.substring(atIndex + 1);
        }
        return "Unknown-Carrier (String format might have changed)";
    }
}
