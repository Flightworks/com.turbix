import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View, TouchableOpacity, Dimensions } from 'react-native';
import { DeviceMotion } from 'expo-sensors';
import { useKeepAwake } from 'expo-keep-awake';
import { useState, useEffect, useRef } from 'react';
import { HeliVibeEngine } from './src/HeliVibeEngine';

const COLORS = [
  '#4CAF50', // 0: Green
  '#FFEB3B', // 1: Yellow
  '#FFC107', // 2: Amber
  '#FF9800', // 3: Orange
  '#F44336', // 4: Red
];

const COLOR_NAMES = ['GREEN', 'YELLOW', 'AMBER', 'ORANGE', 'RED'];

export default function App() {
  useKeepAwake();

  const [data, setData] = useState({
    score: 0,
    liveRms: 0,
    baselineRms: 0,
    colorState: 0
  });

  const engineRef = useRef(new HeliVibeEngine());

  useEffect(() => {
    // Set update interval to ~100Hz (10ms)
    DeviceMotion.setUpdateInterval(10);

    const subscription = DeviceMotion.addListener((event) => {
      // event.acceleration provides linear acceleration (without gravity) in m/s^2 on Android/iOS (mostly)
      // Check if acceleration is present
      const { x, y, z } = event.acceleration || { x: 0, y: 0, z: 0 };

      // If acceleration is null (e.g. web or unavailable), we might try accelerationIncludingGravity - gravity
      // but let's assume it works for now.

      if (x !== null) {
        const newState = engineRef.current.processSample(x, y, z);

        // Throttle state updates to ~30Hz to keep UI responsive
        // Or just update every time? 100Hz might be too much for setState.
        // Let's rely on React batching or simple throttling logic if needed.
        // For now, let's try direct update, if it lags we can optimize.
        // Actually, let's throttle to every 5th frame (20Hz).
        if (Math.random() < 0.2) {
           setData({...newState});
        }
      }
    });

    return () => {
      subscription.remove();
    };
  }, []);

  const handleTare = () => {
    const newState = engineRef.current.tare();
    setData({...newState});
  };

  const backgroundColor = COLORS[data.colorState];
  const textColor = data.colorState === 1 || data.colorState === 2 ? '#000' : '#FFF'; // Dark text for Yellow/Amber

  return (
    <View style={[styles.container, { backgroundColor }]}>
      <StatusBar style="auto" />

      <View style={styles.header}>
        <Text style={[styles.statusText, { color: textColor }]}>SENSOR ACTIVE</Text>
        <Text style={[styles.debugText, { color: textColor }]}>
          Baseline: {data.baselineRms.toFixed(2)} m/s² | Live: {data.liveRms.toFixed(2)} m/s²
        </Text>
      </View>

      <View style={styles.gauge}>
        <Text style={[styles.scoreText, { color: textColor }]}>{data.score}</Text>
        <Text style={[styles.label, { color: textColor }]}>TURBULENCE</Text>
      </View>

      <TouchableOpacity style={styles.tareButton} onPress={handleTare} activeOpacity={0.7}>
        <Text style={styles.tareButtonText}>TARE (SET BASELINE)</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 60,
  },
  header: {
    alignItems: 'center',
  },
  statusText: {
    fontSize: 16,
    fontWeight: 'bold',
    opacity: 0.8,
  },
  debugText: {
    fontSize: 12,
    marginTop: 5,
    opacity: 0.6,
    fontFamily: 'monospace',
  },
  gauge: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  scoreText: {
    fontSize: 160,
    fontWeight: '900',
    fontVariant: ['tabular-nums'],
  },
  label: {
    fontSize: 24,
    fontWeight: 'bold',
    marginTop: -20,
    opacity: 0.8,
  },
  tareButton: {
    backgroundColor: 'rgba(0,0,0,0.3)',
    paddingVertical: 30,
    paddingHorizontal: 40,
    borderRadius: 16,
    width: '80%',
    alignItems: 'center',
  },
  tareButtonText: {
    color: '#FFF',
    fontSize: 20,
    fontWeight: 'bold',
    letterSpacing: 1,
  },
});
