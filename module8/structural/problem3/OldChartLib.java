package module8.structural.problem3;

// This represents a rigid 3rd-party library that you cannot change.
public class OldChartLib {
    public void drawBarChart(String title, int[] data) {
        System.out.println("OldChartLib: Drawing Bar Chart '" + title + "'");
        System.out.println("  Data points: " + data.length);
        for (int value : data) {
            System.out.print("  [Bar: " + value + "] ");
        }
        System.out.println("\n  (Rendered via Legacy System)");
    }
}
