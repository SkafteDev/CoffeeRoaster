package dk.ihub.coffeeroaster.events;

public class CoffeeRoasterEvent {
    private float beanTemperatureCelsius;
    private float dutyCycle;

    public float getBeanTemperatureCelsius() {
        return beanTemperatureCelsius;
    }

    public void setBeanTemperatureCelsius(float beanTemperatureCelsius) {
        this.beanTemperatureCelsius = beanTemperatureCelsius;
    }

    public float getDutyCycle() {
        return dutyCycle;
    }

    public void setDutyCycle(float dutyCycle) {
        this.dutyCycle = dutyCycle;
    }
}
