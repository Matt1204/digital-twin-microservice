package com.example.centralOperator.model;

public interface ScheduledEvent {
    /**
     * How many minutes to wait before firing the event
     */
    long getDelayInMinutes();

    /**
     * The actual logic to run when the event fires
     */
    void execute();
}
