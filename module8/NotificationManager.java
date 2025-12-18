package module8;

public class NotificationManager {
    public void send(String type, String message) {
        if (type.equals("EMAIL")) {
            new EmailService().send(message); // Hard dependency
        } else if (type.equals("SMS")) {
            new SmsService().send(message);
        }
        // To add "SLACK", you must modify this file.
    }

}
