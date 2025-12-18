package module8.creational.solution1;

public class TestSolution {
    public static void main(String[] args) {
        System.out.println("Starting Module 8 Solution 1 Verification...");

        NotificationFactory factory = new NotificationFactory();
        NotificationManager manager = new NotificationManager(factory);

        // Test EMAIL
        System.out.println("\nTesting EMAIL:");
        manager.sendNotification(new EmailNotification(), "Hello via Email");

        // Test SMS
        System.out.println("\nTesting SMS:");
        manager.sendNotification(new SmsNotification(), "Hello via SMS");

        // Test SLACK
        System.out.println("\nTesting SLACK:");
        manager.sendNotification(new SlackNotification(), "Hello via Slack");

        // Test INVALID
        System.out.println("\nTesting INVALID:");
        manager.sendNotification(new InvalidNotification(), "Hello via Invalid");
        System.out.println("\nVerification Complete.");
    }
}
