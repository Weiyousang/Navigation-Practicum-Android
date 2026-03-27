# Navigation Practicum: Indoor Positioning & Trajectory Mapping

## 專案簡介 (About)
本專案為「導航實習」課程的系列實作，主要探討並實作基於智慧型手機微機電感測器 (MEMS Sensors) 的室內定位與慣性導航功能。透過擷取 Android 裝置的加速度計與陀螺儀數據，實作計步演算法、長寬距離推算，並將移動軌跡視覺化，展示了感測器數據融合與座標轉換的實作能力。

## 技術與開發環境 (Tech Stack)
* **開發語言:** Kotlin
* **UI 框架:** Jetpack Compose
* **核心技術:** SensorManager (Accelerometer, Gyroscope) 、Canvas 2D 繪圖 、協程 (Coroutines) 

---

## 專案列表 (Projects Overview)

### 📁 1. 基礎計步器與直線測量 (Basic Pedometer & Distance)
* **說明:** 利用加速度計偵測使用者步伐，並推算直線移動距離。
* **技術重點:**
    * 擷取 Z 軸加速度 (Z-axis acceleration)，當數值大於設定門檻 (Threshold = 2.0) 時判定為一步。
    * 設定忽略數據的冷卻時間 (0.75秒)，避免單次震動造成重複計算。
    * 假設步長為 0.6 公尺，累加計算總移動距離 (Length/Width)。

### 📁 2. 陀螺儀轉向與室內長寬測量 (Gyroscope & Room Measurement)
* **說明:** 結合加速度計與陀螺儀，實作能在室內轉彎並自動切換長、寬測量的導航系統 。
* **技術重點:**
    * 透過角加速度判斷轉向，當 Z 軸旋轉率超過門檻時，自動切換「長度」與「寬度」的計算狀態 。
    * 實作 `PathCanvas` 組件，根據測量到的長寬數據，依比例動態繪製出移動路徑的矩形圖形 。

### 📁 3. 2D 軌跡座標轉換與繪製 (2D Trajectory Mapping)
* **說明:** 建立平面座標系統，即時追蹤並繪製任意方向的移動軌跡 。
* **技術重點:**
    * 將陀螺儀數據積分計算出當前偏航角 (Current Angle) 。
    * 偵測加速度計的三軸合力判定移動 (Shake detection) 。
    * 運用三角函數 (`sin`, `cos`) 將移動量轉換為 X 座標與 Y 座標的位移量 。
    * 使用 Compose `Canvas` 的 `drawLine` 功能，將軌跡點 (Trajectory points) 連線繪製出實際行走路線 。
