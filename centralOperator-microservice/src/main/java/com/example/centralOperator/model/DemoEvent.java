package com.example.centralOperator.model;

public class DemoEvent implements ScheduledEvent {

    private final String recipient;
    private final String message;
    private final long delayMinutes;

    public DemoEvent(String recipient, String message, long delayMinutes) {
        this.recipient = recipient;
        this.message = message;
        this.delayMinutes = delayMinutes;
    }

    @Override
    public long getDelayInMinutes() {
        return delayMinutes;
    }

    @Override
    public void execute() {
        System.out.println("Sending email to " + recipient + " with message: " + message);
        // Here you could inject and call an EmailService
    }
}
