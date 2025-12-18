package module8.creational.solution1;

public class NotificationFactory {

    public Notification createNotification(NotificationType type) {
        return switch (type) {
            case EMAIL -> new EmailNotification();
            case SMS -> new SmsNotification();
            case SLACK -> new SlackNotification();
        };
    }

}
