package dk.ihub.coffeeroaster.devices;

import dk.ihub.coffeeroaster.events.ICoffeeRoasterEventListener;

public class ESP32Roaster implements ICoffeeRoaster {
    @Override
    public boolean connect() {
        return false;
    }

    @Override
    public boolean disconnect() {
        return false;
    }

    @Override
    public float getBeanTemperatureCelsius() {
        return 0;
    }

    @Override
    public float getDutyCycle() {
        return 0;
    }

    @Override
    public void subscribe(ICoffeeRoasterEventListener listener) {

    }

    @Override
    public boolean setDutyCycle(float value) {
        return false;
    }
}
