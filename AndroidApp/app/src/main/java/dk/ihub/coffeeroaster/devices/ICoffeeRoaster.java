package dk.ihub.coffeeroaster.devices;

import dk.ihub.coffeeroaster.events.ICoffeeRoasterEventListener;

public interface ICoffeeRoaster {
    // Connection management

    /**
     * Connects to the coffee roaster.
     * @return Returns true if successfully connected. Otherwise false.
     */
    boolean connect();

    /**
     * Disconnects to the coffee roaster.
     * @return Returns true if successfully disconnected. Otherwise false.
     */
    boolean disconnect();



    // Monitor operations

    /**
     * Gets the bean temperature in the chamber measured in celsius [C].
     * @return
     */
    float getBeanTemperatureCelsius();

    /**
     * Gets the duty cycle of the coffee roaster.
     * @return A duty cycle value in the range [0, 100]%. 0 Equals off and 100 equals max load.
     */
    float getDutyCycle();

    void subscribe(ICoffeeRoasterEventListener listener);




    // Control operations

    /**
     * Sets the Duty Cycle in %.
     * @param value Must be in the range [0, 100]%. 0 Equals off and 100 equals max load.
     * @return Returns true if the operation was executed successfully. Otherwise false.
     */
    boolean setDutyCycle(float value);
}
