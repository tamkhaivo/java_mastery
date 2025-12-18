package module8.creational.solution1;

public sealed interface Notification
        permits EmailNotification, SmsNotification, SlackNotification, InvalidNotification {
    void send(String message);
}
