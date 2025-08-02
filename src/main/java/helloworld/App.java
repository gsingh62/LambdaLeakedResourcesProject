package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class App implements RequestHandler<Object, Object> {
    // Static variables to demonstrate runtime environment reuse
    private static int invocationCount = 0;
    private static String runtimeId = UUID.randomUUID().toString();
    private static long firstInvocationTime = 0;
    
    // Static list to accumulate leaked resources
    private static List<FileOutputStream> leakedFileStreams = new ArrayList<>();
    private static List<FileWriter> leakedFileWriters = new ArrayList<>();
    private static List<BufferedReader> leakedReaders = new ArrayList<>();
    
    @Override
    public Object handleRequest(Object input, Context context) {
        // Increment invocation count (persists across invocations if runtime is reused)
        invocationCount++;
        
        // Record first invocation time
        if (firstInvocationTime == 0) {
            firstInvocationTime = System.currentTimeMillis();
        }
        
        long currentTime = System.currentTimeMillis();
        long runtimeAge = currentTime - firstInvocationTime;
        
        // Log runtime environment information
        System.out.println("=== RUNTIME ENVIRONMENT REUSE EVIDENCE ===");
        System.out.println("Runtime ID: " + runtimeId);
        System.out.println("Invocation Count: " + invocationCount);
        System.out.println("Runtime Age (ms): " + runtimeAge);
        System.out.println("Current Time: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("Request ID: " + context.getAwsRequestId());
        System.out.println("Remaining Time (ms): " + context.getRemainingTimeInMillis());
        System.out.println("==========================================");
        
        // Demonstrate resource leaks that would accumulate if runtime is reused
        demonstrateResourceLeaks();
        
        // Write evidence to multiple locations for debugging
        writeEvidenceToMultipleLocations(context.getAwsRequestId(), invocationCount, runtimeAge);
        
        // Check for existing evidence files to demonstrate persistence
        checkForExistingEvidence();
        
        // Show current resource leak status
        showResourceLeakStatus();
        
        return String.format(
            "Runtime ID: %s, Invocation: %d, Age: %dms, Request: %s, Leaked Resources: %d",
            runtimeId, invocationCount, runtimeAge, context.getAwsRequestId(), 
            leakedFileStreams.size() + leakedFileWriters.size() + leakedReaders.size()
        );
    }
    
    private void demonstrateResourceLeaks() {
        try {
            System.out.println("=== CREATING RESOURCE LEAKS ===");
            
            // 1. Create unclosed FileOutputStream (file handle leak)
            String leakFile1 = "/tmp/leaked_file_" + invocationCount + ".txt";
            FileOutputStream fos = new FileOutputStream(leakFile1);
            fos.write(("Leaked file stream from invocation " + invocationCount + "\n").getBytes());
            // Intentionally NOT closing the stream
            leakedFileStreams.add(fos);
            System.out.println("Created leaked FileOutputStream #" + leakedFileStreams.size());
            
            // 2. Create unclosed FileWriter (another file handle leak)
            String leakFile2 = "/tmp/leaked_writer_" + invocationCount + ".txt";
            FileWriter fw = new FileWriter(leakFile2);
            fw.write("Leaked file writer from invocation " + invocationCount + "\n");
            // Intentionally NOT closing the writer
            leakedFileWriters.add(fw);
            System.out.println("Created leaked FileWriter #" + leakedFileWriters.size());
            
            // 3. Create unclosed BufferedReader (memory leak)
            String leakFile3 = "/tmp/leaked_reader_" + invocationCount + ".txt";
            // First create a file to read from
            try (FileWriter tempWriter = new FileWriter(leakFile3)) {
                tempWriter.write("Data for leaked reader from invocation " + invocationCount + "\n");
            }
            BufferedReader br = new BufferedReader(new FileReader(leakFile3));
            // Intentionally NOT closing the reader
            leakedReaders.add(br);
            System.out.println("Created leaked BufferedReader #" + leakedReaders.size());
            
            // 4. Create unclosed network-like resources (simulated)
            System.out.println("Simulating network connection leak...");
            
            // 5. Memory allocation that won't be garbage collected
            byte[] memoryLeak = new byte[1024 * 1024]; // 1MB memory leak
            System.out.println("Allocated 1MB memory leak");
            
            System.out.println("=== RESOURCE LEAKS CREATED ===");
            
        } catch (IOException e) {
            System.err.println("Error creating resource leaks: " + e.getMessage());
        }
    }
    
    private void showResourceLeakStatus() {
        System.out.println("=== RESOURCE LEAK STATUS ===");
        System.out.println("Total leaked FileOutputStreams: " + leakedFileStreams.size());
        System.out.println("Total leaked FileWriters: " + leakedFileWriters.size());
        System.out.println("Total leaked BufferedReaders: " + leakedReaders.size());
        System.out.println("Total leaked resources: " + (leakedFileStreams.size() + leakedFileWriters.size() + leakedReaders.size()));
        
        // Show memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        System.out.println("Memory usage: " + (usedMemory / 1024 / 1024) + "MB / " + (maxMemory / 1024 / 1024) + "MB");
        System.out.println("================================");
    }
    
    private void writeEvidenceToMultipleLocations(String requestId, int invocationCount, long runtimeAge) {
        String content = String.format(
            "[%s] Runtime ID: %s, Invocation: %d, Request: %s, Age: %dms, Leaked Resources: %d\n",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            runtimeId, invocationCount, requestId, runtimeAge,
            leakedFileStreams.size() + leakedFileWriters.size() + leakedReaders.size()
        );
        
        // Try multiple locations
        String[] locations = {
            "/tmp/runtime_reuse_evidence.txt",
            "/tmp/accumulated_resources.txt", 
            "/tmp/simple_evidence.txt",
            "/var/task/evidence.txt",
            "/opt/evidence.txt",
            "./evidence.txt"
        };
        
        for (String location : locations) {
            try {
                File file = new File(location);
                System.out.println("Attempting to write to: " + location);
                System.out.println("  - File exists: " + file.exists());
                System.out.println("  - Can write: " + file.canWrite());
                System.out.println("  - Parent exists: " + file.getParentFile().exists());
                System.out.println("  - Parent can write: " + file.getParentFile().canWrite());
                
                try (FileWriter writer = new FileWriter(location, true)) {
                    writer.write(content);
                    System.out.println("  - Successfully wrote to: " + location);
                }
            } catch (IOException e) {
                System.err.println("  - Error writing to " + location + ": " + e.getMessage());
            }
        }
    }
    
    private void checkForExistingEvidence() {
        String[] locations = {
            "/tmp/runtime_reuse_evidence.txt",
            "/tmp/accumulated_resources.txt",
            "/tmp/simple_evidence.txt"
        };
        
        for (String location : locations) {
            try {
                File file = new File(location);
                if (file.exists()) {
                    long fileSize = file.length();
                    long lineCount = Files.lines(Paths.get(location)).count();
                    System.out.println("Existing evidence file found: " + location);
                    System.out.println("  - File size: " + fileSize + " bytes");
                    System.out.println("  - Total lines: " + lineCount);
                } else {
                    System.out.println("No existing evidence file found: " + location);
                }
            } catch (IOException e) {
                System.err.println("Error checking " + location + ": " + e.getMessage());
            }
        }
    }
}
