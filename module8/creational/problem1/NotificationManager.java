package module8.creational.problem1;

public class NotificationManager {
    public void send(String type, String message) {
        if (type.equals("EMAIL")) {
            new EmailService().send(message); // Hard dependency
        } else if (type.equals("SMS")) {
            new SmsService().send(message);
        } else if (type.equals("SLACK")) {
            new SlackService().send(message);
        }
    }
}
