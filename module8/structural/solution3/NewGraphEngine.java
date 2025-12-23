package module8.structural.solution3;

import java.util.List;

// The NEW 3rd-party library (Incompatible API)
public class NewGraphEngine {
    // Note: uses List<Double> instead of int[]
    public void generateGraph(String label, List<Double> values) {
        System.out.println("NewGraphEngine: Rendering Modern Graph [" + label + "]");
        System.out.println("  Processing " + values.size() + " floating point values...");
        // Fancy implementation...
        System.out.println("  (Rendered via Modern Engine)");
    }
}
