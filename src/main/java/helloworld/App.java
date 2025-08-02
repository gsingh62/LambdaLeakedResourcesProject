package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class App implements RequestHandler<Object, Object> {
    @Override
    public Object handleRequest(Object input, Context context) {
        // Create unclosed resources
        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream("/tmp/leaked_file.txt");
            fos.write("This file stream will never be closed".getBytes());
            // Intentionally not closing the stream
        } catch (Exception e) {
            System.err.println("Error creating leaked file stream: " + e.getMessage());
        }
        
        System.out.println("Lambda function is about to sleep for 15 seconds (exceeding 10s timeout)...");
        
        // Sleep for longer than the Lambda timeout (10 seconds)
        try {
            Thread.sleep(15000); // 15 seconds
        } catch (InterruptedException e) {
            System.out.println("Thread was interrupted: " + e.getMessage());
        }
        
        return "This should never be reached due to timeout";
    }
}
