package dk.ihub.coffeeroaster;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import dk.ihub.coffeeroaster.devices.ESP32Roaster;
import dk.ihub.coffeeroaster.events.CoffeeRoasterEvent;
import dk.ihub.coffeeroaster.devices.ICoffeeRoaster;
import dk.ihub.coffeeroaster.events.ICoffeeRoasterEventListener;
import dk.ihub.coffeeroaster.devices.RoasterEmulator;

public class MainActivity extends AppCompatActivity implements ICoffeeRoasterEventListener, SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener {

    private LineChart lineChart;
    private SeekBar dutyCycle;
    private Switch switchConnected;
    private ToggleButton tglTimer;
    private Button btnClear;
    private ICoffeeRoaster roaster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initChart();
        initButtons();

        this.roaster = new ESP32Roaster();
        this.roaster.subscribe(this);
    }

    private void initButtons() {
        this.dutyCycle = findViewById(R.id.seekBarDutyCycle);
        this.dutyCycle.setOnSeekBarChangeListener(this);
        this.dutyCycle.setEnabled(false);

        this.switchConnected = findViewById(R.id.switchConnect);
        this.switchConnected.setOnCheckedChangeListener(this);

        this.tglTimer = findViewById(R.id.tglTimer);
        this.tglTimer.setEnabled(false);

        this.btnClear = findViewById(R.id.btnClear);
        this.btnClear.setOnClickListener((listener) -> {
            if (lineChart.getData() != null) {
                lineChart.clear();
                initChart();
            }
        });
    }

    private void initChart() {
        this.lineChart = findViewById(R.id.lineChart);
        // no description text
        lineChart.getDescription().setEnabled(false);
        // enable touch gestures
        lineChart.setTouchEnabled(true);
        lineChart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        lineChart.setPinchZoom(true);

        // set an alternative background color
        lineChart.setBackgroundColor(Color.LTGRAY);

        // Init x-axis as time
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);

        // Init left y-axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.RED);
        leftAxis.setAxisMaximum(230f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);

        // Init right y-axis
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setTextColor(Color.BLUE);
        rightAxis.setAxisMaximum(100);
        rightAxis.setAxisMinimum(0);
        rightAxis.setDrawGridLines(false);
        rightAxis.setDrawZeroLine(false);
        rightAxis.setGranularityEnabled(false);

        // Make linechart update in real-time
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        // add empty data
        lineChart.setData(data);
        Legend l = lineChart.getLegend();
        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        // Create temperature dataset
        LineDataSet temperatureSet = new LineDataSet(null, "Temperature [C]");
        temperatureSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        temperatureSet.setColor(Color.RED);
        temperatureSet.setDrawCircles(false);
        temperatureSet.setLineWidth(2f);
        temperatureSet.setFillAlpha(65);
        temperatureSet.setFillColor(Color.RED);
        temperatureSet.setHighLightColor(Color.rgb(244, 117, 117));
        temperatureSet.setValueTextColor(Color.WHITE);
        temperatureSet.setValueTextSize(9f);
        temperatureSet.setDrawValues(false);
        // Add temperature dataset to chart data. This will be index 0.
        lineChart.getData().addDataSet(temperatureSet);

        // Create duty cyle dataset
        LineDataSet dutyCycleSet = new LineDataSet(null, "Duty cycle [%]");
        dutyCycleSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        dutyCycleSet.setColor(Color.BLUE);
        dutyCycleSet.setDrawCircles(false);
        dutyCycleSet.setLineWidth(2f);
        dutyCycleSet.setFillAlpha(65);
        dutyCycleSet.setFillColor(Color.BLUE);
        dutyCycleSet.setHighLightColor(Color.rgb(244, 117, 117));
        dutyCycleSet.setValueTextColor(Color.WHITE);
        dutyCycleSet.setValueTextSize(9f);
        dutyCycleSet.setDrawValues(false);
        // Add temperature dataset to chart data. This will be index 1.
        lineChart.getData().addDataSet(dutyCycleSet);

    }

    @Override
    public void handleEvent(CoffeeRoasterEvent event) {
        Log.d("MainActivity", String.format("Received event: %f [C]", event.getBeanTemperatureCelsius()));
        float t = event.getBeanTemperatureCelsius();
        float d = event.getDutyCycle();

        LineData data = lineChart.getData();
        ILineDataSet temperatureSet = data.getDataSetByIndex(0);
        data.addEntry(new Entry(temperatureSet.getEntryCount(), t), 0);

        ILineDataSet dutyCycleSet = data.getDataSetByIndex(0);
        data.addEntry(new Entry(dutyCycleSet.getEntryCount(), d), 1);


        // let the chart know it's data has changed
        data.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.invalidate(); // Enforce redraw
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int dutyCycle, boolean b) {
        this.roaster.setDutyCycle(dutyCycle);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean shouldConnect) {
        if (shouldConnect) {
            if (this.roaster.connect()) {
                this.dutyCycle.setEnabled(true);
                this.tglTimer.setEnabled(true);
            }
        } else {
            this.roaster.disconnect();
            this.dutyCycle.setEnabled(false);
            this.tglTimer.setEnabled(false);
        }
    }
}