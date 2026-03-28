package com.example.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlin.math.*

class MainActivity : ComponentActivity() {
   private var counter by mutableStateOf(0)
   private var currentAngle by mutableStateOf(0f)
   private var accumulatedRotation = 0f
   private var baseAngle = 0f
   private lateinit var sensorManager: SensorManager
   private var sensorEventListener: SensorEventListener? = null
   private var lastTimestamp = 0L

   private var xPosition by mutableStateOf(0f)
   private var yPosition by mutableStateOf(0f)
   private val trajectory = mutableStateListOf<Pair<Float, Float>>()

   private var distance0 by mutableStateOf(0f)
   private var distance90 by mutableStateOf(0f)
   private var distance180 by mutableStateOf(0f)
   private var distance270 by mutableStateOf(0f)

   private var accelerometerEventListener: SensorEventListener? = null

   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)

       sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

       sensorEventListener = object : SensorEventListener {
           override fun onSensorChanged(event: SensorEvent) {
               val dt = if (lastTimestamp != 0L) (event.timestamp - lastTimestamp) / 1_000_000_000.0f else 0f
               lastTimestamp = event.timestamp

               val rotationSpeed = Math.toDegrees(event.values[1].toDouble()).toFloat()
               val rotationAngle = rotationSpeed * dt
               accumulatedRotation += rotationAngle
               currentAngle += rotationAngle

               if (abs(currentAngle - baseAngle) >= 360) {
                   if (currentAngle - baseAngle > 0) {
                       counter--
                   } else {
                       counter++
                   }
                   baseAngle = currentAngle
               }
           }

           override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
       }

       val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
       if (gyroSensor != null) {
           sensorManager.registerListener(sensorEventListener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL)
       } else {
           Toast.makeText(this, "Device does not support gyroscope", Toast.LENGTH_LONG).show()
           finish()
       }

       accelerometerEventListener = object : SensorEventListener {
           override fun onSensorChanged(event: SensorEvent) {
               val x = event.values[0]
               val y = event.values[1]
               val z = event.values[2]

               val shake = sqrt((x * x + y * y + z * z).toDouble()) > 10

               if (shake) {
                   val distance = 0.06f
                   val radianAngle = Math.toRadians(abs(currentAngle).toDouble())
                   xPosition += (distance * cos(radianAngle)).toFloat()
                   yPosition += (distance * sin(radianAngle)).toFloat()

                   trajectory.add(Pair(xPosition, yPosition))

                   when {
                       abs(currentAngle) in 0.0..45.0 || abs(currentAngle) in 315.0..360.0 -> distance0 += distance
                       abs(currentAngle) in 45.0..135.0 -> distance90 += distance
                       abs(currentAngle) in 135.0..225.0 -> distance180 += distance
                       abs(currentAngle) in 225.0..315.0 -> distance270 += distance
                   }
               }
           }

           override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
       }


       val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
       if (accelerometerSensor != null) {
           sensorManager.registerListener(accelerometerEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
       } else {
           Toast.makeText(this, "Device does not support accelerometer", Toast.LENGTH_LONG).show()
           finish()
       }

       setContent {
           MyApplicationTheme {
               MainContent()
           }
       }
   }

   @Composable
   fun MainContent() {
       Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
           Column {
               Text(text = "Current Angle: $currentAngle°")
               Text(text = "Side A : $distance0 m")
               Text(text = "Side B : $distance90 m")
               Text(text = "Side C : $distance180 m")
               Text(text = "Side D : $distance270 m")
               Text(text = "Position: (x: $xPosition, y: $yPosition)")

               Button(onClick = {
                   counter = 0
                   accumulatedRotation = 0f
                   currentAngle = 0f
                   baseAngle = 0f
                   xPosition = 0f
                   yPosition = 0f
                   distance0 = 0f
                   distance90 = 0f
                   distance180 = 0f
                   distance270 = 0f
                   trajectory.clear()
               }) {
                   Text(text = "Reset All")
               }
               Canvas(modifier = Modifier
                   .fillMaxSize()
                   .padding(16.dp)
                   .weight(1.5f)) {
                   drawTrajectory(trajectory)
               }

           }
       }
   }

   private fun DrawScope.drawTrajectory(trajectory: List<Pair<Float, Float>>) {
       val scale = 50 // 增加縮放比例以放大路徑
       for (i in 1 until trajectory.size) {
           val (x1, y1) = trajectory[i - 1]
           val (x2, y2) = trajectory[i]
           drawLine(
               color = Color.Blue,
               start = Offset(x1 * scale + size.width / 2, y1 * scale + size.height / 2),
               end = Offset(x2 * scale + size.width / 2, y2 * scale + size.height / 2),
               strokeWidth = 5f,
               cap = Stroke.DefaultCap
           )
       }
   }

   override fun onDestroy() {
       super.onDestroy()
       sensorEventListener?.let { sensorManager.unregisterListener(it) }
       accelerometerEventListener?.let { sensorManager.unregisterListener(it) }
   }
}


