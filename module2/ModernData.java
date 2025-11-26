package module2;

sealed interface Shape permits Circle, Rectangle, Square {}

record Circle(double radius) implements Shape {}
record Rectangle(double length, double width) implements Shape {}
record Square(double side) implements Shape {}

public class ModernData {
    public static void main(String[] args) {
        Shape s1 = new Circle(5);
        Shape s2 = new Rectangle(4, 6);
        Shape s3 = new Square(3);

        System.out.println("Circle area: " + calculateArea(s1));
        System.out.println("Rect area: " + calculateArea(s2));
        System.out.println("Square area: " + calculateArea(s3));
    }

    public static double calculateArea(Shape shape) {
        return switch (shape) {
            case Circle(var r) -> Math.PI * r * r;
            case Rectangle(var l, var w) -> l * w;
            case Square(var s) -> s * s;
        };
    }
}
