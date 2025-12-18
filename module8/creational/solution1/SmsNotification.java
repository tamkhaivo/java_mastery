package module8.creational.solution1;

public final class SmsNotification implements Notification {
    @Override
    public void send(String message) {
        System.out.println("Sending SMS notification: " + message);
    }
}
