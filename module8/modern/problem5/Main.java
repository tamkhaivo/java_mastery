package module8.modern.problem5;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Problem 5: The Monolithic Plugin Architecture ---");
        IDE ide = new IDE();
        ide.startup();

        System.out.println("\n[Critique] To add 'DockerPlugin', we must Modify IDE.java and recompile.");
        System.out.println("(This architecture is 'Closed' for extension)");
    }
}
