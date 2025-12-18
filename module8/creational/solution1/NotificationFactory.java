package module8.creational.solution1;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class NotificationFactory {
    private Map<String, Notification> notificationMap;

    public NotificationFactory() {
        notificationMap = new HashMap<>();
        ServiceLoader<Notification> loader = ServiceLoader.load(Notification.class);
        for (Notification notification : loader) {
            notificationMap.put(notification.getType(), notification);
        }
    }

    public Notification createNotification(String type) {
        return notificationMap.get(type);
    }

}
