package module3;

import java.util.concurrent.StructuredTaskScope;

public class ScopedValuesDemo {

    // 1. Define the Scoped Value
    // It is 'static final' because the key itself is global, but the VALUE is
    // per-thread/per-scope.
    public static final ScopedValue<String> CURRENT_USER = ScopedValue.newInstance();

    public static void main(String[] args) {
        System.out.println("=== Scoped Values Demo ===");

        // 2. Bind a value to the scope
        // "Run this runnable, and while it's running, CURRENT_USER is 'Admin'"
        ScopedValue.where(CURRENT_USER, "Admin")
                .run(() -> handleRequest());

        // Prove it's gone
        System.out.println("[Main] Outside scope, is bound? " + CURRENT_USER.isBound());
    }

    // A top-level controller
    static void handleRequest() {
        System.out.println("[Controller] Handling request...");
        // Call a service, which calls a repository... (Deep stack)
        // Temporarily switch user for a specific internal task
        ScopedValue.where(CURRENT_USER, "Internal")
                .run(() -> serviceLayer());

        // Prove it's gone
        System.out.println("[Controller] Outside scope, is bound? " + CURRENT_USER.get());
    }

    static void serviceLayer() {
        repositoryLayer();
    }

    // Deep down in the stack
    static void repositoryLayer() {
        // 3. Access the value
        // No parameters were passed here!
        if (CURRENT_USER.isBound()) {
            String user = CURRENT_USER.get();
            System.out.println("[Repository] DB Query performed by: " + user);
        } else {
            System.out.println("[Repository] No user logged in.");
        }
    }
}
