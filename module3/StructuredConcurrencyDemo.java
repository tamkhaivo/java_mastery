package module3;

import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class StructuredConcurrencyDemo {

    public static void main(String[] args) {
        System.out.println("=== Structured Concurrency Dashboard ===");
        long start = System.currentTimeMillis();

        try {
            Dashboard dashboard = buildDashboard("User123");
            System.out.println("\nSuccess!");
            System.out.println(dashboard);
        } catch (Exception e) {
            System.err.println("\nFailed to build dashboard: " + e.getMessage());
        }

        System.out.println("Total Time: " + (System.currentTimeMillis() - start) + "ms");
    }

    record UserProfile(String name, String email) {
    }

    record UserOrders(int orderCount, String lastOrder) {
    }

    record Dashboard(UserProfile profile, UserOrders orders) {
    }

    public static Dashboard buildDashboard(String userId) throws ExecutionException, InterruptedException {
        // The "Scope" is the boundary. All threads start here and MUST end here.
        // Joiner.awaitAllSuccessfulOrThrow means: "Wait for all to succeed, or throw if
        // ANY fails."
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<Object>awaitAllSuccessfulOrThrow())) {

            System.out.println("[Main] Forking tasks...");

            // 1. Fork task: Fetch Profile (Takes 100ms)
            StructuredTaskScope.Subtask<UserProfile> profileTask = scope.fork(() -> {
                Thread.sleep(Duration.ofMillis(100));
                // Simulate random failure
                // if (true) throw new RuntimeException("Database Down!");
                return new UserProfile("Alice", "alice@example.com");
            });

            // 2. Fork task: Fetch Orders (Takes 200ms)
            StructuredTaskScope.Subtask<UserOrders> ordersTask = scope.fork(() -> {
                Thread.sleep(Duration.ofMillis(200));
                return new UserOrders(5, "Order#999");
            });

            // 3. The Barrier: Wait for ALL to finish (or one to fail)
            // In the new API, join() waits and throws if the policy (Joiner) dictates it.
            scope.join();

            // 4. Construct the result
            // We can safely call get() because join() would have thrown if there was a
            // failure.
            return new Dashboard(profileTask.get(), ordersTask.get());
        } catch (StructuredTaskScope.FailedException e) {
            // Unwrap the original exception to maintain behavior
            throw new ExecutionException(e.getCause());
        }
        // When this brace } is reached, the JVM guarantees ALL threads are done.
    }
}
