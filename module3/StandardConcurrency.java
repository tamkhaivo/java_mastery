package module3;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CancellationException; // Import for clarity

public class StandardConcurrency {
    public static void main(String[] args) {
        System.out.println("=== Standard (Stable) Structured Concurrency ===");
        long start = System.currentTimeMillis();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println("[Main] Forking tasks...");

            Future<String> userTask = executor.submit(() -> fetchUser());
            Future<Integer> ordersTask = executor.submit(() -> fetchOrders());

            String user = null;
            Integer orders = null;

            try {
                // Attempt to get user profile. If this fails, an ExecutionException is thrown.
                user = userTask.get();
                // If user is successful, then attempt to get orders.
                orders = ordersTask.get();

                System.out.println("\n[Success] Dashboard for " + user + " has " + orders + " orders.");

            } catch (ExecutionException e) {
                // One of the tasks failed.
                System.err.println("\n[Main] Task failed: " + e.getCause().getMessage());
                // IMPORTANT: In stable ExecutorService, the other task is NOT automatically
                // cancelled.
                // We must manually cancel it if we don't want to wait for it.
                if (!userTask.isDone()) { // Check if it's still running or completed normally
                    userTask.cancel(true); // Attempt to interrupt if running, or just mark as cancelled.
                    System.err.println("[Main] User task cancelled.");
                }
                if (!ordersTask.isDone()) { // Check if it's still running or completed normally
                    ordersTask.cancel(true); // Attempt to interrupt if running, or just mark as cancelled.
                    System.err.println("[Main] Orders task cancelled.");
                }
                System.err.println("\n[Failure] Could not build complete dashboard due to an error.");

            } catch (InterruptedException e) {
                System.err.println("[Main] Main thread interrupted: " + e.getMessage());
            } catch (CancellationException e) {
                System.err.println("[Main] A task was cancelled: " + e.getMessage());
            }

        } // executor.close() will wait for any remaining tasks, or handle already
          // cancelled ones.

        long end = System.currentTimeMillis();
        System.out.println("Total Time: " + (end - start) + "ms");
    }

    static String fetchUser() throws InterruptedException {
        Thread.sleep(Duration.ofMillis(100));
        // Simulate failure here
        System.out.println("[User] Fetching user profile (and failing)...");
        throw new RuntimeException("Database Down for User Profile!");
        // return "Alice"; // Uncomment this line to make it succeed
    }

    static Integer fetchOrders() throws InterruptedException {
        System.out.println("[Orders] Fetching orders...");
        Thread.sleep(Duration.ofMillis(200));
        return 42;
    }
}
