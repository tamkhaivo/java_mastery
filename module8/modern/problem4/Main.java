package module8.modern.problem4;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Shape> shapes = List.of(
                new Circle(5.0),
                new Rectangle(4.0, 6.0));

        System.out.println("--- Visitor Pattern (Maintenance Nightmare) ---");
        AreaVisitor areaVisitor = new AreaVisitor();

        for (Shape shape : shapes) {
            shape.accept(areaVisitor);
        }

        System.out.printf("Total Area: %.2f%n", areaVisitor.getTotalArea());

        System.out.println("\n[Critique] To add 'Triangle', we must:");
        System.out.println("1. Create Triangle class");
        System.out.println("2. Modify ShapeVisitor interface (add visit(Triangle)) -> OCP Violation");
        System.out.println("3. Modify AreaVisitor (implement visit(Triangle))");
        System.out.println("4. Recompile ALL other visitors");
    }
}
