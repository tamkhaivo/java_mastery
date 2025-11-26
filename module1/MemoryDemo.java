package module1;

import java.util.ArrayList;
import java.util.List;

public class MemoryDemo {
    public static void main(String[] args) throws InterruptedException {
        long pid = ProcessHandle.current().pid();
        System.out.println("=== Memory Demo Running (PID: " + pid + ") ===");
        System.out.println("1. STACK: I am running inside the 'main' stack frame.");
        System.out.println("2. HEAP:  I am allocating objects now...\n");

        List<byte[]> memoryFill = new ArrayList<>();
        
        // Loop to continuously allocate memory
        for (int i = 0; i < 500; i++) {
            // Allocate 1MB byte array (Stored on HEAP)
            memoryFill.add(new byte[1024 * 1024]); 
            
            if (i % 10 == 0) System.out.print("."); // Progress bar

            // Every 50MB, release everything to force Garbage Collection
            if (memoryFill.size() >= 50) {
                System.out.println("\n[!] 50MB limit reached. Clearing list (Making objects 'garbage').");
                memoryFill.clear();
                System.out.println("[!] Sleeping 2s to let you run 'jmap' or 'jstat'...");
                Thread.sleep(2000); 
            }
            Thread.sleep(50); // Slow down so we can watch
        }
    }
}
