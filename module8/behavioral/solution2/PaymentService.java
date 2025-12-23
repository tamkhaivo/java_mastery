package module8.behavioral.solution2;

import java.util.HashMap;
import java.util.Map;

public class PaymentService {
    // Map to hold strategies. Key is the payment method name (e.g., "CREDIT_CARD")
    private final Map<String, PaymentStrategy> strategies = new HashMap<>();

    public PaymentService() {
        // Register default strategies
        // We can register them here, or letting the Main/Config register them.
        // For this demo, let's pre-register common ones to simulate a "standard" setup,
        // but the key feature is that we can add MORE later.
        register("CREDIT_CARD", new CreditCardStrategy());
        register("PAYPAL", new PayPalStrategy());
    }

    // OCP method: Allows extending behavior without modifying this class
    public void register(String method, PaymentStrategy strategy) {
        strategies.put(method, strategy);
    }

    public void process(Order order, String method) {
        PaymentStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown payment method: " + method);
        }
        strategy.pay(order);
    }
}
