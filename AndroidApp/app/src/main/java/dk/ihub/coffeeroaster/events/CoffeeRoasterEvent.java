package dk.ihub.coffeeroaster.events;

public class CoffeeRoasterEvent {
    private final float beanTemperatureCelsius;
    private final float dutyCycle;

    public CoffeeRoasterEvent(float beanTemperatureCelsius, float dutyCycle) {
        this.beanTemperatureCelsius = beanTemperatureCelsius;
        this.dutyCycle = dutyCycle;
    }

    public float getBeanTemperatureCelsius() {
        return beanTemperatureCelsius;
    }

    public float getDutyCycle() {
        return dutyCycle;
    }
}
