package dk.ihub.coffeeroaster.devices;

import dk.ihub.coffeeroaster.MyApp;
import dk.ihub.coffeeroaster.events.CoffeeRoasterEvent;
import dk.ihub.coffeeroaster.events.ICoffeeRoasterEventListener;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ESP32Roaster implements ICoffeeRoaster {

    private static final String ESP32_BT_ADDRESS = "3C:61:05:16:C8:36";
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // This is a well-known UUID for serial boards.

    private final BluetoothManager btManager;
    private final BluetoothAdapter btAdapter;
    private BluetoothDevice btDevice;
    private BluetoothGatt btGatt;

    private float dutyCycle;
    private float temperatureC;
    private Thread pollingThread;

    private List<ICoffeeRoasterEventListener> listeners;

    @SuppressLint("MissingPermission")
    public ESP32Roaster() {
        this.btManager = MyApp.getAppContext().getSystemService(BluetoothManager.class);
        this.btAdapter = btManager.getAdapter();
    }

    private Thread createPollingThread() {
        return new Thread(() ->
        {
            this.dutyCycle = 0; // Default value.

            while (true) {
                try {
                    this.temperatureC = getBeanTemperatureCelsius();
                    this.dutyCycle = getDutyCycle();

                    CoffeeRoasterEvent event = new CoffeeRoasterEvent();
                    event.setDutyCycle(this.dutyCycle);
                    event.setBeanTemperatureCelsius(this.temperatureC);

                    notifyListeners(event);

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e("ESP32Roaster", "Polling thread interrupted");
                    break;
                }
            }
        });
    }

    @Override
    @SuppressLint("MissingPermission")
    public boolean connect() {
        final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // successfully connected to the GATT Server
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // disconnected from the GATT Server
                }
            }
        };

        btDevice = btAdapter.getRemoteDevice(ESP32_BT_ADDRESS);

        if (btDevice != null) {
            btGatt = btDevice.connectGatt(MyApp.getAppContext(), true, bluetoothGattCallback);
        }



        return btSocket.isConnected();
    }

    @Override
    public boolean disconnect() {
        try {
            btSocket.close();
            pollingThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return btSocket.isConnected();
    }

    @Override
    public float getBeanTemperatureCelsius() {
        if (!btSocket.isConnected()) {
            return 0;
        }
        String cmd = "R001temperature";
        try {
            outputStream.write(cmd.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            byte[] buffer = new byte[256];
            inputStream.read(buffer);

            String response = new String(buffer);
            this.temperatureC = Float.parseFloat(response);
            return temperatureC;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public float getDutyCycle() {
        if (!btSocket.isConnected()) {
            return 0;
        }

        return this.dutyCycle;
    }

    @Override
    public void subscribe(ICoffeeRoasterEventListener listener) {
        if (this.listeners == null) {
            this.listeners = new ArrayList<>();
        }

        this.listeners.add(listener);
    }

    @Override
    public boolean setDutyCycle(float value) {
        if (!btSocket.isConnected()) {
            return false;
        }

        String cmd = "W002dutycycle" + value;

        try {
            outputStream.write(cmd.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            byte[] buffer = new byte[256];
            inputStream.read(buffer);

            String response = new String(buffer);
            this.dutyCycle = Float.parseFloat(response);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void notifyListeners(CoffeeRoasterEvent event) {
        try {
            for (ICoffeeRoasterEventListener l : this.listeners) {
                l.handleEvent(event);
            }
        } catch (Error e) {
            Log.e("RoasterStub", e.getMessage());
        }

    }
}
