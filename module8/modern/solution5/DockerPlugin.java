package module8.modern.solution5;

public class DockerPlugin implements Plugin {
    @Override
    public void execute() {
        System.out.println("DockerPlugin: Starting container services...");
    }

    @Override
    public String getName() {
        return "Docker Support";
    }
}
