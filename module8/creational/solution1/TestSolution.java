package module8.creational.solution1;

public class TestSolution {
    public static void main(String[] args) {
        System.out.println("Starting Module 8 Solution 1 Verification...");

        NotificationFactory factory = new NotificationFactory();
        NotificationManager manager = new NotificationManager(factory);

        // Test EMAIL
        System.out.println("\nTesting EMAIL:");
        manager.sendNotification("EMAIL", "Hello via Email");

        // Test SMS
        System.out.println("\nTesting SMS:");
        manager.sendNotification("SMS", "Hello via SMS");

        // Test SLACK
        System.out.println("\nTesting SLACK:");
        manager.sendNotification("SLACK", "Hello via Slack");

        // Test INVALID
        System.out.println("\nTesting INVALID:");
        manager.sendNotification("INVALID", "Hello via INVALID");

        System.out.println("\nVerification Complete.");
    }
}
