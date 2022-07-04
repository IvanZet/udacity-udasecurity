package com.udacity.security.service;

import com.udacity.image.service.ImageService;
import com.udacity.security.application.StatusListener;
import com.udacity.security.data.AlarmStatus;
import com.udacity.security.data.ArmingStatus;
import com.udacity.security.data.SecurityRepository;
import com.udacity.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService,
                           Set<StatusListener> statusListeners) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
        this.statusListeners = statusListeners;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        // FIXME: reset all sensort to inactive
        // Test requirement 10
        // FIXME: check if cat is detected. Activate alarm if so
        // Test requirement 11
        securityRepository.setArmingStatus(armingStatus);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (areOffAllSensors()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM ->
                {
                    if (areOffAllSensors()) {
                        setAlarmStatus(AlarmStatus.ALARM);
                    }
                }
        }
    }

    /**
     * Internal method for checking that all sensors are deactivated.
     */
    private Boolean areOffAllSensors() {
        return getSensors().stream()
                .map(Sensor::getActive)
                .allMatch(a -> a.equals(false));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated(Sensor sensor) {
        if (securityRepository.getAlarmStatus().equals(AlarmStatus.PENDING_ALARM) &&
                areOffAllSensorsButOne(sensor)) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }

    /**
     * Internal method for updating alarm status when an already active sensor has been activated
     */
    private void handleActiveSensorActivated() {
        if (securityRepository.getAlarmStatus().equals(AlarmStatus.PENDING_ALARM)) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for checking that all sensors but the argument one are deactivated.
     * @param sensor
     */
    private Boolean areOffAllSensorsButOne(Sensor sensor) {
        return getSensors().stream()
                .filter(s -> ! s.equals(sensor))
                .map(Sensor::getActive)
                .allMatch(a -> a.equals(false));
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        if(!sensor.getActive() && active) {
            handleSensorActivated();
        } else if (sensor.getActive() && !active) {
            handleSensorDeactivated(sensor);
        } else if (sensor.getActive() && active) {
            handleActiveSensorActivated();
            return;
        } else if (!sensor.getActive() && !active) {
            return;
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}
