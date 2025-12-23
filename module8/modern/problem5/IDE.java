package module8.modern.problem5;

import java.util.List;

public class IDE {
    // VIOLATION: The IDE class knows exactly which concrete plugins exist.
    // Use Case: If a 3rd party wants to distribute "DockerPlugin", they can't.
    // They would need to convince us to recompile the IDE class with their plugin
    // added here.
    private List<Plugin> plugins = List.of(
            new GitPlugin());

    public void startup() {
        System.out.println("IDE: Starting up...");
        System.out.println("IDE: Loading built-in plugins...");

        for (Plugin plugin : plugins) {
            System.out.println("  -> Loading: " + plugin.getName());
            plugin.execute();
        }

        System.out.println("IDE: Ready.");
    }
}
