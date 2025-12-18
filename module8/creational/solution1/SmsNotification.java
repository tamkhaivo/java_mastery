package module8.creational.solution1;

public class SmsNotification implements Notification {
    @Override
    public void send(String message) {
        System.out.println("Sending SMS notification: " + message);
    }

    @Override
    public String getType() {
        return "SMS";
    }
}
