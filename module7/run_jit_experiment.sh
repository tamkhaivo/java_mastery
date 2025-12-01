#!/bin/bash

# JIT vs TieredStopAtLevel Analysis
# Compares standard JIT vs TieredStopAtLevel=1-4

TARGET_MODE=$1
DURATION=${2:-60}

if [ -z "$TARGET_MODE" ]; then
    echo "Usage: ./run_jit_experiment.sh <standard|tiered1|tiered2|tiered3|tiered4|all> [duration]"
    exit 1
fi

# Compile
echo "Compiling..."
javac module7/LongRunningGCStability.java

run_experiment() {
    local MODE=$1
    local JIT_OPTS=""
    
    if [ "$MODE" == "standard" ]; then
        JIT_OPTS=""
    elif [[ "$MODE" =~ ^tiered[1-4]$ ]]; then
        LEVEL=${MODE#tiered}
        JIT_OPTS="-XX:TieredStopAtLevel=$LEVEL"
    else
        echo "Unknown mode: $MODE"
        return
    fi

    echo "----------------------------------------------------------------"
    echo "Running Mode: $MODE"
    echo "Duration: $DURATION seconds"
    CSV_FILE="module7/jit_metrics.csv"
    echo "CSV: $CSV_FILE"
    echo "Command: java $JIT_OPTS module7.LongRunningGCStability $DURATION $CSV_FILE $MODE"
    echo "----------------------------------------------------------------"

    java -cp . $JIT_OPTS module7.LongRunningGCStability $DURATION $CSV_FILE $MODE
    
    sleep 2
}

if [ "$TARGET_MODE" == "all" ]; then
    MODES=("standard" "tiered1" "tiered2" "tiered3" "tiered4")
    
    echo "Starting sequential execution of ALL modes..."
    for m in "${MODES[@]}"; do
        run_experiment $m
    done
    
    echo "================================================================"
    echo "All experiments completed."
    echo "Combined metrics available in module7/jit_metrics.csv"
    echo "================================================================"
else
    run_experiment $TARGET_MODE
fi
