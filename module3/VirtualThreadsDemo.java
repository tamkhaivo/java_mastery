package module3;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class VirtualThreadsDemo {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // Launch 10,000 virtual threads
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 10_000).forEach(i -> {
                executor.submit(() -> {
                    try {
                        // Simulate some work/blocking
                        Thread.sleep(Duration.ofMillis(10));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            });
        } // Executor automatically closes and waits for tasks here

        long end = System.currentTimeMillis();
        System.out.println("Finished 10,000 threads in " + (end - start) + " ms");
    }
}
