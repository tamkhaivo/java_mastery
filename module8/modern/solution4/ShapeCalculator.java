package module8.modern.solution4;

public class ShapeCalculator {

    public double calculateArea(Shape shape) {
        // Pattern Matching for Switch (Java 21+)
        // Compiler exhaustive check: If we add logic for 'Triangle' but forget it here,
        // compile error!
        return switch (shape) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
        };
    }
}
