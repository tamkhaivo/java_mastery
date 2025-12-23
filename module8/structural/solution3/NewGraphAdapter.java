package module8.structural.solution3;

import java.util.ArrayList;
import java.util.List;

// Adapter for NewGraphEngine
public class NewGraphAdapter implements ChartProvider {
    private final NewGraphEngine adaptee;

    public NewGraphAdapter() {
        this.adaptee = new NewGraphEngine();
    }

    @Override
    public void renderChart(String title, int[] data) {
        // Translation logic:
        // 1. Convert int[] (app format) to List<Double> (vendor format)
        List<Double> doubleList = new ArrayList<>();
        for (int i : data) {
            doubleList.add((double) i);
        }

        // 2. Delegate to new API
        adaptee.generateGraph(title, doubleList);
    }
}
