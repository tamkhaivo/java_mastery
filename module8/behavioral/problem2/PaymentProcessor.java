package module8.behavioral.problem2;

public class PaymentProcessor {

    public void processPayment(Order order, String method) {
        if (method.equals("CREDIT_CARD")) {
            System.out.println("Processing Credit Card for Amount: " + order.getAmount());
            // Simulate complex validation logic
            if (order.getAmount() > 1000) {
                System.out.println("...Performing extra fraud checks for Credit Card");
            }
            System.out.println("Credit Card Charged.");

        } else if (method.equals("PAYPAL")) {
            System.out.println("Redirecting to PayPal...");
            // Simulate different logic
            System.out.println("PayPal payment approved for Order: " + order.getId());

        } else if (method.equals("CRYPTO")) {
            System.out.println("Connecting to Wallet...");
            System.out.println("Crypto transaction mining...");
        } else {
            throw new IllegalArgumentException("Unknown payment method: " + method);
        }

        // PROBLEM: To add "APPLE_PAY", you must modify this class, violating OCP.
    }
}
