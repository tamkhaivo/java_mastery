package module8.modern.solution5;

import java.util.ServiceLoader;

public class IDE {
    // SOLUTION: Use ServiceLoader to discover implementations.
    // The IDE class relies ONLY on the Plugin interface.
    // It doesn't know about GitPlugin or DockerPlugin class names.

    public void startup() {
        System.out.println("IDE: Starting up...");
        System.out.println("IDE: Scanning for plugins via SPI...");

        // This looks in META-INF/services for 'module8.modern.solution5.Plugin'
        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);

        for (Plugin plugin : loader) {
            System.out.println("  -> Discovered & Loaded: " + plugin.getName());
            plugin.execute();
        }

        System.out.println("IDE: Ready.");
    }
}
