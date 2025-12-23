package module8.modern.solution4;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Shape> shapes = List.of(
                new Circle(5.0),
                new Rectangle(4.0, 6.0));

        System.out.println("--- Modern Java Solution (Sealed Classes) ---");
        ShapeCalculator calculator = new ShapeCalculator();

        for (Shape shape : shapes) {
            double area = calculator.calculateArea(shape);
            System.out.printf("Area of %s: %.2f%n", shape.getClass().getSimpleName(), area);
        }

        System.out.println("\n[Analysis] Advantages:");
        System.out.println("1. No 'accept' method in Shape (Decoupled)");
        System.out.println("2. Records reduce 20 lines of boilerplate to 1 line");
        System.out.println("3. Sealed interface ensures compiler checks all cases");
    }
}
