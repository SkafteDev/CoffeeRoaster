package dk.ihub.coffeeroaster;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import dk.ihub.coffeeroaster.devices.ESP32BluetoothRoaster;
import dk.ihub.coffeeroaster.devices.RoasterEmulator;
import dk.ihub.coffeeroaster.events.CoffeeRoasterEvent;
import dk.ihub.coffeeroaster.devices.ICoffeeRoaster;
import dk.ihub.coffeeroaster.events.ConnectionEvent;
import dk.ihub.coffeeroaster.events.ICoffeeRoasterConnectionListener;
import dk.ihub.coffeeroaster.events.ICoffeeRoasterEventListener;

public class MainActivity extends AppCompatActivity implements ICoffeeRoasterEventListener, ICoffeeRoasterConnectionListener, SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener {

    private LineChart lineChart;
    private SeekBar dutyCycle;
    private Switch switchConnected;
    private ToggleButton tglTimer;
    private TextView lblDutyCycleVal;
    private TextView lblTemperatureVal;
    private Button btnClear;
    private ICoffeeRoaster roaster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initChart();
        initButtons();

        this.roaster = new ESP32BluetoothRoaster(ESP32BluetoothRoaster.DEFAULT_ESP32_BT_ADDRESS);
        this.roaster.addConnectionListener(this);
        this.roaster.subscribe(this);
    }

    private void initButtons() {
        this.dutyCycle = findViewById(R.id.seekBarDutyCycle);
        this.dutyCycle.setOnSeekBarChangeListener(this);
        this.dutyCycle.setEnabled(false);

        this.switchConnected = findViewById(R.id.switchConnect);
        this.switchConnected.setOnCheckedChangeListener(this);

        this.lblDutyCycleVal = findViewById(R.id.lblDutyCycleVal);
        this.lblTemperatureVal = findViewById(R.id.lblTemperatureVal);

        this.tglTimer = findViewById(R.id.tglTimer);
        this.tglTimer.setEnabled(false);
        this.tglTimer.setVisibility(TextView.INVISIBLE);

        this.btnClear = findViewById(R.id.btnClear);
        this.btnClear.setOnClickListener((listener) -> {
            if (lineChart.getData() != null) {
                // TODO: BUG multiple event listeners are added when the data are cleared.
                // Make sure that the old event listeners are removed when clearing data.
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
        lineChart.setBackgroundColor(Color.DKGRAY);

        // Init x-axis as time
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);

        // Init left y-axis
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setTextColor(Color.RED);
        rightAxis.setAxisMaximum(230f);
        rightAxis.setAxisMinimum(0f);
        rightAxis.setDrawGridLines(true);
        rightAxis.setGranularityEnabled(true);

        // Init right y-axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(100);
        leftAxis.setAxisMinimum(0);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setGranularityEnabled(false);

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
        temperatureSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
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
        dutyCycleSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dutyCycleSet.setColor(Color.WHITE);
        dutyCycleSet.setDrawCircles(false);
        dutyCycleSet.setLineWidth(2f);
        dutyCycleSet.setFillAlpha(65);
        dutyCycleSet.setFillColor(Color.WHITE);
        dutyCycleSet.setHighLightColor(Color.rgb(244, 117, 117));
        dutyCycleSet.setValueTextColor(Color.WHITE);
        dutyCycleSet.setValueTextSize(9f);
        dutyCycleSet.setDrawValues(false);
        // Add temperature dataset to chart data. This will be index 1.
        lineChart.getData().addDataSet(dutyCycleSet);

    }

    @Override
    public void onRoasterEvent(CoffeeRoasterEvent event) {
        Log.d("MainActivity", String.format("Received event: %f [C]", event.getBeanTemperatureCelsius()));
        float t = event.getBeanTemperatureCelsius();
        float d = event.getDutyCycle();

        lblTemperatureVal.setText(String.format("%s C", String.valueOf(t)));
        lblDutyCycleVal.setText(String.valueOf(d));

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
    public void onProgressChanged(SeekBar seekBar, int dutyCycle, boolean fromUser) { }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        this.roaster.setDutyCycle(seekBar.getProgress());
    }

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

    @Override
    public void onConnectionStateChanged(ConnectionEvent event) {
        Handler handler = new Handler(Looper.getMainLooper());
        switch (event.getConnectionStatus()) {
            case ConnectionEvent.STATE_CONNECTED:
                handler.post(() -> switchConnected.setChecked(true));
                break;
            case ConnectionEvent.STATE_DISCONNECTED:
                handler.post(() -> switchConnected.setChecked(false));
                break;
        }
    }
}