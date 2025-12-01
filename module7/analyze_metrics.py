import pandas as pd
import matplotlib.pyplot as plt
import os

def analyze_metrics(file_path):
    # Read the CSV file
    try:
        df = pd.read_csv(file_path)
    except FileNotFoundError:
        print(f"Error: File not found at {file_path}")
        return

    # Strip whitespace from column names just in case
    df.columns = df.columns.str.strip()

    # Group by Mode
    grouped = df.groupby('Mode')

    # Prepare data for plotting
    modes = []
    throughput_data = []
    latency_data = []
    heap_data = []

    for mode, group in grouped:
        # Simulate collecting samples of 60
        sample = group.head(60)
        
        modes.append(mode)
        throughput_data.append(sample['Throughput'])
        latency_data.append(sample['LatencyP99'])
        heap_data.append(sample['HeapUsed'])

    # Create histograms
    fig, axes = plt.subplots(1, 3, figsize=(18, 6))

    # Throughput Histogram
    axes[0].hist(throughput_data, bins=25, label=modes, alpha=0.7, histtype='bar', stacked=False)
    axes[0].set_title('Throughput Histogram')
    axes[0].set_xlabel('Throughput')
    axes[0].set_ylabel('Frequency')
    axes[0].legend()

    # Latency Histogram
    axes[1].hist(latency_data, bins=25, label=modes, alpha=0.7, histtype='bar', stacked=False)
    axes[1].set_title('Latency P99 Histogram')
    axes[1].set_xlabel('Latency P99')
    axes[1].set_ylabel('Frequency')
    axes[1].legend()

    # Heap Used Histogram
    axes[2].hist(heap_data, bins=25, label=modes, alpha=0.7, histtype='bar', stacked=False)
    axes[2].set_title('Heap Used Histogram')
    axes[2].set_xlabel('Heap Used')
    axes[2].set_ylabel('Frequency')
    axes[2].legend()

    plt.tight_layout()
    
    # Save the plot to a file
    output_file = os.path.join(os.path.dirname(file_path), 'histograms.png')
    plt.savefig(output_file)
    print(f"Histograms saved to {output_file}")
    
    # Show the plot (optional, might not work in headless env but good to have)
    # plt.show()

if __name__ == "__main__":
    metrics_file = '/Users/tvo/java_mastery/module7/metrics.csv'
    analyze_metrics(metrics_file)
