#include "max6675.h"
#include <LCD_I2C.h>

float startTime = 0;

LCD_I2C lcd(0x27, 16, 2); // Default address of most PCF8574 modules

int relayPin = 16;
int potPin = 26;

int thermoDO = 19;
int thermoCS = 23;
int thermoCLK = 5;
MAX6675 thermocouple(thermoCLK, thermoCS, thermoDO);

int potValue = 0;
float dutyCycle = 0;
float currentTemp = 0;
float currentError = 0;
float lastError = 0;
float cummulativeError = 0;
float rateOfChangeError = 0;

TaskHandle_t temperatureSensor_t;

void setup() {
  Serial.begin(115200);
  lcd.begin();
  lcd.backlight();
  pinMode(relayPin, OUTPUT);
  startTime = millis();

  xTaskCreatePinnedToCore(
      readTemp, /* Function to implement the task */
      "TemperatureSensor", /* Name of the task */
      10000,  /* Stack size in words */
      NULL,  /* Task input parameter */
      0,  /* Priority of the task */
      &temperatureSensor_t,  /* Task handle. */
      0); /* Core where the task should run */

  delay(1000);
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
  
    currentTemp = temperatureC / numReadings;
  }
}

void loop() {
  String out = "";
  potValue = analogRead(potPin);
  dutyCycle = (float(potValue) / 4096.0) * 100;
  out += String(dutyCycle);

  lcd.setCursor(0, 0);
  lcd.print("Set:" + String(int(dutyCycle)) + "%");

  out += ", " + String(currentTemp);
  lcd.setCursor(9, 0);
  lcd.print("T: " + String(int(currentTemp)) + "C");

  float delayMs = dutyCycle * 10;
  digitalWrite(relayPin, HIGH); // Turn on on heater
  delay(dutyCycle*10);
  if (delayMs > 0) {
    digitalWrite(relayPin, LOW); // Turn off heater
    delay(1000 - (dutyCycle*10));
  }

  float timePassedSeconds = (millis() - startTime) / 1000;
  int minutes = int(timePassedSeconds / 60);
  int seconds = int(timePassedSeconds) % 60;
  lcd.setCursor(0, 1);
  lcd.print("Time: " + String(minutes) + "m " + String(seconds) + "s");
  Serial.println(out);
}
