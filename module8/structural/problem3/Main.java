package module8.structural.problem3;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting Dashboard ---");
        AnalyticsDashboard dashboard = new AnalyticsDashboard();
        dashboard.displayMonthlyReport();

        System.out.println("\n[System Notification] The vendor for OldChartLib has announced End-of-Life.");
        System.out.println(
                "[Problem] We need to switch to 'NewGraphEngine', but AnalyticsDashboard is tightly coupled to OldChartLib.");
    }
}
