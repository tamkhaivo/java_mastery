package module8.creational.solution1;

public final class InvalidNotification implements Notification {
    @Override
    public void send(String message) {
        System.out.println("Invalid notification: " + message);
    }
}
