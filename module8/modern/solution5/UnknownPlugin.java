package module8.modern.solution5;

public class UnknownPlugin implements Plugin {
    @Override
    public void execute() {
        System.out.println("UnknownPlugin: Starting container services...");
    }

    @Override
    public String getName() {
        return "Unknown Support";
    }
}
