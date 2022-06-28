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
     * 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status
     */
    @ParameterizedTest
    @MethodSource("provideArmingStatus")
    public void changeSensorActivationStatus_activateSensor_alarmArmed_putPending(ArmingStatus armingStatus) {
        // SUT's arguments
        Sensor sensor = new Sensor("testSensor", SensorType.DOOR);
        sensor.setActive(false);
        Boolean active = true;

        // Set arming status to armed
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // Set alarm to armed
        AlarmStatus alarm = AlarmStatus.ALARM;
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(alarm);

        // Run it
        securityService.changeSensorActivationStatus(sensor, active);

        // Verify setting
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(AlarmStatus.PENDING_ALARM));
    }

    private static Stream<Arguments> provideArmingStatus() {
        return Stream.of(
          Arguments.of(ArmingStatus.ARMED_AWAY),
          Arguments.of(ArmingStatus.ARMED_HOME)
        );
    }

    @ParameterizedTest
    @MethodSource("provideArmingStatus")
    public void setArmingStatus_armed(ArmingStatus armingStatus) {
        // Run it
        securityService.setArmingStatus(armingStatus);

        Mockito.verify(securityRepository, times(1)).setArmingStatus(eq(armingStatus));
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