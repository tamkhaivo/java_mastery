package module8.behavioral.solution2;

public class Main {
    public static void main(String[] args) {
        Order order = new Order("ORD-123", 1500.00);
        PaymentService service = new PaymentService();

        System.out.println("--- Standard Payment Methods ---");
        service.process(order, "CREDIT_CARD");
        service.process(order, "PAYPAL");

        System.out.println("\n--- Extending with New Payment Method (OCP) ---");
        // We add Apple Pay WITHOUT modifying PaymentService or existing strategies
        service.register("APPLE_PAY", (o) -> {
            System.out.println("Authenticating with Face ID...");
            System.out.println("Apple Pay transaction complete for " + o.getId());
        });

        service.process(order, "APPLE_PAY");
    }
}
