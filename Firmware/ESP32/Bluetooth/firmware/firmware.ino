#include "max6675.h"
#include "BluetoothSerial.h"
#include "Roaster.pb.h"
#include "nanopb/pb_common.h"
#include "nanopb/pb.h"
#include "nanopb/pb_encode.h"
#include "nanopb/pb_decode.h"
#include "cobs.c"
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

// Thread that handles the inbound bluetooth serial data.
TaskHandle_t inboundBluetooth_t;

// Thread that handles the dispatch of inbound bluetooth serial data.
TaskHandle_t dispatchInboundBluetooth_t;

void setup() {
  Serial.begin(115200);
  SerialBT.begin("CoffeeRoaster"); //Bluetooth device name
  
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
      readInboundBT, /* Function to implement the task */
      "ReadInboundBT", /* Name of the task */
      10000,  /* Stack size in words */
      NULL,  /* Task input parameter */
      1,  /* Priority of the task */
      &inboundBluetooth_t,  /* Task handle. */
      0); /* Core where the task should run */

 xTaskCreatePinnedToCore(
      dispatchInboundBT, /* Function to implement the task */
      "DispatchInboundBT", /* Name of the task */
      10000,  /* Stack size in words */
      NULL,  /* Task input parameter */
      1,  /* Priority of the task */
      &dispatchInboundBluetooth_t,  /* Task handle. */
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
      temperatureC += thermocouple.readCelsius();
    }
  
    _currentTemp = temperatureC / numReadings;
  }
}

/*
 * PROTOCOL:
 * Data are read from the serial bluetooth channel as a stream of bytes.
 * Packets can have max length of 256 bytes.
 * Packets are encoded with COBS (Consistent Overhead Byte Stuffing).
 * Packets are delimited by 00.
 *
*/
int _bufferSize = 256;
uint8_t _packetDelimiter = 0;
std::vector<uint8_t> _buffer; // Buffer to store the incoming packet.
bool _bufferReady = false; // Flag to determine if the buffer is ready to be read.
void readInboundBT(void * parameters) {
  for (;;) {
    while (!SerialBT.hasClient()) {
      Serial.println("readInboundBT(): Waiting for BT client to connect.");  
      delay(250);
    }
    
    if (_bufferReady == true) {
      Serial.println("readInboundBT(): Buffer ready to be read.");
      delay(250);
      continue;
    }

    while (_buffer.size() < _bufferSize && !checkPacketDelimiter(_buffer, _packetDelimiter)) {      
      if (!SerialBT.connected()) {
        Serial.println("readInboundBT(): Client disconnected. Flushing buffer.");
        _buffer.clear();
        break;
      }
      while(SerialBT.connected() && !SerialBT.available()) {
        Serial.println("readInboundBT(): Waiting for inbound data.");
        delay(250); // Wait for for inbound data.
      }
      
      if (SerialBT.available()) {
        uint8_t incomingChar = SerialBT.read();
        Serial.print("readInboundBT(): Received ");
        Serial.println(incomingChar, HEX);
        _buffer.push_back(incomingChar);
      }
    }

    // Discard packet if packet size is too large and not delimited.
    if (_buffer.size() == _bufferSize && !checkPacketDelimiter(_buffer, _packetDelimiter)) {
      _buffer.clear(); // Discard packet.
      _bufferReady = false;
      Serial.println("readInboundBT(): Packet invalid. Discarded.");
    } else {
      _bufferReady = true; // Buffer is valid.
      Serial.println("readInboundBT(): Packet ready for dispatch.");
    }
  }
}

bool checkPacketDelimiter(std::vector<uint8_t> buffer, uint8_t delimiter) {
  return buffer.size() < 1 ? false : _buffer.at(buffer.size()-1) == 0;
}

void dispatchInboundBT(void * parameters) {
  for(;;) {
    while (!_bufferReady) {
      delay(20);
    }
    
    if (_bufferReady) {
      // Apply COBS decoding on the packet buffer.
      uint8_t encoded[_buffer.size()];
      uint8_t decoded[sizeof(encoded)-2]; // Decoded COBS packet is 2 bytes shorter than the encoded COBS message.
      convertToArr(_buffer, encoded); // Convert the packet buffer to a static array.
      cobsDecode(encoded, _buffer.size(), decoded); // COBS decoding.
      flushBuffer(); // Flush packet buffer.
      
      // TODO: Implement protobuffer decoding.
      pb_istream_t istream;
      istream = pb_istream_from_buffer(decoded, sizeof(decoded));
      SensorResponse decoded_proto = SensorResponse_init_zero;
      pb_decode(&istream, SensorResponse_fields, &decoded_proto);
      Serial.println("Decoded protobuffer message:");
      Serial.println(decoded_proto.sensor_id);
      Serial.println(decoded_proto.sensor_value);

      String msg = "";
      if (msg.startsWith("R001temperature")) {
        // Write the temperature to the BT.
        String temperature = String(_currentTemp);
        sendStringBT(temperature);
        
      } else if (msg.startsWith("W002dutycycle")) {
        int index = 13; // Index where duty cycle value begins.
        String dutyCycle = msg.substring(index, msg.length());
        _dutyCycle = dutyCycle.toInt();
        String response = String(_dutyCycle);
        sendStringBT(response);
        Serial.println("Setting duty cycle: " + String(_dutyCycle));
      }
    }
  }
}

void convertToArr(std::vector<uint8_t> vec, uint8_t *out) {  
  for (int i = 0; i < vec.size(); i++) {
    out[i] = vec.at(i);
  }
}

void sendStringBT(String str) {
  for (int i = 0; i < str.length(); i++) {
    SerialBT.write(str.charAt(i));
  }
}

String parseBuffer(std::vector<uint8_t> buffer) {
  String out = "";
  for (int i = 0; i < buffer.size(); i++) {
    out = out + buffer.at(i);
  }
  return out;
}

String parseBuffer2(uint8_t *buffer, int size) {
  String out = "";
  for (int i = 0; i < size; i++) {
    out = out + "|" + buffer[i];
    Serial.println(buffer[i], HEX);
  }
  return out;
}

void flushBuffer() {
  _buffer.clear();
  _bufferReady = false;
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
