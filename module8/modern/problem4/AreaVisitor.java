package module8.modern.problem4;

// A concrete visitor implementation
public class AreaVisitor implements ShapeVisitor {
    private double totalArea = 0;

    public double getTotalArea() {
        return totalArea;
    }

    @Override
    public void visit(Circle circle) {
        double area = Math.PI * Math.pow(circle.getRadius(), 2);
        System.out.println("Calculating area for Circle: " + area);
        totalArea += area;
    }

    @Override
    public void visit(Rectangle rectangle) {
        double area = rectangle.getWidth() * rectangle.getHeight();
        System.out.println("Calculating area for Rectangle: " + area);
        totalArea += area;
    }
}
