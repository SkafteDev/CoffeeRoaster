#include "max6675.h"
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// System boot time.
float bootTime = 0;

// Pin of the solid state relay.
// The solid state relay controls the binary ON/OFF state of the heater element.
int relayPin = 16;

// Pins for reading the temperature sensor values.
int thermoDO = 19;
int thermoCS = 23;
int thermoCLK = 5;
MAX6675 thermocouple(thermoCLK, thermoCS, thermoDO);

// Duty cycle is a value in the range [0, 100]%.
// Duty cycle of 0% means that the heater is off at all times.
// Duty cycle of 100% means that the heater is on at all times.
// Duty cycle of 25% means that the heater is on for 250 [ms] of a second and off 750 [ms] of a second.
// Duty cycle of 75% means that the heater is on for 750 [ms] of a second and off 250 [ms] of a second.
float _dutyCycle = 0;

// The current temperature of the heat chamber.
float _currentTemp = 0;

// Thread that handles the temperature reading.
TaskHandle_t temperatureSensor_t;

// Thread that handles the BLE notify/read/write.
TaskHandle_t bleTask_t;

//BLE server name
#define bleServerName "COFFEE_ROASTER_ESP32"
// BLE SERVICE UUID
#define SERVICE_UUID "91bad492-b950-4226-aa2b-4ede9fa42f59"
BLECharacteristic temperatureCelsiusCharacteristics("cba1d466-344c-4be3-ab3f-189f80dd7518", BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor temperatureCelsiusDescriptor(BLEUUID((uint16_t)0x2902));

BLECharacteristic dutyCycleCharacteristics("ca73b3ba-39f6-4ab3-91ae-186dc9577d99",
                                              BLECharacteristic::PROPERTY_NOTIFY|
                                              BLECharacteristic::PROPERTY_WRITE|
                                              BLECharacteristic::PROPERTY_READ);
BLEDescriptor dutyCycleDescriptor(BLEUUID((uint16_t)0x2903));

BLEServer *pServer;
bool deviceConnected = false;

//Setup callbacks onConnect and onDisconnect
class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
    Serial.println("BLE client connected.");
  };
  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    Serial.println("BLE client disconnected.");
    pServer->getAdvertising()->start();
    Serial.println("MyServerCallbacks::onDisconnect(): Waiting for a client connection to notify...");
  }
};

void setup() {
  Serial.begin(115200);
  
  pinMode(relayPin, OUTPUT);
  bootTime = millis();

  xTaskCreatePinnedToCore(
      readTemp, /* Function to implement the task */
      "TemperatureSensor", /* Name of the task */
      10000,  /* Stack size in words */
      NULL,  /* Task input parameter */
      0,  /* Priority of the task */
      &temperatureSensor_t,  /* Task handle. */
      0); /* Core where the task should run */

  xTaskCreatePinnedToCore(
      bleHandler, /* Function to implement the task */
      "BLE_Handler", /* Name of the task */
      10000,  /* Stack size in words */
      NULL,  /* Task input parameter */
      0,  /* Priority of the task */
      &bleTask_t,  /* Task handle. */
      0); /* Core where the task should run */

  delay(1000);
  Serial.println("setup() completed.");
}

void setupBLE() {
  // Create the BLE Device
  BLEDevice::init(bleServerName);

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *bleService = pServer->createService(SERVICE_UUID);

  // Temperature
  bleService->addCharacteristic(&temperatureCelsiusCharacteristics);
  temperatureCelsiusDescriptor.setValue("Temperature Celsius");
  temperatureCelsiusCharacteristics.addDescriptor(&temperatureCelsiusDescriptor); 

  // Duty cycle
  bleService->addCharacteristic(&dutyCycleCharacteristics);
  dutyCycleDescriptor.setValue("Duty cycle");
  dutyCycleCharacteristics.addDescriptor(&dutyCycleDescriptor);
  
  // Start the service
  bleService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pServer->getAdvertising()->start();
  Serial.println("setupBLE(): Waiting for a client connection to notify...");
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
      temperatureC += thermocouple.readCelsius();
    }
  
    _currentTemp = temperatureC / numReadings;
  }
}

void bleHandler(void * pvParameters) {
  setupBLE();

  while(true) {
    if (deviceConnected) {
      int currentTemp = _currentTemp; // Only ints allowed.
      temperatureCelsiusCharacteristics.setValue(currentTemp);
      temperatureCelsiusCharacteristics.notify();
    
      std::string newDutyCycleValStr = dutyCycleCharacteristics.getValue();
      int newDutyCycleVal = atoi(newDutyCycleValStr.c_str());
      if (0 <= newDutyCycleVal && newDutyCycleVal <= 100) { // Duty cycle must be between [0, 100].
        _dutyCycle = newDutyCycleVal;
        dutyCycleCharacteristics.notify();
      }
  
      Serial.print("bleHandler(): ");
      Serial.print("Notified (temperature, " + String(currentTemp) + "), (dutyCycle, " + String(_dutyCycle) + ")");
      Serial.println();
    }

    delay(1000);
  }
}

void loop() {
  if (_dutyCycle == 0) {
    return; // Nothing to do. The heater must be off.
  }

  float delayMs = _dutyCycle * 10; // Transform the duty cyle [%] to the corresponding delay in [ms]. DelayMs is in the range [0, 1000] [ms].  
  
  digitalWrite(relayPin, HIGH); // Turn on on heater
  delay(delayMs); // Sleep for the fraction of the second while the heater is in ON state.
  digitalWrite(relayPin, LOW); // Turn off heater
  delay(1000 - (delayMs)); // Sleep for the fraction of the second while the heater is in the OFF state.

  float systemUptime = (millis() - bootTime) / 1000;
  int minutes = int(systemUptime / 60);
  int seconds = int(systemUptime) % 60;
}
