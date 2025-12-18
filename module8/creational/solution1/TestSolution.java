package module8.creational.solution1;

public class TestSolution {
    public static void main(String[] args) {
        System.out.println("Starting Module 8 Solution 1 Verification...");

        NotificationFactory factory = new NotificationFactory();
        NotificationManager manager = new NotificationManager(factory);

        // Test EMAIL
        System.out.println("\nTesting EMAIL:");
        manager.sendNotification(NotificationType.EMAIL, "Hello via Email");

        // Test SMS
        System.out.println("\nTesting SMS:");
        manager.sendNotification(NotificationType.SMS, "Hello via SMS");

        // Test SLACK
        System.out.println("\nTesting SLACK:");
        manager.sendNotification(NotificationType.SLACK, "Hello via Slack");

        // Test INVALID (Removed as it's not in the Enum)
        System.out.println("\nVerification Complete.");
    }
}
