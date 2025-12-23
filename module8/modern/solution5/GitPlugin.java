package module8.modern.solution5;

public class GitPlugin implements Plugin {
    @Override
    public void execute() {
        System.out.println("GitPlugin: Initializing repository scanning...");
    }

    @Override
    public String getName() {
        return "Git Integration";
    }
}
