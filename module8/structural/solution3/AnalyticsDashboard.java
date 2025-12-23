package module8.structural.solution3;

public class AnalyticsDashboard {
    // OCP Adherence: Depends on the stable interface, not concrete libraries.
    private final ChartProvider chartProvider;

    // Dependency Injection
    public AnalyticsDashboard(ChartProvider chartProvider) {
        this.chartProvider = chartProvider;
    }

    public void displayMonthlyReport() {
        System.out.println("AnalyticsDashboard: Generating Monthly Sales Report...");

        int[] salesData = { 500, 1200, 800, 1500 };

        // Polymorphic call - simpler and decoupled
        chartProvider.renderChart("Monthly Sales", salesData);
    }
}
