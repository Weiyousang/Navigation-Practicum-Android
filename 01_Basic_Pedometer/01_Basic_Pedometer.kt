package com.example.myapplication

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    private var totalLength by mutableStateOf(0.0)
    private var totalWidth by mutableStateOf(0.0)
    private var stepCount by mutableStateOf(0)
    private val threshold = 2.0 // 設置門檻值
    private var ignoreData = false // 是否忽略後續數據
    private var calculatingWidth = false // 是否正在計算寬度

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        setContent {
            MyApplicationTheme {
                // 使用主題中的背景顏色的表面容器
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AccDistanceScreen(
                        totalLength = totalLength,
                        totalWidth = totalWidth,
                        stepCount = stepCount,
                        onButtonClick = {
                            calculatingWidth = true
                        }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(accListener)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            accListener,
            accelerometer,
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
    onButtonClick: () -> Unit
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
        Button(onClick = onButtonClick) {
            Text("Start Calculating Width")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        AccDistanceScreen(totalLength = 10.0, totalWidth = 5.0, stepCount = 15, onButtonClick = {})
    }
}


