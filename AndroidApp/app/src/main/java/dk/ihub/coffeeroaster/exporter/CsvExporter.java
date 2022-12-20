package dk.ihub.coffeeroaster.exporter;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import dk.ihub.coffeeroaster.events.CoffeeRoasterEvent;

public class CsvExporter implements IExporter {

    private final String filename;
    private final Context ctx;
    private final StringBuilder sb;

    public CsvExporter(String filename, List<Pair<Duration, CoffeeRoasterEvent>> roastProfile, Context ctx) {
        this.filename = filename;
        this.ctx = ctx;

        sb = new StringBuilder();
        sb.append("Seconds[s];BeanTemperature[C];DutyCycle[%]\n");
        for (Pair<Duration, CoffeeRoasterEvent> durationCoffeeRoasterEventPair : roastProfile) {
            long seconds = durationCoffeeRoasterEventPair.first.getSeconds();
            float beanTemperatureCelsius = durationCoffeeRoasterEventPair.second.getBeanTemperatureCelsius();
            float dutyCycle = durationCoffeeRoasterEventPair.second.getDutyCycle();
            sb.append(String.format("%d;%f;%f\n", seconds, beanTemperatureCelsius, dutyCycle));
        }
    }

    @Override
    public void export() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
        Log.d("CsvExporter", "export: filename " + file.getAbsolutePath());

        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            stream.write(sb.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
