package module8.creational.solution1;

public class NotificationManager {
    private NotificationFactory notificationFactory;

    public NotificationManager(NotificationFactory notificationFactory) {
        this.notificationFactory = notificationFactory;
    }

    public void sendNotification(Notification type, String message) {
        Notification notification = notificationFactory.createNotification(type);
        notification.send(message);
    }
}
