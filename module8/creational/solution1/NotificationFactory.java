package module8.creational.solution1;

public class NotificationFactory {

    public Notification createNotification(Notification type) {
        return switch (type) {
            case EmailNotification _ -> new EmailNotification();
            case SmsNotification _ -> new SmsNotification();
            case SlackNotification _ -> new SlackNotification();
            case InvalidNotification _ -> new InvalidNotification();
        };
    }

}
