package module8.structural.solution3;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Scenario 1: Using Legacy Adapter ---");
        // Configure dashboard with Old Adapter
        ChartProvider legacyProvider = new OldChartAdapter();
        AnalyticsDashboard dashboard1 = new AnalyticsDashboard(legacyProvider);
        dashboard1.displayMonthlyReport();

        System.out.println("\n--- Scenario 2: Swapping to Modern Engine (OCP) ---");
        // Vendor EOL? No problem. Just swap the adapter.
        // Dashboard code remains strict 100% UNTOUCHED.
        ChartProvider modernProvider = new NewGraphAdapter();
        AnalyticsDashboard dashboard2 = new AnalyticsDashboard(modernProvider);
        dashboard2.displayMonthlyReport();

        System.out.println("\n[Success] Switched underlying library without modifying AnalyticsDashboard class.");
    }
}
