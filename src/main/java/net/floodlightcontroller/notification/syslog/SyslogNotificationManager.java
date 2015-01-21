package net.floodlightcontroller.notification.syslog;

import net.floodlightcontroller.notification.INotificationManager;

import org.slf4j.Logger;

public class SyslogNotificationManager implements INotificationManager {

    private final Logger logger;

    public SyslogNotificationManager(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void postNotification(String notes) {
        logger.warn(notes);
    }

}
