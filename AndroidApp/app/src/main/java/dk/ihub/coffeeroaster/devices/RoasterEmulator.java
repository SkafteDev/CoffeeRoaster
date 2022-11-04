package dk.ihub.coffeeroaster.devices;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dk.ihub.coffeeroaster.events.CoffeeRoasterEvent;
import dk.ihub.coffeeroaster.events.ICoffeeRoasterEventListener;

public class RoasterEmulator implements ICoffeeRoaster {

    private float dutyCycle;
    private double temperature;
    private double uptime; // In seconds.
    private double systemBootTime;
    private Thread esp32;

    private List<ICoffeeRoasterEventListener> subscribers;

    public RoasterEmulator() {
    }

    @Override
    public boolean connect() {
        this.esp32 = new Thread(() ->
        {
            this.systemBootTime = System.currentTimeMillis();
            this.dutyCycle = 0; // Default value.
            this.temperature = 20; // Typical chamber temperature on cold start.

            // Emulate the ESP32 roaster control loop
            while (true) {
                try {
                    this.uptime = (System.currentTimeMillis() - systemBootTime) / 1000;
                    this.temperature = 196.82 / (1+ 6.54 * Math.exp(-0.04*uptime*(dutyCycle/100)));

                    CoffeeRoasterEvent event = new CoffeeRoasterEvent();
                    event.setDutyCycle(this.dutyCycle);
                    event.setBeanTemperatureCelsius((float)temperature);

                    notifySubscribers(event);

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e("RoasterStub", "ESP32 thread interrupted");
                    break;
                }
            }
        });
        this.esp32.start();
        return true;
    }

    @Override
    public boolean disconnect() {
        this.esp32.interrupt();
        return true;
    }

    @Override
    public float getBeanTemperatureCelsius() {
        return (float)this.temperature;
    }

    @Override
    public float getDutyCycle() {
        return this.dutyCycle;
    }

    @Override
    public void subscribe(ICoffeeRoasterEventListener listener) {
        if (this.subscribers == null) {
            this.subscribers = new ArrayList<>();
        }
        this.subscribers.add(listener);
    }

    @Override
    public boolean setDutyCycle(float value) {
        this.dutyCycle = value;

        return true;
    }

    private void notifySubscribers(CoffeeRoasterEvent event) {
        try {
            for (ICoffeeRoasterEventListener l : this.subscribers) {
                l.handleEvent(event);
            }
        } catch (Error e) {
            Log.e("RoasterStub", e.getMessage());
        }

    }
}
