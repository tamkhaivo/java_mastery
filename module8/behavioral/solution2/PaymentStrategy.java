package module8.behavioral.solution2;

@FunctionalInterface
public interface PaymentStrategy {
    void pay(Order order);
}
