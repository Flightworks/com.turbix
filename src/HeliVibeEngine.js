export class HeliVibeEngine {
  constructor() {
    this.bufferSize = 200; // 2 seconds at 100Hz
    this.buffer = [];
    this.state = {
      liveRms: 0,
      baselineRms: 0,
      deltaRms: 0,
      score: 0,
      colorState: 0, // 0: Green, 1: Yellow, 2: Amber, 3: Orange, 4: Red
    };
  }

  processSample(x, y, z) {
    const magnitude = Math.sqrt(x * x + y * y + z * z);

    // Add to buffer
    this.buffer.push(magnitude);
    if (this.buffer.length > this.bufferSize) {
      this.buffer.shift();
    }

    // Calculate RMS of buffer
    let sumSq = 0.0;
    for (const v of this.buffer) {
      sumSq += v * v;
    }

    const rms = this.buffer.length > 0 ? Math.sqrt(sumSq / this.buffer.length) : 0;

    const baseline = this.state.baselineRms;
    const delta = Math.max(0, rms - baseline);

    const score = this.calculateScore(delta);
    const color = this.calculateColor(score);

    this.state = {
      ...this.state,
      liveRms: rms,
      deltaRms: delta,
      score: score,
      colorState: color,
    };

    return this.state;
  }

  tare() {
    this.state.baselineRms = this.state.liveRms;
    return this.state;
  }

  calculateScore(delta) {
    let s = 0.0;
    if (delta < 0.1) {
      s = 0.0;
    } else if (delta < 0.3) {
      s = this.interpolate(delta, 0.1, 0.3, 1.0, 2.0);
    } else if (delta < 0.5) {
      s = this.interpolate(delta, 0.3, 0.5, 3.0, 4.0);
    } else if (delta < 0.8) {
      s = this.interpolate(delta, 0.5, 0.8, 5.0, 6.0);
    } else if (delta < 1.6) {
      s = this.interpolate(delta, 0.8, 1.6, 7.0, 8.0);
    } else {
      s = this.interpolate(delta, 1.6, 3.2, 9.0, 10.0);
    }

    // Round to nearest int and clamp 0-10
    return Math.min(10, Math.max(0, Math.round(s)));
  }

  interpolate(v, x0, x1, y0, y1) {
    return y0 + (v - x0) * (y1 - y0) / (x1 - x0);
  }

  calculateColor(score) {
    if (score <= 2) return 0; // Green
    if (score <= 4) return 1; // Yellow (Green in PRD for 1-2, Yellow for 3-4? Wait. PRD table says 1-2 is Green, 3-4 is Yellow.)
    // Kotlin code: 0,1,2 -> Green. 3,4 -> Yellow.
    // My code: <=2 -> 0. <=4 -> 1. Correct.
    if (score <= 6) return 2; // Amber
    if (score <= 8) return 3; // Orange
    return 4; // Red
  }
}
