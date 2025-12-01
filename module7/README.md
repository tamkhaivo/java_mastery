# Module 7: Advanced GC Optimizations & Stability

This module explores the critical metrics for long-running Java systems: **Latency**, **Throughput**, and **Memory Footprint**. It demonstrates how Garbage Collection (GC) impacts these metrics and how to define "stability" for a production system.

---

# The Architect’s Handbook to Java Garbage Collection
**Exhaustive Argumentation for Throughput, Latency, and Footprint Analysis**

## 1. Introduction: The Trilemma of Managed Runtime Performance

The optimization of the Java Virtual Machine (JVM) is fundamentally an exercise in navigating a computational trilemma: the tension between **Throughput** (the raw volume of work performed), **Latency** (the responsiveness of individual transactions), and **Memory Footprint** (the operational cost of physical resources). For nearly three decades, the JVM has evolved from a simple stack-based machine into a sophisticated, adaptive runtime environment capable of managing terabytes of heap memory with sub-millisecond pauses. However, the default "ergonomics"—the JVM's internal heuristics for self-tuning—are designed for a generalized "average" case that rarely aligns perfectly with the extremes of high-frequency trading, massive batch processing, or microservices architecture.

With the release of Java 21, the landscape of Garbage Collection (GC) has matured significantly. The bifurcation of collectors into distinct categories—Throughput-oriented (Parallel), Latency-oriented (ZGC, Shenandoah), and Balanced (G1)—requires engineers to adopt a precise, argument-driven methodology for testing and verification. One cannot simply "turn on" a collector and expect optimal results; one must instrument the runtime to expose its internal decision-making processes.

This report serves as an exhaustive reference for the JVM arguments necessary to rigorously test and profile the four pillars of performance: Throughput, Latency, Memory Footprint, and GC Times. It synthesizes the latest mechanisms available in OpenJDK 21, while retaining critical context for the widely deployed Java 11 and 17 environments. The analysis moves beyond surface-level flag descriptions to explore the second-order effects of configuration, detailing how specific arguments influence the delicate balance of object allocation, promotion, and reclamation.

## 2. The Lens of Observability: The Unified Logging Framework

To test performance parameters effectively, one must first establish a high-fidelity feedback loop. In the era of Java 8, logging was controlled by a disparate set of flags (e.g., `-XX:+PrintGCDetails`, `-XX:+PrintGCDateStamps`) that lacked coherence and granularity. With the introduction of the Unified Logging Framework (Xlog) in JDK 9, the JVM provided a structured, tag-based system for telemetry. Understanding Xlog is the prerequisite for all subsequent analysis, as it is the only mechanism capable of providing the data resolution needed to distinguish between application stalls and GC pauses.

### 2.1. The Taxonomy of Unified Logging Tags

The syntax `-Xlog:<tags>*=<level>:<output>:<decorators>` offers infinite combinatorics, but for performance testing, specific tag combinations are non-negotiable. The wildcard selector (`*`) is particularly powerful, as it enables all sub-tags associated with a primary category, ensuring that obscure phases of the GC cycle are not omitted from the analysis.

#### 2.1.1. Garbage Collection Tags for Timing Analysis

To exhaustively test GC times, the simple `gc` tag is insufficient. It provides only the high-level start and end times of a collection. For rigorous profiling, one must enable `gc+phases` and `gc+cpu`.

The `gc+phases` tag decomposes the collection into its constituent micro-operations—such as "External Root Scanning," "Code Root Marking," and "Object Copying". This decomposition is vital for identifying bottlenecks. For instance, if testing reveals that "External Root Scanning" is dominating the pause time, it suggests that the application has too many threads or a bloated JNI usage, rather than a heap sizing issue.

The `gc+cpu` tag adds a layer of operating system context, reporting the user, system, and real time consumed by the GC threads. This is critical for Throughput testing. A discrepancy where "Real" time is significantly higher than "User/Sys" time implies that the GC threads are being starved of CPU cycles by other processes or are blocked on I/O, a finding that invalidates any heap tuning efforts until the resource contention is resolved.

#### 2.1.2. Safepoint Logging for Latency Attribution

A common methodological error in latency testing is attributing all application pauses to Garbage Collection. The JVM must stop application threads at "safepoints" for various system operations, including biased locking revocation, class redefinition, and code deoptimization. Without logging these events, non-GC pauses will be misdiagnosed as GC latency spikes.

The flag `-Xlog:safepoint` (replacing the legacy `PrintGCApplicationStoppedTime`) reports two critical metrics: "Time to Safepoint" (TTSP) and the operation duration. TTSP measures how long it takes for all threads to agree to stop. If a test run shows low GC times but high application latency, and the safepoint log reveals high TTSP, the culprit is likely an application thread executing a long "counted loop" (e.g., a massive array copy or cryptographic calculation) without polling for safepoint requests. This insight allows the developer to refactor code rather than futilely tuning the GC.

### 2.2. Migration Matrix: Legacy to Unified Logging

For teams upgrading benchmarks from older JDKs, accurate translation of logging arguments is essential to maintain data continuity. The following table maps the critical legacy flags to their modern Xlog equivalents.

| Legacy Flag (JDK 8) | Unified Logging Argument (JDK 17/21) | Diagnostic Purpose |
| :--- | :--- | :--- |
| `-XX:+PrintGCDetails` | `-Xlog:gc*` | Enables detailed breakdown of GC phases and heap transitions. |
| `-XX:+PrintGCDateStamps` | Decorator: `time` | Absolute timestamps for correlating with external monitoring systems. |
| `-XX:+PrintGCTimeStamps` | Decorator: `uptime` | Relative time since JVM start, useful for aligning with load test phases. |
| `-XX:+PrintGCApplicationStoppedTime` | `-Xlog:safepoint` | Captures STW pauses caused by non-GC VM operations. |
| `-XX:+PrintTenuringDistribution` | `-Xlog:gc+age=trace` | Visualizes object aging in Survivor spaces to tune promotion thresholds. |
| `-XX:+PrintAdaptiveSizePolicy` | `-Xlog:gc+ergo*=trace` | Exposes the "why" behind ergonomic decisions (e.g., resizing generations). |
| `-XX:+PrintReferenceGC` | `-Xlog:gc+ref=debug` | Tracks time spent processing Soft/Weak/Phantom references. |

### 2.3. The Exhaustive Logging Configuration

For a definitive test run intended to capture all aspects of Throughput, Latency, and GC mechanics without overwhelming the I/O subsystem, the following argument string is recommended:

```bash
-Xlog:gc*,gc+ref=debug,gc+phases=debug,gc+age=trace,safepoint:file=gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M
```

This configuration directs output to a rotating file set (`gc-%t.log`, where `%t` is the timestamp), preventing the standard output blocking that can artificially throttle high-throughput applications. It captures the full lifecycle of memory: allocation, aging in survivor spaces (`gc+age`), reference processing (`gc+ref`), phase execution (`gc+phases`), and the overarching safepoint behavior (`safepoint`).

## 3. Throughput Testing: The Parallel Collector Architecture

When the testing objective is to maximize Throughput—defined as the ratio of time spent executing application code to total runtime—the Parallel Garbage Collector remains the gold standard. Unlike concurrent collectors that trade CPU cycles for lower pause times, the Parallel collector (enabled via `-XX:+UseParallelGC`) utilizes a deterministic, stop-the-world approach that is highly cache-efficient and minimizes the total number of CPU instructions required to reclaim memory.

### 3.1. Mathematical Tuning of Throughput Targets

The Parallel collector operates on a sophisticated control theory loop controlled principally by the Garbage Collection Time Ratio. This provides a direct mechanism to specify the throughput requirement as a mathematical goal.

The flag `-XX:GCTimeRatio=<N>` defines the target throughput using the formula:

`Throughput Goal = 1 / (1 + N)`

By default, `N=99`, implying a goal of 1% GC time and 99% application time. During throughput testing, manipulating this variable forces the JVM to aggressively resize the heap and generations to meet the target.

**Testing Scenario**: If an application is failing to meet throughput SLAs, decreasing `N` (e.g., to 19, allowing 5% GC time) can paradoxically increase actual application throughput. A relaxed goal allows the collector to perform fewer, larger collections, which are often more efficient per byte collected than frequent, constrained collections.

**Insight**: The interaction between `GCTimeRatio` and heap size is governed by `MinHeapFreeRatio` and `MaxHeapFreeRatio`. If the `GCTimeRatio` cannot be met, the JVM will expand the heap until it hits `-Xmx`. Therefore, measuring the actual memory footprint against the `GCTimeRatio` setting provides deep insight into the "cost" of throughput.

### 3.2. Ergonomics and Policy Weighting

The Parallel collector's adaptability is controlled by `-XX:AdaptiveSizePolicyWeight=<N>`. This argument determines how much historical weight is given to previous GC cycles when calculating the size of the next generation.

**Mechanism**: A higher weight makes the collector more resistant to transient spikes in allocation, while a lower weight makes it highly reactive.

**Testing Implication**: In "bursty" workloads (e.g., batch processing of uneven file sizes), the default weighting might cause the heap to oscillate inefficiently. Testing with varying weights allows engineers to dampen this oscillation, stabilizing the memory footprint and throughput.

### 3.3. Thread Parallelism and CPU Saturation

The argument `-XX:ParallelGCThreads=<N>` strictly controls the number of worker threads used during the Stop-The-World phases. While the ergonomics defaults this to the number of available cores (or a fraction thereof for high-core machines), explicit definition is mandatory for containerized testing.

**Oversubscription Risk**: In a container restricted to 4 CPUs but running on a 64-core host, the JVM might default to dozens of threads, leading to massive context-switching overhead that destroys throughput. Explicitly setting `-XX:ParallelGCThreads` to match the active processor count is the only way to obtain valid throughput data in Docker/Kubernetes environments.

## 4. Latency Engineering: The G1 Collector

For the majority of general-purpose applications, Latency—specifically the reduction of tail latency (p99)—is the primary concern. The Garbage First (G1) collector, the default in Java 21, addresses this by partitioning the heap into regions and targeting specific pause times.

### 4.1. The Pause Time Target Model

The central argument for G1 testing is `-XX:MaxGCPauseMillis=<time>`. This is not a hard limit but a "soft goal" that drives the entire heuristic engine of G1.

**Testing Methodology**: One cannot simply set this to 10ms and expect success. The testing workflow involves a "sweep" of values (e.g., 50ms, 100ms, 200ms). As the target decreases, G1 will shrink the Young Generation to limit the amount of data to copy.

**Second-Order Effect**: If the target is set too low, the Young Gen becomes too small to hold transient objects. These objects are then prematurely promoted to the Old Gen, leading to frequent "Mixed Collections" and potentially a "Full GC" (the failure mode of G1). Testing must monitor the `gc+age` logs to confirm that the `MaxGCPauseMillis` target isn't causing a collapse in the Tenuring Distribution.

### 4.2. Tuning the Marking Cycle (IHOP)

Latency spikes in G1 often occur when the Old Generation fills up faster than the concurrent marker can scan it. The argument `-XX:InitiatingHeapOccupancyPercent=<percent>` (IHOP) controls the threshold at which the Concurrent Marking cycle begins.

**Default**: 45%.

**Testing Insight**: For applications with high allocation rates, waiting for 45% occupancy might be too late. Testing with a lower IHOP (e.g., 35%) can smooth out latency by starting the expensive marking work earlier. However, this consumes more CPU bandwidth (concurrent threads). The test metrics must therefore balance "Pause Time" against "CPU Utilization" (captured via `gc+cpu`) to find the sweet spot where marking finishes just before the Old Gen is exhausted.

### 4.3. Managing Humongous Allocations

A unique pathology of G1 is the "Humongous Object"—any object larger than 50% of a G1 region. These are allocated directly in the Old Generation, bypassing the efficient Young Gen collection, and can cause significant fragmentation and latency.

**Diagnostic Argument**: `-XX:G1HeapRegionSize=<size>`.

**Testing Strategy**: If logs reveal frequent "Humongous Allocation" pauses, the region size is likely too small. Testing with larger region sizes (e.g., increasing from 2MB to 8MB or 16MB) can convert these into normal allocations, dramatically improving both latency and throughput. This is a critical tuning step for Big Data applications processing large serialized blobs.

## 5. The Low-Latency Revolution: ZGC and Shenandoah

Java 21 solidifies the position of ZGC and Shenandoah as specialized tools for ultra-low latency requirements (sub-millisecond pauses), regardless of heap size. These collectors fundamentally change the testing parameters by decoupling pause times from the live set size.

### 5.1. ZGC: The Generational Shift

The introduction of Generational ZGC (enabled via `-XX:+UseZGC -XX:+ZGenerational` in JDK 21) addresses the primary weakness of the original ZGC: throughput. By separating the heap into generations, ZGC can now reclaim young objects cheaply, similar to G1, while maintaining its concurrent compaction for the old generation.

**Soft Heap Limits**: A unique argument for ZGC testing is `-XX:SoftMaxHeapSize=<size>`. This allows the JVM to use a smaller heap target for GC heuristics while retaining the ability to expand to `-Xmx` to handle sudden traffic bursts. Testing with `SoftMaxHeapSize` allows engineers to simulate "elastic" memory behavior, measuring how quickly the collector can adapt to varying load without triggering OOM errors.

**Uncommit Mechanics**: ZGC proactively returns unused memory to the OS, which is excellent for cost savings but risky for latency (due to page faults when memory is reclaimed). For strict latency testing, the argument `-XX:-ZUncommit` should be used to disable this behavior, isolating the GC performance from OS virtual memory management artifacts.

### 5.2. Shenandoah: Heuristics and Pacing

Shenandoah (available in Red Hat and other OpenJDK builds) offers distinct configuration modes via `-XX:ShenandoahGCHeuristics=<mode>`.

*   `adaptive`: The default, balanced approach.
*   `compact`: Runs GC cycles continuously. This is an invaluable mode for "Stress Testing." By forcing continuous collection, one can verify the application's absolute worst-case throughput floor and identify any race conditions in concurrent access.

**Failure Modes**: Shenandoah attempts to perform all work concurrently. If the allocation rate exceeds the collection rate, it degrades gracefully: first to "Pacing" (micro-stalls injected into allocation threads, monitored via `ShenandoahPacing`), then to "Degenerated GC" (a brief STW phase), and finally to "Full GC". Exhaustive testing requires monitoring the logs for these transition events. The presence of "Degenerated GC" events in the logs is a primary indicator that the heap size or concurrent thread count (`-XX:ConcGCThreads`) is insufficient for the load.

## 6. Memory Footprint Forensics: Native Memory Tracking

Testing for Memory Footprint is often mistakenly limited to the Java Heap (`-Xmx`). However, the JVM consumes significant "Native Memory" for thread stacks, code caches, GC structures, and off-heap buffers. In containerized environments, ignoring native memory leads to the "OOM Killer" terminating the process even when the heap is empty.

### 6.1. Enabling Native Memory Tracking (NMT)

The NMT feature is the forensic tool for footprint analysis. It is enabled with `-XX:NativeMemoryTracking=[summary|detail]`.

**Cost**: Enabling NMT incurs a 5-10% performance penalty. Therefore, footprint testing should be performed in a separate phase from throughput testing.

**Workflow**:
1.  Start the application with `-XX:NativeMemoryTracking=summary`.
2.  Establish a baseline: `jcmd <pid> VM.native_memory baseline`.
3.  Run the load test.
4.  Generate a diff: `jcmd <pid> VM.native_memory summary.diff`.

### 6.2. Analyzing the Footprint Breakdown

The output of NMT allows for the precise targeting of footprint-reducing arguments:

*   **Thread Stacks**: If the `Thread` category is consuming gigabytes, use `-Xss<size>` to reduce the per-thread stack size (e.g., from 1MB to 256KB). This is often necessary for applications with thousands of threads (e.g., heavy web servers).
*   **Code Cache**: If the `Code` category is excessive, `-XX:ReservedCodeCacheSize=<size>` can limit the JIT compiler's storage.
*   **Direct Buffers**: The `Internal` or `Other` sections often hide direct byte buffer usage. The argument `-XX:MaxDirectMemorySize=<size>` is the only safety valve to prevent unchecked growth of NIO buffers, which defaults to the size of the heap if unset.
*   **Metaspace**: The `Class` category is controlled by `-XX:MaxMetaspaceSize=<size>`. Testing should verify that the application stabilizes its class loading; uncapped Metaspace can grow until physical RAM is exhausted.

## 7. Allocation and Promotion Dynamics: The Root Cause

Ultimately, GC performance is a derivative function of the Object Allocation Rate. Testing the GC without understanding allocation is diagnosing the symptom, not the disease. While external profilers (like JProfiler) can track this, they add massive overhead. The JVM provides lightweight arguments to profile allocation via Thread Local Allocation Buffers (TLABs).

### 7.1. TLAB Profiling

TLABs are the fast-path for memory allocation. Logging their behavior with `-Xlog:gc+tlab=trace` provides a proxy for allocation intensity.

**Metrics**: The logs reveal "refills" and "slow allocs." A high refill rate indicates that threads are filling their buffers too quickly, forcing frequent synchronization with the main heap.

**Tuning Argument**: `-XX:TLABSize=<size>`.

**Testing Strategy**: Increasing the TLAB size (e.g., from default ergonomics to explicit 256k or 512k) can significantly improve throughput for allocation-intensive applications. The test involves sweeping TLAB sizes and correlating the `gc+tlab` logs with the overall Transaction Per Second (TPS) metric.

### 7.2. Controlling Promotion

The movement of objects from Young to Old generation is controlled by the Tenuring Threshold.

**Monitoring**: Use `-Xlog:gc+age=trace`.
**Tuning**: `-XX:MaxTenuringThreshold=<N>`.

**Testing Insight**: If the logs show objects are being promoted at Age 1 or 2, but then die quickly in the Old Gen, it creates "premature promotion" which is highly expensive for G1 and Parallel collectors. Increasing `-XX:SurvivorRatio` or the heap size itself to allow objects to age longer (e.g., to Age 4 or 5) before promotion can prevent these expensive Old Gen collections.

## 8. Diagnostic Toolkit: Unlocking Advanced Capabilities

For the most rigorous scenarios, standard product flags are insufficient. The JVM includes safety mechanisms that must be unlocked to access experimental or diagnostic features.

*   `-XX:+UnlockDiagnosticVMOptions`: This flag is a prerequisite for dumping internal states or forcing specific compilation behaviors.
*   `-XX:+PrintFlagsFinal`: This is the single most important argument for test reproducibility. Running `java -XX:+PrintFlagsFinal -version` outputs the exact state of every JVM parameter, including those set by ergonomics. Without capturing this output, a performance test is scientifically invalid, as the hidden variables (ergonomics) remain unknown.
*   `-XX:+HeapDumpOnOutOfMemoryError`: Essential for footprint testing. It ensures that if the test pushes the application to failure, the evidence (the heap state) is preserved for analysis.
*   `-XX:+DisableExplicitGC`: Many third-party libraries (especially those using RMI or Direct Buffers) invoke `System.gc()` explicitly. In a performance test, these artificial GCs can skew latency results. This argument disables them, ensuring that all observed pauses are the result of genuine memory pressure.

## 9. Conclusion

The testing of Java GC performance—spanning Throughput, Latency, Footprint, and GC Times—is not achieved through a static set of "best practice" flags, but through a dynamic process of hypothesis and verification. By utilizing the Unified Logging Framework (`-Xlog`) to expose the granular timing of phases and safepoints, and by employing Native Memory Tracking (`-XX:NativeMemoryTracking`) to audit the non-heap footprint, engineers can construct a high-resolution profile of the runtime.

Whether tuning the Parallel GC for raw throughput via `GCTimeRatio`, or optimizing G1 and ZGC for latency via `MaxGCPauseMillis` and `SoftMaxHeapSize`, the arguments detailed in this report provide the necessary control plane. The transition to data-driven tuning—where every configuration change is validated against specific log outputs and NMT diffs—is the defining characteristic of expert-level JVM performance engineering in the Java 21 era.

---

# Lab Exercise: Applying the Handbook

This module includes a demo program `LongRunningGCStability.java` and a runner script `run_experiments.sh` that implements the exhaustive logging and configuration strategies discussed in the handbook.

## 1. Compile and Run

Use the provided script to run the demo in different modes. The script automatically compiles the Java code and applies the complex logging flags recommended in the handbook.

```bash
# Make the script executable
chmod +x module7/run_experiments.sh

# Run in Baseline mode (Default GC + Exhaustive Logging)
./module7/run_experiments.sh baseline 60
```

## 2. Experiment Modes

Run the following commands to observe different GC behaviors:

### Throughput Optimization
Uses ParallelGC with aggressive throughput targets.
```bash
./module7/run_experiments.sh throughput 60
```
*Observe*: High throughput numbers, potentially higher max latency.

### Latency Optimization (G1)
Uses G1GC with a 50ms pause target.
```bash
./module7/run_experiments.sh latency-g1 60
```
*Observe*: More stable latency, potentially slightly lower throughput than ParallelGC.

### Ultra-Low Latency (ZGC)
Uses ZGC (Generational).
```bash
./module7/run_experiments.sh latency-zgc 60
```
*Observe*: Extremely low P99 latency (often < 1ms), but watch the memory footprint.

### Footprint Analysis
Enables Native Memory Tracking.
```bash
./module7/run_experiments.sh footprint 60
```
*Action*: While running, open another terminal and run:
```bash
jcmd <pid> VM.native_memory summary
```
(Find the PID using `jps` or looking at the java process).

## 3. Analyzing Logs

The script generates detailed logs in the format `gc-<mode>-<timestamp>.log`.
Open these logs to see the breakdown of GC phases, safepoint times, and age distribution as described in Section 2 of the Handbook.
