package module8.structural.solution3;

// Adapter for the OldChartLib
public class OldChartAdapter implements ChartProvider {
    private final OldChartLib adaptee;

    public OldChartAdapter() {
        this.adaptee = new OldChartLib();
    }

    @Override
    public void renderChart(String title, int[] data) {
        // Translation logic: ChartProvider.renderChart -> OldChartLib.drawBarChart
        adaptee.drawBarChart(title, data);
    }
}
