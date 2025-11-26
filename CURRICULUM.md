# Java 25 Mastery Curriculum

## Module 1: Under the Hood (The JVM)
- [X] **The Lifecycle of a Java Class:** Compilation (`javac`), Bytecode, Classloading.
- [X] **Deep Dive: Bytecode:** Reading `.class` files with `javap -c -v -p module1.SimpleLifecycle`.
- [X] **Memory Management:** Stack vs. Heap, Object Layout, Garbage Collection basics.
- - Heap dump: `jmap -histo:live <PID> | head -n 10`
- - Stack Frame dump: `jstat -gcutil <PID> 500`
- - Live Memory dump: `jstat -gc <PID> 500`
- [X] **Garbage Collection:** Types of GC (Serial, Parallel, CMS, G1), Tuning.
- - Serial GC: `java -XX:+UseSerialGC` - Low Overhead - Best Use Case:Tiny scripts, Lambda functions
- - Parallel GC: `java -Xmx200m -XX:+UseParallelGC -Xlog:gc:stdout module1.MemoryDemo` - Max Throughput │ Number crunching, ETL jobs.
- - CMS GC: `java -XX:+UseConcMarkSweepGC -Xmx200m -Xlog:gc:stdout module1.MemoryDemo` - Balanced │ Web servers, standard apps (Default).
- - G1 GC: `java -XX:+UseG1GC -Xmx200m -Xlog:gc:stdout module1.MemoryDemo` - Min Latency │ Real-time apps, massive datasets.
- [X] **Execution Engine:** Interpreter vs. JIT Compiler (C1/C2), Native Code.
- - JIT Compilation: `java -XX:+PrintCompilation module1.JITDemo | grep "performCalculation"`
- - Compare Tiers: `java -XX:TieredStopAtLevel=1 module1.JITDemo` vs `java -XX:TieredStopAtLevel=4 module1.JITDemo`
- - Native Code: `javac -h module1.JITDemo`

## Module 2: Modern Java Syntax & Safety (Project Amber)
- [X] **Data Modeling:** Records, Sealed Classes/Interfaces.
- - `javap -p module2.User`
- - `javap -v module2.TransactionStatus`
- - `javap -v module2.TransactionStatus | grep "Permitted" -A 3`
- [X] **Control Flow:** Pattern Matching for `switch`, Record Patterns.
- [X] **Text & Formatting:** String Templates (if available/preview), Text Blocks.

## Module 3: Next-Gen Concurrency (Project Loom)
- [X] **Virtual Threads:** The M:N threading model, lightweight threads.
- [X] **Structured Concurrency:** Managing thread lifecycles safely.
- - `javac module3.StandardConcurrency.java`
- - `java module3.StandardConcurrency`
- - `StructuredTaskScope` is in preview, so we need to use `--enable-preview` flag.
- [X] **Scoped Values:** Efficient alternatives to ThreadLocal.
- - `javac --release 25 --enable-preview module3/ScopedValuesDemo.java`
- - `java --enable-preview module3.ScopedValuesDemo`

## Module 4: High Performance & Native Interop (Project Panama & Vector)
- [X] **Foreign Function & Memory API:** Calling C code without JNI.
- - `javac --release 25 --enable-preview module4/PanamaHello.java`
- - `java --enable-native-access=ALL-UNNAMED module4.PanamaHello`
- [X] **Vector API:** SIMD (Single Instruction, Multiple Data) programming in Java.
- - `javac --add-modules jdk.incubator.vector module4/VectorDemo.java`
- - `java --add-modules jdk.incubator.vector module4.VectorDemo`
## Module 5: Advanced Runtime Tuning
- [ ] **JVM Flags:** Tuning heap, GC algorithms (ZGC, G1).
- [X] **CDS (Class Data Sharing):** Fast startup.
- - `javac module5/HelloWorld.java`
- - `time java module5.HelloWorld`
- - `java -Xshare:dump -XX:SharedArchiveFile=app.jsa -cp . module5.HelloWorld`
- - `time java -Xshare:on -XX:SharedArchiveFile=app.jsa -cp . module5.HelloWorld`
