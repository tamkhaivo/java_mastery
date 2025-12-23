package module8.modern.solution5;

public class GeminiPlugin implements Plugin {
    @Override
    public void execute() {
        System.out.println("GeminiPlugin: Starting container services...");
    }

    @Override
    public String getName() {
        return "Gemini Support";
    }
}
