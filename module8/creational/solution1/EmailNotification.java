package module8.creational.solution1;

public final class EmailNotification implements Notification {
    @Override
    public void send(String message) {
        System.out.println("Sending Email notification: " + message);
    }
}
