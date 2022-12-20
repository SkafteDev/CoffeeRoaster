package dk.ihub.coffeeroaster.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Timer {

    private Runnable timer;
    private Thread timerThread;
    private Duration duration;
    private List<OnTickListener> onTickListeners;

    public Timer() {
        this.duration = Duration.ofSeconds(0);

        timer = () -> {
            long startTime = System.currentTimeMillis();
            while (true) {
                try {
                    long elapsed = System.currentTimeMillis() - startTime;
                    this.duration = Duration.ofMillis(elapsed);
                    notifyOnTickListeners(this.duration);

                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        };

        timerThread = new Thread(timer);

        onTickListeners = new ArrayList<>();
    }

    public void start() {
        timerThread = new Thread(timer);
        timerThread.start();
    }

    public void stop() {
        timerThread.interrupt();
    }

    public void restart() {
        stop();
        start();
    }

    public Duration getDuration() {
        return this.duration;
    }

    private void notifyOnTickListeners(Duration duration) {
        for (OnTickListener onTickListener : onTickListeners) {
            onTickListener.onTick(duration);
        }
    }

    public void setOnTickListener(OnTickListener listener) {
        if (onTickListeners.contains(listener)) return;

        onTickListeners.add(listener);
    }

    public interface OnTickListener {
        void onTick(Duration d);
    }
}
