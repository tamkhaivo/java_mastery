package module1;

public class SimpleLifecycle {
    public static void main(String[] args) {
        int a = 5;
        int b = 10;
        int sum = add(a, b);
        System.out.println("Sum: " + sum);
    }

    public static int add(int x, int y) {
        return x + y;
    }
}
