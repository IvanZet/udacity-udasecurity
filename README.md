# Udacity UdaSecurity

This is my implementation of the third (out of three) project in Udacity's Java Programing Nanodegree.

## Application requirements to test
1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
3. If pending alarm and all sensors are inactive, return to no alarm state.
4. If alarm is active, change in sensor state should not affect the alarm state.
5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
9. If the system is disarmed, set the status to no alarm.
10. If the system is armed, reset all sensors to inactive.
11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.

## Optional TODO
- [ ] **Connect Your Project to the AWS Image Recognition Library**\
Once you have created an interface for your image service, complete the steps described in the AwsImageService to
create credentials and provide them in a properties file for your application. Change the ImageService
implementation class in the CatpointGui class to use the AwsImageService instead of the FakeImageService. Try
submitting different types of images and see what comes back!
- [ ] **Create a Finite State Machine diagram to enhance your project understanding**\
Link [here](https://www.javatpoint.com/uml-state-machine-diagram)
- [ ] **Add Integration Tests**\
Create a FakeSecurityRepository class that works just like the PretendDatabaseSecurityRepository class (except without
the property files). Create a second test class called SecurityServiceIntegrationTest.java and write methods that test
our requirements as integration tests.\
These tests can call service methods and then use JUnit Assertions to verify that the values you retrieve after
performing operations are the expected values.
