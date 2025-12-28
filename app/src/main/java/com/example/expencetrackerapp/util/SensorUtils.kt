package com.example.expencetrackerapp.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Global CompositionLocal for device rotation to avoid multiple sensor listeners.
 * Defaults to (0f, 0f) -> (Pitch, Roll)
 */
val LocalDeviceRotation = compositionLocalOf { 0f to 0f }

/**
 * Returns a state of (Pitch, Roll) of the device.
 * Pitch ranges roughly from -pi/2 to pi/2 (tilt up/down)
 * Roll ranges roughly from -pi to pi (tilt left/right)
 */
@Composable
fun rememberDeviceRotation(): State<Pair<Float, Float>> {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationState = remember { mutableStateOf(0f to 0f) }

    DisposableEffect(Unit) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR || 
                        it.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        
                        // orientation[1] is pitch, orientation[2] is roll
                        rotationState.value = orientation[1] to orientation[2]
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    
    return rotationState
}
