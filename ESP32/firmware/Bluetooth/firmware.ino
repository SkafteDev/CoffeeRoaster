#include "max6675.h"
#include "BluetoothSerial.h"
// Check if bluetooth configs are enabled.
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;

// System boot time.
float bootTime = 0;

// Pin of the solid state relay.
// The solid state relay controls the binary ON/OFF state of the heater element.
int relayPin = 16;

// Pins for reading the temperature sensor values.
int thermoDO = 19;
int thermoCS = 23;
int thermoCLK = 5;
//MAX6675 thermocouple(thermoCLK, thermoCS, thermoDO);

// Duty cycle is a value in the range [0, 100]%.
// Duty cycle of 0% means that the heater is off at all times.
// Duty cycle of 100% means that the heater is on at all times.
// Duty cycle of 25% means that the heater is on for 250 [ms] of a second and off 750 [ms] of a second.
// Duty cycle of 75% means that the heater is on for 750 [ms] of a second and off 250 [ms] of a second.
float dutyCycle = 0;

// The current temperature of the heat chamber.
float currentTemp = 0;

// Thread that handles the temperature reading.
//TaskHandle_t temperatureSensor_t;

// Thread that handles the inbound bluetooth serial data.
TaskHandle_t inboundBluetooth_t;

void setup() {
  Serial.begin(115200);
  SerialBT.begin("CoffeeRoaster"); //Bluetooth device name
  
  pinMode(relayPin, OUTPUT);
  bootTime = millis();

//  xTaskCreatePinnedToCore(
//      readTemp, /* Function to implement the task */
//      "TemperatureSensor", /* Name of the task */
//      10000,  /* Stack size in words */
//      NULL,  /* Task input parameter */
//      0,  /* Priority of the task */
//      &temperatureSensor_t,  /* Task handle. */
//      0); /* Core where the task should run */

 xTaskCreatePinnedToCore(
      readInboundBT, /* Function to implement the task */
      "ReadInboundBT", /* Name of the task */
      10000,  /* Stack size in words */
      NULL,  /* Task input parameter */
      1,  /* Priority of the task */
      &inboundBluetooth_t,  /* Task handle. */
      0); /* Core where the task should run */

  delay(1000);
  Serial.println("setup() completed.");
}

/**
 * Read the avg. temperature and store the value
 */
void readTemp(void * pvParameters) {
  for (;;) {
    delay(250); // The temperature sensor needs time between samples. A high sample frequency might result in no change in temperature reading.
    float temperatureC = 0;
    int numReadings = 10;
    for (int i = 0; i < numReadings; i++) {
//      temperatureC += thermocouple.readCelsius();
    }
  
    currentTemp = temperatureC / numReadings;
  }
}

/*
 * PROTOCOL:
 * Data are read from the serial bluetooth channel as a stream of bytes.
 * Packets are of length 256 bytes.
 * The first byte determines the type of the operation.
 * The second, third and fourth bytes determines the identifier to operate on.
 * The remaining 252 bytes determines the payload.
 * 
 * Read operations:
 * Packets starts with R followed by an identifier.
 * Example...
 * [R001] translates to READ identifier 001. No payload is specified.
 * 
 * Write operations:
 * Packets starts with W followed by an identifier.
 * Example...
 * [W001VALUE] translates to WRITE identifier 001 with the payload VALUE.
 */
int bufferSize = 256;
char buffer[256]; // Buffer to store the incoming packet.
bool waitForRead = false; // Flag to determine if the buffer is ready to be read.
void readInboundBT(void * parameters) {
  for (;;) {
    if (waitForRead == true) {
      continue;
    }
      
    for (int i = 0; i < bufferSize; i++) {
      if (SerialBT.available()) {
        char incomingChar = SerialBT.read();
        buffer[i] = incomingChar;
      }

      if (i == bufferSize-1) {
        waitForRead = true;
      } else {
        waitForRead = false;
      }
    }
  }
}

void loop() {
  dutyCycle = 0; // TODO: Get value from bluetooth

  if (dutyCycle == 0) {
    return; // Nothing to do. The heater must be off.
  }

  float delayMs = dutyCycle * 10; // Transform the duty cyle [%] to the corresponding delay in [ms]. DelayMs is in the range [0, 1000] [ms].  
  
  digitalWrite(relayPin, HIGH); // Turn on on heater
  delay(delayMs); // Sleep for the fraction of the second while the heater is in ON state.
  digitalWrite(relayPin, LOW); // Turn off heater
  delay(1000 - (delayMs)); // Sleep for the fraction of the second while the heater is in the OFF state.

  float systemUptime = (millis() - bootTime) / 1000;
  int minutes = int(systemUptime / 60);
  int seconds = int(systemUptime) % 60;
}
