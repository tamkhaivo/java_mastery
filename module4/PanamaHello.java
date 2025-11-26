package module4;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class PanamaHello {
    public static void main(String[] args) throws Throwable {
        System.out.println("=== Project Panama: Java Calling C ===");

        // 1. The Linker: Your bridge to the OS libraries
        Linker linker = Linker.nativeLinker();
        SymbolLookup stdlib = linker.defaultLookup();

        // 2. Find the C function 'strlen'
        // C signature: size_t strlen(const char *str);
        MemorySegment strlenAddress = stdlib.find("strlen")
                .orElseThrow(() -> new RuntimeException("Function not found"));

        // 3. Describe the function to Java
        // Arguments: Address (Pointer to String) -> Returns: long (size_t)
        FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_LONG,  // Return type
                ValueLayout.ADDRESS     // Argument type (The pointer)
        );

        MethodHandle strlen = linker.downcallHandle(strlenAddress, descriptor);

        // 4. Manage Off-Heap Memory
        // We use an "Arena" to allocate memory safely outside the Java Heap.
        // try-with-resources ensures memory is freed immediately when the block ends.
        try (Arena arena = Arena.ofConfined()) {
            
            // Convert Java String -> C String (Off-Heap Memory)
            String greeting = "Hello from Java 25!";
            MemorySegment cString = arena.allocateFrom(greeting);

            System.out.println("Java String: " + greeting);
            System.out.println("Memory Address: " + cString);

            // 5. Invoke the C function
            long length = (long) strlen.invoke(cString);
            
            System.out.println("Length (calculated by C 'strlen'): " + length);
        }
        // The off-heap memory is now deallocated.
    }
}
