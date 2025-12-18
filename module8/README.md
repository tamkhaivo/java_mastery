# Academic Plan: "From Rigidity to Extensibility"

Here is an academic plan and a problem set designed to demonstrate the impact of Java design patterns on software maintainability, specifically through the lens of the Open/Closed Principle (OCP).

## Course Objective
To master the refactoring of rigid, fragile Java code into maintainable, extensible systems using design patterns, with a strict focus on adhering to the Open/Closed Principle (software entities should be open for extension, but closed for modification).

## Module Structure

*   **Week 1: Creational Agility.** Decoupling object creation from business logic to allow new types without surgical code changes.
*   **Week 2: Behavioral Flexibility.** Replacing conditional logic (if/else chains) with polymorphic composition.
*   **Week 3: Structural Insulation.** Protecting the core domain from external changes and legacy systems.
*   **Week 4: Modern Extensibility.** Using Java 17+ features (Sealed Classes, ServiceLoader) to reimagine classical patterns.

---

# Problem Set: Maintainability & OCP Case Studies

For each problem, we compare the "Maintenance Nightmare" (Violation of OCP) with the "Pattern Solution" (Adherence to OCP).

## Problem 1: The "Notification Sprawl" (Creational)

**Scenario:** You are building a notification system. Currently, the `NotificationManager` class instantiates specific services directly based on a string input.

### A. The Maintenance Nightmare (OCP Violation)
The class uses a hard-coded decision tree. To add a new notification type (e.g., "Slack"), you must modify the tested `sendNotification` method, risking regressions in existing logic.

```java
public class NotificationManager {
    public void send(String type, String message) {
        if (type.equals("EMAIL")) {
            new EmailService().send(message); // Hard dependency
        } else if (type.equals("SMS")) {
            new SmsService().send(message);
        }
        // To add "SLACK", you must modify this file.
    }
}
```

### B. The Pattern Solution: Factory Method
**Goal:** Make the manager "Closed" for modification but "Open" for extension.

**Strategy:** Implement a `NotificationFactory`. The `NotificationManager` relies only on the `Notification` interface. To add "Slack", you create a `SlackNotification` class and register it with the factory (or add a case to the factory), leaving the manager logic untouched.

**Metric Improvement:** Extensibility. New types are added via new classes, not by changing existing control flow.

## Problem 2: The "Payment Switch from Hell" (Behavioral)

**Scenario:** A payment processing class contains a massive switch statement handling logic for Credit Cards, PayPal, and Crypto.

### A. The Maintenance Nightmare (OCP Violation)
Every time a new payment method is added or a fee calculation changes, the core `processPayment` method grows larger. This increases cyclomatic complexity and makes the class a "God Object".

```java
public void processPayment(Order order, String method) {
    if (method.equals("CREDIT_CARD")) {
        // 50 lines of validation logic
    } else if (method.equals("PAYPAL")) {
        // Different validation logic
    }
    // Adding "APPLE_PAY" requires invasive editing here.
}
```

### B. The Pattern Solution: Strategy Pattern (with Lambdas)
**Goal:** Encapsulate algorithms so they can be interchanged dynamically.

**Strategy:** Define a `PaymentStrategy` interface. Use a `Map<String, PaymentStrategy>` to look up the correct strategy at runtime. Modern Java allows these strategies to be concise lambdas.

```java
// OCP Adherence: logic is injected, not hardcoded.
Map<String, Consumer<Order>> strategies = new HashMap<>();
strategies.put("CREDIT_CARD", this::processCreditCard);
// Adding "APPLE_PAY" involves putting a new key-value pair, not editing the 'process' method.
```

## Problem 3: The "Legacy Vendor" Lock-in (Structural)

**Scenario:** Your analytics dashboard directly calls a 3rd-party library `OldChartLib`. The vendor is deprecating it in favor of `NewGraphEngine`, which has completely different method signatures.

### A. The Maintenance Nightmare (OCP Violation)
Because the dashboard is tightly coupled to `OldChartLib`, upgrading requires rewriting every line of code in the dashboard that calls the library. The code was not "Closed" to external changes.

### B. The Pattern Solution: Adapter Pattern
**Goal:** Isolate the core application from the volatility of external dependencies.

**Strategy:** Create an interface `ChartProvider` that your app owns. Create an adapter `OldChartAdapter` (and later `NewGraphAdapter`) that translates your interface calls to the vendor's specific API.

**Metric Improvement:** Stability. You can swap the underlying library by changing one line of configuration (the adapter injection), protecting the rest of the application.

## Problem 4: The "Shape Calculator" (Modern Java)

**Scenario:** You need to calculate the area for a set of shapes. You want to ensure the compiler forces you to handle new shapes if they are added.

### A. The Maintenance Nightmare (Visitor Pattern)
The classical Visitor pattern achieves OCP but requires immense boilerplate (double dispatch). Adding a new operation requires modifying every shape class to accept the visitor.

### B. The Pattern Solution: Sealed Classes & Pattern Matching
**Goal:** Data-oriented OCP with compiler safety.

**Strategy:** Use a sealed interface `Shape` which permits only specific implementations. Use Java 21 Pattern Matching for switch to handle logic.

```java
public double calculateArea(Shape shape) {
    return switch (shape) {
        case Circle c -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.width() * r.height();
        // Compiler Error if a new 'Triangle' is added to the sealed interface but missing here.
    };
}
```

**Comparison:** Unlike the Visitor pattern, which spreads logic across multiple classes, this approach centralizes behavior while maintaining type safety and ensuring all cases are handled.

## Problem 5: The "Plugin Architecture" (Advanced)

**Scenario:** You are building an IDE that needs to support user-contributed plugins without recompiling the main application.

### A. The Maintenance Nightmare (Hardcoded List)
The app has a static list: `List<Plugin> plugins = List.of(new GitPlugin(), new MavenPlugin());`. Adding a plugin requires recompiling the app.

### B. The Pattern Solution: ServiceLoader (SPI)
**Goal:** True modularity where the application doesn't know what implementations exist until runtime.

**Strategy:** Define a `Plugin` interface. Use `java.util.ServiceLoader` to discover implementations provided in external JARs (via `META-INF/services`).

**Metric Improvement:** Deployment Independence. New features can be "dropped in" as JAR files, fully realizing OCP at the architectural level.
