package module8.structural.problem3;

public class AnalyticsDashboard {
    // VIOLATION: Hard dependency on a specific concrete class.
    // If we want to switch to "NewGraphEngine", we have to modify this file.
    private OldChartLib chartLib = new OldChartLib();

    public void displayMonthlyReport() {
        System.out.println("AnalyticsDashboard: Generaling Monthly Sales Report...");

        // Data logic
        int[] salesData = { 500, 1200, 800, 1500 };

        // Rigid call to specific API
        chartLib.drawBarChart("Monthly Sales", salesData);
    }
}
