package com.example.myapplication

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var gyroscope: Sensor
    private var totalLength by mutableStateOf(0.0)
    private var totalWidth by mutableStateOf(0.0)
    private var stepCount by mutableStateOf(0)
    private val threshold = 12.0f// 設置門檻值
    private val gyroThreshold = 4 // 角加速度門檻值，調整為更靈敏
    private var ignoreData = false // 是否忽略後續數據
    private var ignoreGyroData = false // 是否忽略角加速度數據
    private var calculatingWidth by mutableStateOf(false) // 是否正在計算寬度
    private var showPath by mutableStateOf(false) // 是否顯示路徑圖

    private val accListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor == accelerometer && !ignoreData) {
                val zAcceleration = event.values[2] // 只取得 Z 軸加速度
                if (zAcceleration > threshold) { // 當加速度超過門檻值
                    stepCount++
                    if (calculatingWidth) {
                        totalWidth += 0.60 // 每跳增加 60cm
                    } else {
                        totalLength += 0.60 // 每跳增加 60cm
                    }
                    ignoreData = true // 設置忽略後續數據的標誌
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(750) // 0.75 秒後重置忽略標誌
                        ignoreData = false
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor == gyroscope && !ignoreGyroData) {
                val zRotationRate = event.values[2] // 只取得 Z 軸角加速度
                if (kotlin.math.abs(zRotationRate) > gyroThreshold) {
                    calculatingWidth = !calculatingWidth
                    ignoreGyroData = true // 設置忽略後續數據的標誌
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(900) // 0.9 秒後重置忽略標誌
                        ignoreGyroData = false
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AccDistanceScreen(
                        totalLength = totalLength,
                        totalWidth = totalWidth,
                        stepCount = stepCount,
                        calculatingWidth = calculatingWidth,
                        onShowPathClick = {
                            showPath = true
                        },
                        showPath = showPath
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(accListener)
        sensorManager.unregisterListener(gyroListener)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            accListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            gyroListener,
            gyroscope,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }
}

@Composable
fun AccDistanceScreen(
    modifier: Modifier = Modifier,
    totalLength: Double,
    totalWidth: Double,
    stepCount: Int,
    calculatingWidth: Boolean,
    onShowPathClick: () -> Unit,
    showPath: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step: $stepCount",
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Length: %.2f meters".format(totalLength),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Width: %.2f meters".format(totalWidth),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (calculatingWidth) "Measuring Width" else "Measuring Length",
            color = if (calculatingWidth) Color.Green else Color.Blue,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onShowPathClick) {
            Text("Show Path")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (showPath) {
            PathCanvas(totalLength = totalLength, totalWidth = totalWidth)
        }
    }
}

@Composable
fun PathCanvas(totalLength: Double, totalWidth: Double) {
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(300.dp)
        .padding(16.dp)) {
        val rectWidth = size.width * 0.8f
        val rectHeight = size.height * 0.8f
        val left = (size.width - rectWidth) / 2
        val top = (size.height - rectHeight) / 2
        val right = left + rectWidth
        val bottom = top + rectHeight

        drawRect(
            color = Color.Gray,
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
            style = Stroke(width = 5f)
        )

        val scaleX = rectWidth / totalLength.toFloat()
        val scaleY = rectHeight / totalWidth.toFloat()
        val rectScale = minOf(scaleX, scaleY)

        val scaledLength = totalLength.toFloat() * rectScale
        val scaledWidth = totalWidth.toFloat() * rectScale

        drawRect(
            color = Color.Blue,
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(scaledLength, scaledWidth),
            style = Stroke(width = 5f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        AccDistanceScreen(
            totalLength = 10.0,
            totalWidth = 5.0,
            stepCount = 15,
            calculatingWidth = false,
            onShowPathClick = {},
            showPath = false
        )
    }
}
