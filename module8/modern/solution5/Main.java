package module8.modern.solution5;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Solution 5: Modular Plugin Architecture (SPI) ---");
        IDE ide = new IDE();
        ide.startup();

        System.out.println("\n[Analysis] Advantages:");
        System.out.println("1. IDE class is untouched when adding plugins.");
        System.out.println("2. Plugins can be in completely separate JARs.");
        System.out.println("3. True OCP at the deployment level.");
    }
}
