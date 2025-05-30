# Turbix – Product Requirements Document *(forked‑from Sensify)*

*A Lightweight Helicopter Turbulence Meter built on **JunkieLabs/sensify‑android***

**Document version:** 1.1
**Last updated:** 2025‑05‑30
**Author:** ChatGPT (with user inputs)
**Code base:** Fork of [JunkieLabs/sensify‑android]([github.com]%28https://github.com/JunkieLabs/sensify-android?utm_source=chatgpt.com%29) (MIT‑licensed)

---

## 0. Build Strategy

1. **Up‑stream fork** `github.com/JunkieLabs/sensify-android` tag `v2023.03.05` as `turbix‑android`.
2. Retain Sensify’s **MVVM + Jetpack Compose** scaffold and `SensorProvider` / `SensorPacketProvider` for raw IMU access.([github.com](https://github.com/JunkieLabs/sensify-android))
3. Replace the existing MPAndroidChart fragments with a bespoke **Turbulence UI** (see §6.2) while keeping the charting library for the FFT drawer.
4. Introduce a new Kotlin module `turbulence‑engine` (pure JVM) and wire it into Sensify’s `SensorViewModel` flow.
5. Maintain compatibility with Sensify’s CSV export format **until v2** to ease regression testing.
6. Contribute generic bug‑fixes back to upstream via PR when feasible.
7. Commit this PRD (`docs/PRD.md`) to the repository and update it alongside code changes (include PRD edits in relevant pull requests).

---

## 1. Vision

Give any helicopter pilot a **simple, reliable, offline tool**—running entirely on a smartphone—that tells *“How rough is the air right now?”* by removing rotor‑induced vibration and presenting a single turbulence score (0‑10) plus an intuitive colour bar.

*(Same as v1.0)*

---

## 2. Problem Statement

* Pilots still rely on subjective feel to gauge turbulence, which is unreliable and hard to quantify for flight‑test notes.
* Existing professional vibration kits are bulky, expensive, or require post‑processing.
* A phone IMU already sits in most cockpits but raw data is polluted by rotor harmonics.
* **Sensify already solves “visualise any sensor”**; Turbix specialises it for *accurate turbulence scoring*.

---

## 3. Target Users *(unchanged)*

| Persona               | Description                                    | Primary Need                                 |
| --------------------- | ---------------------------------------------- | -------------------------------------------- |
| **Flight‑test pilot** | Evaluates handling qualities of new rotorcraft | Objective turbulence index during manoeuvres |
| **Line pilot**        | Makes go/no‑go and comfort calls               | Quick glance severity indication             |
| **Aero‑eng student**  | Logs flight data for research                  | Easy CSV export                              |

---

## 4. Success Metrics

* **≤ 2 s latency** from accelerometer sample to UI update *(leveraging Sensify’s existing 16 ms sensor loop)*.
* **> 20 dB attenuation** at rotor fundamental & 1st harmonic.
* Pilot subjective rating correlates with app scale at **ρ ≥ 0.75** (Spearman) in validation flights.
* Battery drain **< 5 % / h** on a mid‑range 2024 Android phone (screen on).
* **< 10 % fork drift** from upstream after three release cycles (monitored via `git diff --stat`).

---

## 5. Assumptions & Constraints

* Device runs Android 11+ with accelerometer (≥ 100 Hz) & GNSS.
* App must work **100 % offline**; no runtime libraries pulled from cloud.
* UI must stay operable in sunlight, portrait or landscape, with gloves.
* All new code must remain **MIT‑compatible** to match upstream licence.
* Keep Sensify’s **Flow‑based** data stream conventions.

---

## 6. Functional Requirements

### 6.1 Real‑Time Turbulence Engine *(new module)*

| ID   | Requirement                                                                                                                        |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------- |
| FR‑1 | Subscribe to `SensorPacketProvider` for 3‑axis accelerometer at **configurable fs** (default = 100 Hz; user‑adjustable 10–200 Hz). |
| FR‑2 | Remove gravity vector with low‑pass filter (τ = 5 s) then compute body magnitude *a\_b*.                                           |
| FR‑3 | **Adaptive notch filter** centre = RPM / 60 × N\_blades; Q ≈ 25; update when ΔRPM > 5 rpm.                                         |
| FR‑4 | High‑pass 0.5 Hz to suppress manoeuvre bias.                                                                                       |
| FR‑5 | Sliding RMS window 2 s → turbulence metric *T*<sub>RMS</sub>.                                                                      |
| FR‑6 | Map *T*<sub>RMS</sub> → scale **S 0‑10** via calibrated break‑points (see §7).                                                     |
| FR‑7 | Long‑press screen **TARE** sets current *T* = 0 baseline (persists until app close).                                               |
| FR‑8 | If RPM input missing, skip FR‑3 and flag *reduced accuracy* banner.                                                                |

*Implementation note:* FR‑1..FR‑6 live inside `TurbulenceEngine.kt`; the ViewModel exposes a `StateFlow<UiState>` consumed by Compose just like Sensify’s chart screens.

### 6.2 User Interface *(extends Sensify’s Compose code)*

| ID   | Requirement                                                                                                     |
| ---- | --------------------------------------------------------------------------------------------------------------- |
| UI‑1 | Primary screen shows coloured bar (green‑yellow‑red) + large numeric **S** (0‑10).                              |
| UI‑2 | **Drawer** (swipe‑up): FFT strip 0‑50 Hz (reuse Sensify’s MPAndroidChart), raw vs. filtered traces, RPM status. |
| UI‑3 | **REC** button toggles logging; icon flashes while active (reuse Sensify’s FAB component for consistency).      |
| UI‑4 | Settings: Rotor RPM, Blade count, Sample rate, Theme (day/night).                                               |

### 6.3 Data Logging *(re‑uses Sensify’s CSV infra)*

| ID   | Requirement                                                                |
| ---- | -------------------------------------------------------------------------- |
| DL‑1 | On REC start, write header to `turbix_YYYYMMDD_HHMMSS.csv`.                |
| DL‑2 | Log rows: epoch ms, a\_x, a\_y, a\_z, a\_b, T\_RMS, S, RPM, lat, lon, alt. |
| DL‑3 | File stored in app‑private `/Android/data/com.turbix/files/logs/`.         |
| DL‑4 | Share sheet export after flight (opens system picker).                     |

---

## 7. Turbulence Scale Mapping

| *T*<sub>RMS</sub> (m/s²) | Scale S | Pilot perception  |
| -----------------------: | :-----: | ----------------- |
|                 0 – 0.30 |   0–1   | Glassy air        |
|              0.30 – 0.60 |   2–3   | Light chop        |
|              0.60 – 1.20 |   4–6   | Noticeable bumps  |
|              1.20 – 2.50 |   7–8   | Seat‑belt tight   |
|                   > 2.50 |   9–10  | Severe; limit ops |

*Initial numbers; refine with flight‑test regression.*

---

## 8. Non‑Functional Requirements

* **Performance:** ≤ 4 ms DSP per sample on Snapdragon 778G.
* **Reliability:** No crashes during > 2 h continuous run (Sensify baseline already stable).
* **Safety:** All interactions doable with one tap; no hidden multi‑step flows.
* **Privacy:** No personal data collected; logs remain local until user export.
* **Maintainability:** 90 % code covered by unit tests (DSP & mapping logic).
* **Legal:** Upheld MIT licence & upstream attribution in `about.md` screen.

---

## 9. Technical Architecture *(Follows Sensify’s MVVM)*

```mermaid
graph TD
    subgraph Android App
        A[Compose UI Layer] -->|StateFlow| B(ViewModel)
        B -->|push(accel, rpm)| C[TurbulenceEngine]
        C -- Metrics --> B
        D[SensorService (from Sensify)] --> C
    end
    style A fill:#ffc,stroke:#555
    style B fill:#cfc,stroke:#555
    style C fill:#ccf,stroke:#555
    style D fill:#eee,stroke:#555
```

* **Hot‑reload‑ready**: Each layer is a pure Kotlin class / composable, in line with Vibe Code and Sensify patterns.
* **Pluggable DSP**: `TurbulenceEngine` exposes `fun push(accel: Vec3, rpm: Float?): Result`.
* **Sensor bootstrapping** delegates to Sensify’s `SensorProvider`, reducing boilerplate.

---

## 10. Open Issues / Risks

| Risk                                              | Impact                      | Mitigation                                    |
| ------------------------------------------------- | --------------------------- | --------------------------------------------- |
| Fork drifts far, losing upstream fixes            | Security / perf regressions | Sync quarterly; keep changes modular          |
| Notch lag if RPM sensor unavailable               | False positives             | Manual RPM slider + fast UI warning           |
| Phone mounting orientation affects gravity filter | Scale skew                  | Use magnitude only; instruct to mount rigidly |
| Battery drain in older phones                     | User stops using            | Provide “low‑power 30 Hz” mode                |

---

## 11. Milestones & Timeline *(shifted by +1 week for upstream merge work)*

| Date         | Deliverable                                      |
| ------------ | ------------------------------------------------ |
| **T + 0 wk** | PRD sign‑off                                     |
| T + 3 wk     | Fork sync & DSP prototype validated on bench rig |
| T + 5 wk     | MVP UI & logging integration complete            |
| T + 7 wk     | Flight‑test build (β)                            |
| T + 9 wk     | Calibration flights & scale tuning               |
| T + 11 wk    | 1.0 release on internal Play Track               |

---

## 12. Future Enhancements

1. Gyro‑assisted discrimination (remove rotational accel).
2. Fleet cloud dashboard (heat‑map of turbulence routes).
3. ML‑based adaptive scale per helicopter type.
4. **Upstream merge plugin**: export generic *TurbulenceEngine* as Gradle dependency for other sensor apps.

---

### Appendix A – CSV Schema

```
timestamp_ms,a_x,a_y,a_z,a_b,t_rms,scale_s,rpm,lat,lon,alt
```

### Appendix B – Adaptive Notch Math

Biquad coefficients:
$\omega_0 = 2\pi f_n/f_s,\;\; \alpha = \sin\omega_0/(2Q)$
See \[RBJ 2012] equations 11‑14 for direct‑form I.

---

*End of document – v1.1 (forked edition)*
