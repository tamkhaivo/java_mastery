package module8.behavioral.solution2;

public class CreditCardStrategy implements PaymentStrategy {
    @Override
    public void pay(Order order) {
        System.out.println("Processing Credit Card for Amount: " + order.getAmount());
        if (order.getAmount() > 1000) {
            System.out.println("...Performing extra fraud checks for Credit Card");
        }
        System.out.println("Credit Card Charged.");
    }
}
