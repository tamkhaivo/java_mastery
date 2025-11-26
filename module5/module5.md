# Module 5: Advanced Runtime Tuning

Understanding JVM flags allows you to fine-tune your application's performance characteristics, from memory consumption to startup
  time and CPU utilization.

Here are the key categories and examples of flags you'll encounter.

1. Heap Tuning Flags
These flags control the overall memory allocated to your application.

- `-Xms<size>`: Sets the initial size of the Java heap. It's often good practice to set this equal to -Xmx to avoid heap resizing
     during runtime, which can cause minor pauses.
- `-Xmx<size>`: Sets the maximum size of the Java heap.
- `-XX:NewRatio=<N>`: Divides the heap into Young and Old generations. A value of N means the ratio of Old to Young generation space
     is N:1. So, NewRatio=2 means Old Gen is twice the size of Young Gen.
- `-XX:MaxNewSize=<size>`: Maximum size of the Young generation.
- `-XX:SurvivorRatio=<N>`: Ratio of Eden space to Survivor spaces (e.g., SurvivorRatio=8 means one Eden for 8 Survivor spaces).

2. Garbage Collector (GC) Tuning Flags
These flags influence how and when the Garbage Collector runs.

- `-XX:MaxGCPauseMillis=<ms>`: A hint to the GC (especially G1 and ZGC) to try and achieve pauses no longer than the specified
     milliseconds. The GC will adjust its behavior to meet this target, potentially by running more frequently.
- `-XX:+PrintGCDetails` / `-Xlog:gc*`: Enables verbose GC logging. -Xlog:gc* is the modern, highly configurable way to get detailed
     GC logs.
- `-XX:+UseG1GC` / `-XX:+UseZGC`: Explicitly selects the Garbage Collector.

  3. JIT Compiler Tuning Flags
  These flags affect the behavior of the Just-In-Time compiler.

- `-XX:TieredStopAtLevel=<N>`: Controls at which tier the JIT compiler should stop optimizing.
    * 0: Interpreter only.
    * 1: C1 (simple compiler).
    * 4: C2 (full optimization, default).
- `-XX:CompileThreshold=<N>`: Sets the number of invocations or loop back-edges after which a method becomes "hot" and is compiled by
     the JIT.

4. Logging and Diagnostic Flags
These provide insights into the JVM's internal operations.

- `-XX:+PrintCompilation`: As we saw, logs when methods are compiled by the JIT.
- `-XX:+UnlockDiagnosticVMOptions`: Required to enable certain diagnostic flags, like PrintAssembly.
- `-XX:+HeapDumpOnOutOfMemoryError`: Automatically generates a heap dump file when an OutOfMemoryError occurs, invaluable for
     debugging memory leaks.

## Lab 1: JVM Flags in Action (Heap and GC)

We will use your existing ~/java_mastery/module1/MemoryDemo.java to observe the effects of Heap and GC tuning flags.

1. Compile `MemoryDemo.java` (if you haven't already):
    1. cd ~/java_mastery
    2. javac module1/MemoryDemo.java

2. Experiment A: Small Heap, Aggressive GC
We'll set a tiny heap (e.g., 64MB max) and ask G1 to try for very short pauses.

    1. java -Xmx64m -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Xlog:gc module1.MemoryDemo
       * Observe: The program will likely run slower, with more frequent (but hopefully shorter) GC pauses. The MemoryDemo tries to
         allocate 1MB repeatedly, so with a 64MB heap, it will be constantly fighting for space. The GC logs will be very active.

3. Experiment B: Large Young Generation (for throughput)
Let's give more space to the Young Generation using NewRatio, which might reduce full GCs but increase minor GC times.

1. java -Xmx200m -XX:NewRatio=1 -Xlog:gc module1.MemoryDemo
       * Observe: A NewRatio=1 means Young Gen is as big as Old Gen (ratio Old:Young is 1:1). You might see different patterns of minor
         vs. major GC, or even fewer full GCs compared to the default.
