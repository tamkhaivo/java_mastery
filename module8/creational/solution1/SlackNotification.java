package module8.creational.solution1;

public final class SlackNotification implements Notification {
    @Override
    public void send(String message) {
        System.out.println("Sending Slack notification: " + message);
    }
}
