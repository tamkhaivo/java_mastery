package module8.modern.problem4;

// The Visitor interface
// PROBLEM: If we add a new Shape (e.g., Triangle), we MUST modify this interface
// and ALL classes that implement it.
public interface ShapeVisitor {
    void visit(Circle circle);

    void visit(Rectangle rectangle);
}
