package module8.behavioral.solution2;

public class PayPalStrategy implements PaymentStrategy {
    @Override
    public void pay(Order order) {
        System.out.println("Redirecting to PayPal...");
        System.out.println("PayPal payment approved for Order: " + order.getId());
    }
}
