package com.udacity.security.service;

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

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    // SUT's Dependencies
    private Set<StatusListener> statusListeners;
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private StatusListener aListener;

    // SUT's arguments
    @Mock
    private Sensor sensor1;

    // SUT
    private SecurityService securityService;

    @BeforeEach
    public void init() {
        statusListeners = new HashSet<>();
        statusListeners.add(aListener);
        securityService = new SecurityService(securityRepository, imageService, statusListeners);
    }

    /**
     * Application requirements:
     *
     * 1.   If alarm is armed and a sensor becomes activated, put the system into pending alarm status
     *      [If system is armed and a sensor becomes activated, change alarm status to pending]
     */
    @ParameterizedTest
    @MethodSource("provide_changeSensorActivationStatus_activateSensor_pendingAlarm")
    public void changeSensorActivationStatus_activateSensor_pendingAlarm(ArmingStatus armingStatus) {
        // Mock sensor
        Mockito.when(sensor1.getActive()).thenReturn(false);

        // Mock system status
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // Mock alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        // Run it
        securityService.changeSensorActivationStatus(sensor1, true);

        // Verify alarm status after
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(AlarmStatus.PENDING_ALARM));

        // Verify listener notified
        Mockito.verify(aListener, times(1)).notify(eq(AlarmStatus.PENDING_ALARM));

        // Verify activating sensor
        Mockito.verify(sensor1, times(1)).setActive(eq(true));
        Mockito.verify(securityRepository, times(1)).updateSensor(eq(sensor1));
    }

    private static Stream<Arguments> provide_changeSensorActivationStatus_activateSensor_pendingAlarm() {
        return Stream.of(
                Arguments.of(ArmingStatus.ARMED_AWAY),
                Arguments.of(ArmingStatus.ARMED_HOME)
        );
    }

    /**
     * Application requirement:
     *
     * 2.   If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm
     *      status to alarm. [This is the case where all sensors are deactivated and then one gets activated]
     */
    @ParameterizedTest
    @MethodSource("provide_changeSensorActivationStatus_activateSensor_allSensorsOff")
    public void changeSensorActivationStatus_activateSensor_allSensorsOff_activateAlarm(ArmingStatus armingStatus) {
        // Stub sensors
        Mockito.when(sensor1.getActive()).thenReturn(false);

        // Stub system status
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // Stub alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Run it
        securityService.changeSensorActivationStatus(sensor1, true);

        // Verify alarm status after
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(AlarmStatus.ALARM));

        // Verify listener notified
        Mockito.verify(aListener, times(1)).notify(eq(AlarmStatus.ALARM));

        // Verify activating sensor
        Mockito.verify(sensor1, times(1)).setActive(eq(true));
        Mockito.verify(securityRepository, times(1)).updateSensor(eq(sensor1));
    }

    private static Stream<Arguments> provide_changeSensorActivationStatus_activateSensor_allSensorsOff() {
        return Stream.of(
                Arguments.of(ArmingStatus.ARMED_AWAY),
                Arguments.of(ArmingStatus.ARMED_HOME)
        );
    }

    /**
     * Application requirement:
     *
     * 3.   If pending alarm and all sensors are inactive, return to no alarm state
     */
    @Test
    public void changeSensorActivationStatus_deactivateSensor_allSensorsGetOff_changeAlarmStatus() {
        // Mock sensors
        Mockito.when(sensor1.getActive()).thenReturn(true);
        Sensor sensor2 = Mockito.mock(Sensor.class);
        Mockito.when(sensor2.getActive()).thenReturn(false);

        // Mock alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Mock getting 1 active and 1 inactive sensor (expect checking all sensors)
        Set<Sensor> allSensors = Set.of(sensor1, sensor2);
        Mockito.when(securityRepository.getSensors()).thenReturn(allSensors);

        // Run it
        securityService.changeSensorActivationStatus(sensor1, false);

        // Verify alarm status after
        AlarmStatus noAlarm = AlarmStatus.NO_ALARM;
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(noAlarm));

        // Verify listener notified
        Mockito.verify(aListener, times(1)).notify(eq(noAlarm));

        // Verify deactivating sensor
        Mockito.verify(sensor1, times(1)).setActive(eq(false));
        Mockito.verify(securityRepository, times(1)).updateSensor(eq(sensor1));
    }

    /**
     * Application requirement:
     *
     * 4.   If alarm is active, change in sensor state should not affect the alarm state.
     *
     * For case when sensor is activated
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provide_changeSensorActivationStatus_activeAlarm_activateSensor_sameAlarm")
    public void changeSensorActivationStatus_activeAlarm_activateSensor_sameAlarm(ArmingStatus armingStatus) {
        // Mock sensor
        Mockito.when(sensor1.getActive()).thenReturn(false);

        // Mock system status
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // Mock alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // Run it
        Boolean activate = true;
        securityService.changeSensorActivationStatus(sensor1, activate);

        // Verify changing alarm status never called
        Mockito.verify(securityRepository, never()).setAlarmStatus(any());

        // Verify sensor's activity changed
        Mockito.verify(sensor1, times(1)).setActive(activate);

        // Verify listener notified
        Mockito.verify(securityRepository, times(1)).updateSensor(eq(sensor1));
    }

    private static Stream<Arguments> provide_changeSensorActivationStatus_activeAlarm_activateSensor_sameAlarm() {
        return Stream.of(
          Arguments.of(ArmingStatus.ARMED_AWAY),
          Arguments.of(ArmingStatus.ARMED_HOME)
        );
    }

    /**
     * Application requirement:
     *
     * 4.   If alarm is active, change in sensor state should not affect the alarm state.
     *
     * For case when sensor is deactivated
     */
    @Test
    public void changeSensorActivationStatus_activeAlarm_deactivateSensor_sameAlarm() {
        // Stub sensor
        Mockito.when(sensor1.getActive()).thenReturn(true);

        // Stub alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // Run it
        Boolean activate = false;
        securityService.changeSensorActivationStatus(sensor1, activate);

        // Verify changing alarm status never called
        Mockito.verify(securityRepository, never()).setAlarmStatus(any());

        // Verify sensor's activity changed
        Mockito.verify(sensor1, times(1)).setActive(activate);

        // Verify listener notified
        Mockito.verify(securityRepository, times(1)).updateSensor(eq(sensor1));
    }

    /**
     * Application requirement:
     *
     * 5.   If a sensor is activated while already active and the system is in pending state, change it to alarm state.
     */
    @Test
    public void changeSensorActivationStatus_pendingAlarm_reactivateSensor_activeAlarm() {
        // Mock a sensor
        Mockito.when(sensor1.getActive()).thenReturn(false);

        // Mock alarm status
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Run it
        securityService.changeSensorActivationStatus(sensor1, true);

        // Verify changing alarm status to active
        AlarmStatus alarmActive = AlarmStatus.ALARM;
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(alarmActive));

        // Verify listener notified
        Mockito.verify(aListener, times(1)).notify(alarmActive);

        // Verify sensor activated
        Mockito.verify(sensor1, times(1)).setActive(true);

        // Verify updating sensor is never called
        Mockito.verify(securityRepository, times(1)).updateSensor(sensor1);
    }

    /**
     * Application requirement:
     *
     * 6.   If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @Test
    public void changeSensorActivationStatus_deactivateInactiveSensor_sameAlarm() {
        // Mock a sensor
        Mockito.when(sensor1.getActive()).thenReturn(false);

        // Run it
        securityService.changeSensorActivationStatus(sensor1, false);

        // Verify no alarm state changed
        Mockito.verify(securityRepository, never()).setAlarmStatus(any());

        // Verify listener not notified
        Mockito.verify(aListener, never()).notify(any());

        // Verify deactivating sensor never called
        Mockito.verify(sensor1, never()).setActive(any());

        // Verify updating sensor is never called
        Mockito.verify(securityRepository, never()).updateSensor(any());
    }

    /**
     * Application requirement:
     *
     * 7.   If the image service identifies an image containing a cat while the system is armed-home, put the system
     *      into alarm status
     */
    @Test
    public void processImage_isACat_armedHome_activateAlarm() {
        // Dummy SUT's argument
        BufferedImage currentCameraImage = Mockito.mock(BufferedImage.class);

        // Stub recognising a cat
        Mockito.when(imageService.imageContainsCat(eq(currentCameraImage), eq(50.0f))).thenReturn(true);

         // Stub arming status
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // Run it
        securityService.processImage(currentCameraImage);

        // Verify changing alarm status
        AlarmStatus alarmActive = AlarmStatus.ALARM;
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(alarmActive));

        // Verify notifying listener
        Mockito.verify(aListener, times(1)).notify(alarmActive);
    }

    /**
     * Application requirement:
     *
     * 8.   If the image service identifies an image that does not contain a cat, change the status to no alarm as long
     *      as the sensors are not active
     */
    @Test
    public void processImage_notACat_allSensorsInactive_setNoAlarm() {
        // Dummy image
        BufferedImage currentCameraImage = Mockito.mock(BufferedImage.class);

        // Stub recognising a cat
        Mockito.when(imageService.imageContainsCat(eq(currentCameraImage), eq(50.0f))).thenReturn(false);

        // Stub 2 inactive sensors
        when(sensor1.getActive()).thenReturn(false);
        Sensor sensor2 = Mockito.mock(Sensor.class);
        when(sensor2.getActive()).thenReturn(false);
        Set<Sensor> allSensors = Set.of(sensor1, sensor2);

        // Stub getting sensors (for checking all of them)
        Mockito.when(securityRepository.getSensors()).thenReturn(allSensors);

        // Run it
        securityService.processImage(currentCameraImage);

        // Verify changing alarm status
        AlarmStatus alarmActive = AlarmStatus.NO_ALARM;
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(alarmActive));

        // Verify notifying listener
        Mockito.verify(aListener, times(1)).notify(alarmActive);
    }

    /**
     * Application requirement:
     *
     * 9.   If the system is disarmed, set the status to no alarm
     */
    @Test
    public void setArmingStatus_disarmed_setNoAlarm() {
        // Run it
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        AlarmStatus noAlarm = AlarmStatus.NO_ALARM;
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(eq(noAlarm));
        Mockito.verify(aListener, times(1)).notify(eq(noAlarm));
    }

    /**
     * Application requirement:
     *
     * 10.  If the system is armed, reset all sensors to inactive
     */
    @ParameterizedTest
    @MethodSource("provideArmingStatus")
    public void setArmingStatus_armed_resetAllSensors(ArmingStatus armingStatus) {
        // Stub 2 sensors
        when(sensor1.getActive()).thenReturn(false);
        Sensor sensor2 = Mockito.mock(Sensor.class);
        when(sensor2.getActive()).thenReturn(true);
        Set<Sensor> allSensors = Set.of(sensor1, sensor2);

        // Stub getting sensors (for checking all of them)
        Mockito.when(securityRepository.getSensors()).thenReturn(allSensors);

        // Run it
        securityService.setArmingStatus(armingStatus);

        // Verify setting second sensor to inactive
        Mockito.verify(sensor1, never()).setActive(any());
        Mockito.verify(sensor2, times(1)).setActive(false);

        // Verify updating sensor 2
        Mockito.verify(securityRepository, times(1)).updateSensor(sensor2);

        // Verify notifying listener
        Mockito.verify(aListener, times(1)).sensorStatusChanged();

        // Verify changing arming status
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
     * 11.  If the system is armed-home while the camera shows a cat, set the alarm status to alarm
     */
    @Test
    public void setArmingStatus_armedHome_catIsDetected_activateAlarm() {
        // Stub cat detected
        Mockito.when(securityRepository.getCatDetected()).thenReturn(true);

        // Run it
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // Verify alarm activated
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);

        // Verify listener notified
        Mockito.verify(aListener, times(1)).notify(AlarmStatus.ALARM);
    }
}
