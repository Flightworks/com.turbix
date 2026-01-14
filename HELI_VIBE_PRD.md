# Product Requirements Document: HeliVibe (One-Button Edition)

## 1. Executive Summary
**HeliVibe** is a high-performance, native Android utility for helicopter test pilots. It provides a real-time quantification of atmospheric turbulence by isolating it from the airframe's baseline mechanical vibration.

The application features a minimalist **"One-Button" interface**: the pilot stabilizes the aircraft in laminar conditions, presses **TARE**, and the app immediately displays a **Turbulence Rating (0–10)** derived from ISO 2631 comfort standards.

---

## 2. Core User Flow & Objective
* **Context:** Pilot is flying a test profile (e.g., Level Flight, $V_{ne}$, etc.).
* **Step 1 (Setup):** Pilot mounts the device securely to the instrument panel or kneeboard.
* **Step 2 (Baseline):** In steady, laminar air, the pilot presses the large **TARE** button. The app learns the "normal" vibration signature of the helicopter for that specific flight regime.
* **Step 3 (Monitoring):** As the flight progresses, the app displays a live score (0–10). A score of **0** means vibration is identical to the baseline; higher scores indicate external turbulence intensity.

---

## 3. Functional Requirements (FR)

| ID | Feature | Requirement Description |
| :--- | :--- | :--- |
| **FR1** | **Native Sensor Access** | Access `Sensor.TYPE_LINEAR_ACCELERATION` (gravity excluded) at the highest available sampling rate (Android `SENSOR_DELAY_FASTEST`, typically 100Hz–200Hz). |
| **FR2** | **Signal Buffering** | Maintain a circular buffer of the last $N$ seconds (default: 2.0s) of raw sensor data to compute sliding-window metrics. |
| **FR3** | **One-Button TARE** | Upon pressing **TARE**: <br>1. Calculate the RMS of the buffer ($RMS_{baseline}$).<br>2. Store this value as the zero-reference.<br>3. Provide haptic feedback to confirm. |
| **FR4** | **Delta Calculation** | Continuously compute live RMS ($RMS_{live}$). The turbulence metric is the positive deviation: <br>$$RMS_{\Delta} = \max(0, RMS_{live} - RMS_{baseline})$$. |
| **FR5** | **ISO 2631 Scaling** | Map the $RMS_{\Delta}$ (in $m/s^2$) to a linear 0–10 scale based on ISO 2631-1 "Comfort Reactions" (detailed in Section 5). |
| **FR6** | **UI Display** | Display the score as a single, large, high-contrast integer. Background color changes dynamically based on severity (Green/Amber/Red). |
| **FR7** | **Cockpit Constraints** | Enforce `KEEP_SCREEN_ON` (Wake Lock) active while the app is in the foreground. Support Dark Mode defaults. |

---

## 4. Technical Architecture

### 4.1 Sensor Strategy
To avoid complex filtering of gravity vectors, the app will use the Android **Linear Acceleration Sensor** (software sensor fusing accelerometer + gyroscope) to provide "pure" acceleration minus gravity.
* **Axis:** Magnitude vector ($|v| = \sqrt{x^2+y^2+z^2}$) is used to capture turbulence in any direction.

### 4.2 Signal Processing (DSP)
All processing occurs on a background thread (Kotlin Coroutines) to ensure the UI remains frozen at 60fps.

1.  **Sampling:** ~100Hz.
2.  **Windowing:** 1-second sliding window for RMS stability.
3.  **RMS Formula:**
    $$RMS = \sqrt{\frac{1}{n} \sum_{i=0}^{n} (x_i^2 + y_i^2 + z_i^2)}$$

---

## 5. Turbulence Rating Scale (ISO 2631 Adapted)
The 0–10 rating quantifies **added** vibration (turbulence) on top of the helicopter's baseline. We map the Delta RMS ($RMS_{\Delta}$) to the ISO 2631-1 comfort thresholds.

| Delta RMS ($m/s^2$) | Score | ISO 2631-1 Equivalent | App Color State |
| :--- | :--- | :--- | :--- |
| **< 0.1** | **0** | Not uncomfortable (Baseline) | **GREEN** |
| **0.1 – 0.3** | **1 – 2** | A little uncomfortable | **GREEN** |
| **0.3 – 0.5** | **3 – 4** | Fairly uncomfortable | **YELLOW** |
| **0.5 – 0.8** | **5 – 6** | Uncomfortable | **AMBER** |
| **0.8 – 1.6** | **7 – 8** | Very uncomfortable | **ORANGE** |
| **> 1.6** | **9 – 10** | Extremely uncomfortable | **RED** |

* **Scaling Logic:**
    * Linear interpolation between thresholds.
    * Example: A Delta RMS of **0.4 m/s²** results in a score of **3.5** (Rounded to **4**).

---

## 6. User Interface (UI) Design
* **Platform:** Jetpack Compose (Material 3).
* **Orientation:** Portrait Locked (optimized for kneeboard/phone holder).

### 6.1 Main Screen Layout
1.  **Header:**
    * Small Status Icon: "Sensor Active" (Pulse animation).
    * Debug Text (Tiny, optional): "Baseline: 0.X m/s²".
2.  **Central Gauge:**
    * **Big Number:** Takes up 50% of the screen height. Font: Monospace/Industrial.
    * **Background:** The entire screen background changes color lightly based on the score (Green -> Red gradient).
3.  **Footer (Control Area):**
    * **TARE Button:** Rectangular button taking up the bottom 25% of the screen.
    * **Label:** "TARE (SET BASELINE)".
    * **Behavior:** Long-press protection is **disabled** (instant press preferred for timing).

---

## 7. Performance & Safety
* **Battery Efficiency:** Sensor sampling should stop when the app is minimized (Lifecycle-aware components).
* **Start-up Time:** < 2 seconds from tap to ready-to-measure.
* **Permissions:** `HIGH_SAMPLING_RATE_SENSORS` permission declared in Manifest (Android 12+ requirement).

## 8. Roadmap (Future V2)
* **Sensitivity Toggle:** A hidden settings menu to adjust the ISO scaling factor (e.g., "High Sensitivity" for smooth VIP aircraft vs. "Low Sensitivity" for tactical/utility aircraft).
* **Logs:** Simple CSV export of the last 5 minutes of flight data.