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
import static org.mockito.Mockito.never;
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

    // SUT's arguments
    @Mock
    private Sensor sensor1;
    @Mock
    private Sensor sensor2;

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
        // Mock sensor
        Mockito.when(sensor1.getActive()).thenReturn(false);

        // Mock system status
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // Mock alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(alarmBefore);

        // Run it
        securityService.changeSensorActivationStatus(sensor1, true);

        // Verify alarm status after
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(alarmAfter));

        // Verify activating sensor
        Mockito.verify(sensor1, times(1)).setActive(eq(true));
        Mockito.verify(securityRepository, times(1)).updateSensor(eq(sensor1));
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
        // Mock sensors
        Mockito.when(sensor1.getActive()).thenReturn(true);
        Mockito.when(sensor2.getActive()).thenReturn(false);

        // Mock alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Mock getting 1 active and 1 inactive sensor (expect checking all sensors)
        Set<Sensor> allSensors = Set.of(sensor1, sensor2);
        Mockito.when(securityRepository.getSensors()).thenReturn(allSensors);

        // Run it
        securityService.changeSensorActivationStatus(sensor1, false);

        // Verify alarm status after
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(AlarmStatus.NO_ALARM));

        // Verify deactivating sensor
        Mockito.verify(sensor1, times(1)).setActive(eq(false));
        Mockito.verify(securityRepository, times(1)).updateSensor(eq(sensor1));
    }

    /**
     * Application requirement:
     *
     * 4.   If alarm is active, change in sensor state should not affect the alarm state.
     */
    @ParameterizedTest(name = "[{index}] {0}, Sensor is active: {1}, setting it to active: {2}")
    @MethodSource("provide_changeSensorActivationStatus_activeAlarm_updateSensor_sameAlarm")
    public void changeSensorActivationStatus_activeAlarm_updateSensor_sameAlarm(ArmingStatus armingStatus,
                                                                                Boolean isActive, Boolean activate) {
        // Mock sensor
        Mockito.when(sensor1.getActive()).thenReturn(isActive);

        // Mock system status
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // Mock alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // Run it
        securityService.changeSensorActivationStatus(sensor1, activate);

        // Verify changing alarm status never called
        Mockito.verify(securityRepository, never()).setAlarmStatus(any());
    }

    private static Stream<Arguments> provide_changeSensorActivationStatus_activeAlarm_updateSensor_sameAlarm() {
        return Stream.of(
          Arguments.of(ArmingStatus.ARMED_AWAY, false, true),
          Arguments.of(ArmingStatus.ARMED_HOME, false, true),
          Arguments.of(ArmingStatus.ARMED_AWAY, true, false),
          Arguments.of(ArmingStatus.ARMED_HOME, true, false)
        );
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

    /**
     * Application requirement:
     *
     * 5.   If a sensor is activated while already active and the system is in pending state, change it to alarm state.
     */
    @Test
    public void changeSensorActivationStatus_pendingAlarm_reactivateSensor_activeAlarm() {
        // Mock a sensor
        Mockito.when(sensor1.getActive()).thenReturn(true);

        // Mock alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Run it
        securityService.changeSensorActivationStatus(sensor1, true);

        // Verify changing alarm status to active
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(AlarmStatus.ALARM));

        // Verify activating sensor never called
        Mockito.verify(sensor1, never()).setActive(any());

        // Verify updating sensor is never called
        Mockito.verify(securityRepository, never()).updateSensor(any());
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