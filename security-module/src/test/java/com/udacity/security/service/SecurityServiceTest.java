package com.udacity.security.service;

import com.udacity.image.service.FakeImageService;
import com.udacity.image.service.ImageService;
import com.udacity.security.application.StatusListener;
import com.udacity.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    // SUT's Dependencies
    private ImageService imageService;
    private Set<StatusListener> statusListeners;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private StatusListener aListener;

    // SUT
    private SecurityService securityService;

    @BeforeEach
    public void init() {
        imageService = new FakeImageService();
        statusListeners = new HashSet<>();
        statusListeners.add(aListener);
        securityService = new SecurityService(securityRepository, imageService, statusListeners);
    }

    /**
     * Application requirements:
     *
     * 1.   If alarm is armed and a sensor becomes activated, put the system into pending alarm status
     * 2.   If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm
     *      status to alarm
     */
    @ParameterizedTest
    @MethodSource("provide_changeSensorActivationStatus_activateSensor")
    public void changeSensorActivationStatus_activateSensor_changeAlarmStatus(ArmingStatus armingStatus,
                                                                  AlarmStatus alarmBefore, AlarmStatus alarmAfter) {
        // SUT's arguments
        Sensor sensor = new Sensor("testSensor", SensorType.DOOR);
        sensor.setActive(false);
        Boolean active = true;

        // Mock system status
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // Mock alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(alarmBefore);

        // Run it
        securityService.changeSensorActivationStatus(sensor, active);

        // Verify alarm status after
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(alarmAfter));

        // Verify activating sensor
        sensor.setActive(active);
        Mockito.verify(securityRepository, times(1)).updateSensor(eq(sensor));
    }

    private static Stream<Arguments> provide_changeSensorActivationStatus_activateSensor() {
        return Stream.of(
          Arguments.of(ArmingStatus.ARMED_AWAY, AlarmStatus.ALARM, AlarmStatus.PENDING_ALARM),
          Arguments.of(ArmingStatus.ARMED_HOME, AlarmStatus.ALARM, AlarmStatus.PENDING_ALARM),
          Arguments.of(ArmingStatus.ARMED_AWAY, AlarmStatus.PENDING_ALARM, AlarmStatus.ALARM),
          Arguments.of(ArmingStatus.ARMED_HOME, AlarmStatus.PENDING_ALARM, AlarmStatus.ALARM)
        );
    }

    /**
     * Application requirement:
     *
     * 3.   If pending alarm and all sensors are inactive, return to no alarm state
     */
    @Test
    public void changeSensorActivationStatus_deactivateSensor_changeAlarmStatus() {
        // SUT's arguments
        Sensor sensorActive = new Sensor("testActive", SensorType.DOOR);
        sensorActive.setActive(true);
        Boolean active = false;

        // Mock alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Mock getting 1 active and 1 inactive sensor (expect checking all sensors)
        Sensor sensorInactive = new Sensor("testInactive", SensorType.WINDOW);
        Set<Sensor> allSensors = Set.of(
                sensorActive,
                sensorInactive
        );
        Mockito.when(securityRepository.getSensors()).thenReturn(allSensors);

        // Run it
        securityService.changeSensorActivationStatus(sensorActive, active);

        // Verify alarm status after
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(AlarmStatus.NO_ALARM));

        // Verify deactivating sensor
        sensorActive.setActive(active);
        Mockito.verify(securityRepository, times(1)).updateSensor(sensorActive);
    }

    @ParameterizedTest
    @MethodSource("provideArmingStatus")
    public void setArmingStatus_armed(ArmingStatus armingStatus) {
        // Run it
        securityService.setArmingStatus(armingStatus);

        Mockito.verify(securityRepository, times(1)).setArmingStatus(eq(armingStatus));
    }

    private static Stream<Arguments> provideArmingStatus() {
        return Stream.of(
                Arguments.of(ArmingStatus.ARMED_AWAY),
                Arguments.of(ArmingStatus.ARMED_HOME)
        );
    }


    @Test
    public void setArmingStatus_disarmed() {
        ArmingStatus armingStatus = ArmingStatus.DISARMED;

        // Run it
        securityService.setArmingStatus(armingStatus);

        AlarmStatus noAlarm = AlarmStatus.NO_ALARM;
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(noAlarm));
        Mockito.verify(aListener, times(1)).notify(eq(noAlarm));
    }
}