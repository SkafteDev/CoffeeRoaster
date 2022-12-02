package dk.ihub.coffeeroaster.events;

public class ConnectionEvent {
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTED = 2;

    private final int connectionStatus;

    public ConnectionEvent(int connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }
}