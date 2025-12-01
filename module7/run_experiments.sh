#!/bin/bash

# Module 7: GC Experiments Runner
# Implements the "Exhaustive Argumentation" configurations.

TARGET_MODE=$1
DURATION=${2:-60} # Default 60 seconds for quick testing

if [ -z "$TARGET_MODE" ]; then
    echo "Usage: ./run_experiments.sh <mode|all> [duration]"
    echo "Modes:"
    echo "  baseline       - Default GC (likely G1) with exhaustive logging"
    echo "  throughput     - ParallelGC optimized for throughput"
    echo "  latency-g1     - G1GC optimized for 50ms max pause"
    echo "  latency-zgc    - ZGC (Generational if available)"
    echo "  latency-shen   - Shenandoah (if available)"
    echo "  footprint      - Native Memory Tracking enabled"
    echo "  all            - Run all above modes sequentially"
    exit 1
fi

# Compile
echo "Compiling..."
javac module7/LongRunningGCStability.java

run_experiment() {
    local MODE=$1
    
    # Base Logging Configuration (from Handbook)
    # Captures full lifecycle: allocation, aging, reference processing, phases, and safepoints.
    # Note: We use ${MODE} in the filename
    LOG_OPTS="-Xlog:gc*,gc+ref=debug,gc+phases=debug,gc+age=trace,safepoint:file=gc-${MODE}-%t.log:time,uptime,level,tags:filecount=10,filesize=100M"

    case $MODE in
        baseline)
            GC_OPTS=""
            ;;
        throughput)
            # ParallelGC with GCTimeRatio=19 (5% GC time goal) and AdaptiveSizePolicyWeight=90
            GC_OPTS="-XX:+UseParallelGC -XX:GCTimeRatio=19 -XX:AdaptiveSizePolicyWeight=90"
            ;;
        latency-g1)
            # G1GC with 50ms pause target and early marking (IHOP=45)
            GC_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:InitiatingHeapOccupancyPercent=45"
            ;;
        latency-zgc)
            # ZGC (Generational) - Note: ZGenerational is default in later JDK 21 builds or requires flag
            GC_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:-ZUncommit"
            ;;
        latency-shen)
            # Shenandoah with compact heuristics for stress testing
            GC_OPTS="-XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact"
            ;;
        latency-generational-shen)
            # Shenandoah with compact heuristics for stress testing
            GC_OPTS="-XX:+UseShenandoahGC -XX:ShenandoahGCMode=generational"
            ;;
        footprint)
            # Enable NMT
            GC_OPTS="-XX:NativeMemoryTracking=summary"
            echo "Note: For footprint, run 'jcmd <pid> VM.native_memory summary.diff' during execution."
            ;;
        *)
            echo "Unknown mode: $MODE"
            return
            ;;
    esac

    echo "----------------------------------------------------------------"
    echo "Running Mode: $MODE"
    echo "Duration: $DURATION seconds"
    echo "Logs: gc-${MODE}-*.log"
    CSV_FILE="module7/metrics.csv"
    echo "CSV: $CSV_FILE"
    echo "Command: java $LOG_OPTS $GC_OPTS module7.LongRunningGCStability $DURATION $CSV_FILE $MODE"
    echo "----------------------------------------------------------------"

    java -cp . $LOG_OPTS $GC_OPTS module7.LongRunningGCStability $DURATION $CSV_FILE $MODE
    
    # Add a small pause between runs
    sleep 2
}

if [ "$TARGET_MODE" == "all" ]; then
    # Run all modes
    # Note: We skip shenandoah by default in 'all' if it's not guaranteed to be present, 
    # but for this exercise we assume the user might want to try it. 
    # If it fails, it fails.
    MODES=("baseline" "throughput" "latency-g1" "latency-zgc" "footprint")
    
    echo "Starting sequential execution of ALL modes..."
    for m in "${MODES[@]}"; do
        run_experiment $m
    done
    
    echo "================================================================"
    echo "All experiments completed."
    echo "Combined metrics available in module7/metrics.csv"
    echo "================================================================"
else
    run_experiment $TARGET_MODE
fi
