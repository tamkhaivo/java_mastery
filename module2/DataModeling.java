package module2;

// 1. RECORDS: Pure data carriers
// Compiler generates: constructor, accessors, equals, hashCode, toString
record User(String username, int id) {
    // Validation using a "Compact Constructor"
    public User {
        if (id < 0)
            throw new IllegalArgumentException("ID cannot be negative");
    }
}

// 2. SEALED HIERARCHY: Strictly controlled inheritance
// Only Payment, Refund, and Dispute can implement TransactionStatus.
// No other class in the universe can implement this interface.
sealed interface TransactionStatus permits Payment, Refund, Dispute {
}

// 'final' means the hierarchy ends here.
final class Payment implements TransactionStatus {
    @Override
    public String toString() {
        return "Payment Processed";
    }
}

// Records can implement sealed interfaces too.
record Refund(double amount) implements TransactionStatus {
}

// 'non-sealed' opens the hierarchy back up for arbitrary extension.
non-sealed class Dispute implements TransactionStatus {
}

class FraudClaim extends Dispute {
} // Legal because Dispute is non-sealed

public class DataModeling {
    public static void main(String[] args) {
        // A. Record Magic (Equality & toString)
        User u1 = new User("alice", 1001);
        User u2 = new User("alice", 1001);

        System.out.println("=== Records ===");
        System.out.println("toString(): " + u1); // "User[username=alice, id=1001]"
        System.out.println("equals():   " + u1.equals(u2)); // true (Values, not memory location)

        // B. Sealed Logic
        TransactionStatus tx = new Refund(50.00);
        TransactionStatus tx2 = new Payment();
        TransactionStatus tx3 = new Dispute();
        TransactionStatus tx4 = new FraudClaim();
        handleTransaction(tx);
        handleTransaction(tx2);
        handleTransaction(tx3);
        handleTransaction(tx4);
    }

    static void handleTransaction(TransactionStatus tx) {
        System.out.println("\n=== Sealed Classes ===");
        // Pattern Matching switch deals with the specific types
        switch (tx) {
            case Payment p -> System.out.println("Success: " + p);
            case Refund r -> System.out.println("Refunding: $" + r.amount());
            case Dispute d -> System.out.println("Manual Review: " + d.getClass().getSimpleName());
        }
    }
}
