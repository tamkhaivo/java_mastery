package module8.behavioral.problem2;

public class Main {
    public static void main(String[] args) {
        Order order = new Order("ORD-123", 1500.00);
        PaymentProcessor processor = new PaymentProcessor();

        System.out.println("--- Payment Scenario 1 ---");
        processor.processPayment(order, "CREDIT_CARD");

        System.out.println("\n--- Payment Scenario 2 ---");
        processor.processPayment(order, "PAYPAL");

        System.out.println("\n--- Payment Scenario 3 ---");
        try {
            processor.processPayment(order, "APPLE_PAY");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("(This demonstrates the need to modify PaymentProcessor to support Apple Pay)");
        }
    }
}
