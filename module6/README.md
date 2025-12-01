# Module 6: Capstone (The Ultimate Battle)

This module is the culmination of your Java 25 Mastery. It combines **Virtual Threads (Project Loom)** and **Vector API (Project Panama)** to create a high-performance "Risk Analysis Server" simulation.

## 1. The Benchmarks

We have built three advanced tools to stress-test the JVM.

### Tool A: `RiskAnalysisServer` (The 3x3 Matrix)
Tests the interaction between Concurrency (Low/Med/High) and Data Size (Small/Med/Large).
- **Goal:** Prove that Modern Java scales linearly while Legacy Java hits a "Thread Wall."
- **Run:**
  ```bash
  javac --add-modules jdk.incubator.vector module6/RiskAnalysisServer.java
  java --add-modules jdk.incubator.vector module6.RiskAnalysisServer
  ```

### Tool B: `OptimizationSweeper` (Throughput & Latency)
Sweeps through parameters to find the exact "saturation point" of your hardware.
- **Goal:** Measure `Req/s` (Throughput), `GigaOps/s` (Compute Power), and `P99 Latency`.
- **Key Insight:** Shows that Modern Java delivers 10x-50x lower latency at high concurrency.
- **Run:**
  ```bash
  javac --add-modules jdk.incubator.vector module6/OptimizationSweeper.java
  java --add-modules jdk.incubator.vector module6.OptimizationSweeper
  ```

### Tool C: `GCOptimizationSweeper` (Garbage Collection Battle)
Adds memory pressure (10KB allocation per request) to test the Garbage Collector.
- **Goal:** Compare G1, ZGC, and Parallel GC under heavy load.
- **Run (G1 - Default):**
  ```bash
  java --add-modules jdk.incubator.vector -XX:+UseG1GC module6.GCOptimizationSweeper
  ```
- **Run (ZGC - Low Latency):**
  ```bash
  java --add-modules jdk.incubator.vector -XX:+UseZGC module6.GCOptimizationSweeper
  ```
- **Run (Parallel - High Throughput):**
  ```bash
  java --add-modules jdk.incubator.vector -XX:+UseParallelGC module6.GCOptimizationSweeper
  ```

## 2. Expected Results

| Metric | Legacy (Threads + Scalar) | Modern (Virtual + Vector) | Speedup |
| :--- | :--- | :--- | :--- |
| **Throughput** | Caps at ~100k req/s | Scales to ~2M req/s | **10x - 20x** |
| **Compute** | Caps at ~15 GigaOps | Peaks at ~130 GigaOps | **9x** |
| **Latency (P99)** | Spikes to >100ms | Stays <5ms | **50x Drop** |

## 3. Conclusion

By combining:
1.  **Virtual Threads** (to eliminate waiting overhead)
2.  **Vector API** (to execute math 4x-16x faster)
3.  **ZGC** (to eliminate GC pauses)

You achieve the "Holy Grail" of backend engineering: **High Throughput AND Low Latency.**
