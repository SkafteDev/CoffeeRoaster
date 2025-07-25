# CoffeeRoaster

CoffeeRoaster is an experimental setup for controlling a small coffee bean roaster with an ESP32 microcontroller and a companion Android application.  The project contains two major parts:

* **Firmware** – Arduino‑style source code for the ESP32 that drives the heater through a solid state relay and reports the bean temperature over Bluetooth or a serial link.
* **AndroidApp** – A simple Android application used to connect to the roaster via BLE, adjust the duty cycle of the heater and plot the temperature profile.  A CSV exporter is included for saving roast data.

## Demo setup
Hardware list:
- [ESP32 microcontroller](https://www.aliexpress.com/item/1005006456519790.html)
- [MAX6675 + K Thermocouple](https://www.aliexpress.com/item/1005006222174410.html)
- Solid State Relay, [SSR-40DA DC Control AC](https://www.aliexpress.com/item/4000269173201.html)
- [1200W Popcorn machine](https://www.computersalg.dk/i/9572438/day-popcornmaskine-1200w)
- Power supply Module. 220V to DC 24V [XK-2412-24](https://www.aliexpress.com/item/1005003107326198.html)
- [LCD1602 I2C display](https://www.aliexpress.com/item/1005006100081942.html)
- [Potentiometer for setpoint temperature](https://www.aliexpress.com/item/1005008570514925.html)

Demo videos:  
[Roasting-1.mp4](demo%2FRoasting-1.mp4)  
[Roasting-2.mp4](demo%2FRoasting-2.mp4)  

This image shows a primitive minimum viable hardware setup with an ESP32, a breadboard, an LCD, a thermocouple, a relay, and a potentiometer that controls the modified popcorn machine.  
![hardware-setup-2.png](demo%2Fhardware-setup-2.png)

This image shows an example of the inside of the roasting bed and the thermocouple.  
![hardware-setup-3.png](demo%2Fhardware-setup-3.png)

## Repository structure

```
Firmware/ESP32
├── Bluetooth      # ESP32 firmware with BLE interface
├── Serial         # ESP32 firmware controlled locally with a potentiometer
└── MAX6675_library# Thermocouple amplifier library (vendored)
AndroidApp         # Android Studio project
```

The `Bluetooth` sketch exposes the current temperature and duty cycle as BLE characteristics that the Android application consumes.  The `Serial` sketch is meant for a standalone setup where a potentiometer and LCD are attached to the ESP32.

## Prerequisites

### Firmware

* [Arduino IDE](https://www.arduino.cc/) with ESP32 board support installed
* The included `MAX6675_library` is required for the thermocouple amplifier
* An ESP32 development board capable of driving a solid state relay

### Android application

* Android Studio (tested with Gradle plugin 7.3.1)
* Android SDK 31 or newer
* Java 9 compatibility is used in the build configuration

Run `./gradlew assembleDebug` inside `AndroidApp` to build the APK.

## Hardware requirements

The ESP32 firmware expects the following hardware connections:

* **Heater and solid state relay** attached to GPIO 16
* **MAX6675 thermocouple amplifier**
  * CLK on GPIO 5
  * CS  on GPIO 23
  * DO  on GPIO 19
* **Optional potentiometer** on GPIO 26 for the serial firmware
* **Optional 16x2 I²C LCD** at address `0x27` used by the serial firmware

A standard K‑type thermocouple connects to the MAX6675 module to measure bean temperature.  The Android app communicates over BLE with the firmware from the `Bluetooth` folder.

