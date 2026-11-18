package uws.ac.uk.bookmark;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class AmbientLightHelper implements SensorEventListener {

    public interface LightChangeListener {
        void onLightChanged(float lux);
    }

    private final SensorManager sensorManager;
    private final Sensor lightSensor;
    private final LightChangeListener listener;

    public AmbientLightHelper(Context context, LightChangeListener listener) {
        this.listener = listener;

        sensorManager = (SensorManager)
                context.getSystemService(Context.SENSOR_SERVICE);

        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    public boolean isSensorAvailable() {
        return lightSensor != null;
    }

    public void start() {
        if (lightSensor != null) {
            sensorManager.registerListener(
                    this,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) {
            return;
        }

        float lux = event.values[0];

        // log the ambient light reading for testing
        Log.d("AmbientLight", "Light level: " + lux + " lux");

        listener.onLightChanged(lux);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
