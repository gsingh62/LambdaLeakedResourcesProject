# Lambda Leaked Resources Project

A Java AWS Lambda project designed to test and understand Lambda runtime behavior, particularly focusing on container reuse, resource leaks, timeout handling, and error propagation.

## 🎯 Project Overview

This project demonstrates various Lambda runtime behaviors including:
- Container reuse after exceptions and timeouts
- Resource leak accumulation across invocations
- Error handling and propagation
- Timeout enforcement
- Platform compatibility for Apple Silicon

## 🚀 Quick Start

### Prerequisites
- Java 11 or higher
- AWS SAM CLI
- Docker
- Gradle

### Setup
```bash
# Clone the repository
git clone git@github.com:gsingh62/LambdaLeakedResourcesProject.git
cd LambdaLeakedResourcesProject

# Build the project
./gradlew clean shadowJar

# Build Docker image
sam build --use-container

# Test the Lambda function
sam local invoke FailingFunction --event events/event.json
```

## 📁 Project Structure

```
LambdaLeakedResourcesProject/
├── src/
│   ├── main/java/helloworld/
│   │   └── App.java                 # Main Lambda function
│   └── test/java/helloworld/
│       └── AppTest.java             # Unit tests
├── events/
│   ├── event.json                   # API Gateway test event
│   └── simple-event.json            # Simple string test event
├── build.gradle                     # Gradle build configuration
├── Dockerfile                       # Docker configuration
├── template.yaml                    # SAM template
├── platform-configuration.json      # Platform configuration for Apple Silicon
└── README.md                        # This file
```

## 🧪 Testing Scenarios

### 1. Exception Handling Test
```java
// Function throws RuntimeException
throw new RuntimeException("Test exception for error handling");
```

**Expected Behavior:**
- Lambda catches and reports the exception
- Container is reused for subsequent invocations
- Full stack trace is provided in error response

### 2. Resource Leak Test
```java
// Creates unclosed file streams and hanging threads
FileOutputStream fos = new FileOutputStream("/tmp/leaked_file.txt");
fos.write("This file stream will never be closed".getBytes());
// Intentionally not closing the stream
```

**Expected Behavior:**
- Resources persist across invocations
- Container continues to be reused
- Memory usage may increase over time

### 3. Timeout Test
```java
// Function sleeps for 15 seconds (exceeds 10s timeout)
Thread.sleep(15000);
```

**Expected Behavior:**
- Function times out exactly at 10 seconds
- Container is reused after timeout
- Full timeout period is billed

## 📊 Key Learnings

### Container Reuse Behavior
- ✅ **Containers are reused** even after exceptions and timeouts
- ✅ **Warm starts** provide fast initialization (~1ms Init Duration)
- ✅ **Resource leaks persist** across invocations
- ⚠️ **No automatic cleanup** of leaked resources

### Error Handling
- ✅ **Exceptions are properly caught** and reported
- ✅ **Full stack traces** are provided
- ✅ **Error metadata** includes error type and message
- ✅ **Container remains functional** after exceptions

### Timeout Enforcement
- ✅ **Strict timeout enforcement** at configured limit (10 seconds)
- ✅ **Full timeout period is billed** regardless of actual execution
- ✅ **Container reuse continues** after timeouts
- ⚠️ **No response returned** when timeout occurs

### Resource Management
- ⚠️ **Unclosed resources persist** across invocations
- ⚠️ **Memory leaks accumulate** in long-running containers
- ⚠️ **File handles remain open** until container replacement
- ⚠️ **Threads continue running** in background

## 🔧 Configuration Files

### Dockerfile
```dockerfile
FROM public.ecr.aws/lambda/java:11
COPY build/libs/my-java-lambda.jar ${LAMBDA_TASK_ROOT}/lib/my-java-lambda.jar
ENV _HANDLER=helloworld.App::handleRequest
ENTRYPOINT ["/lambda-entrypoint.sh"]
```

### template.yaml
```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Resources:
  FailingFunction:
    Type: AWS::Serverless::Function
    Properties:
      PackageType: Image
      Timeout: 10
      MemorySize: 512
      ImageUri: failingfunction:latest
      ImageConfig:
        Command: ["helloworld.App::handleRequest"]
    Metadata:
      Dockerfile: Dockerfile
      DockerContext: .
      DockerTag: latest
      DockerBuildArgs:
        DOCKER_DEFAULT_PLATFORM: linux/arm64
```

### build.gradle
```gradle
plugins {
    id 'java'
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation 'com.amazonaws:aws-lambda-java-core:1.2.1'
    implementation 'com.amazonaws:aws-lambda-java-events:3.11.0'
    testImplementation 'junit:junit:4.12'
}

shadowJar {
    archiveBaseName.set('my-java-lambda')
    archiveClassifier.set('')
    archiveVersion.set('')
    manifest {
        attributes 'Main-Class': 'helloworld.App'
    }
}
```

## 🍎 Apple Silicon Compatibility

### Platform Configuration
```json
{
  "platform": "linux/arm64"
}
```

### Why It's Needed
- Resolves platform mismatch warnings
- Ensures native ARM64 performance on Apple Silicon
- Eliminates Docker compatibility issues

## 📈 Performance Metrics

### Typical Invocation Results
```
START RequestId: xxx Version: $LATEST
END RequestId: xxx
REPORT RequestId: xxx
  Init Duration: 1.07 ms
  Duration: 10000.00 ms
  Billed Duration: 10000 ms
  Memory Size: 512 MB
  Max Memory Used: 512 MB
```

### Key Observations
- **Init Duration**: ~1ms (warm start - container reuse)
- **Duration**: Actual execution time
- **Billed Duration**: Time you're charged for
- **Memory Usage**: As configured in template

## 🚨 Production Implications

### Resource Management Best Practices
1. **Always close resources** (files, streams, connections)
2. **Use try-with-resources** for automatic cleanup
3. **Implement proper error handling**
4. **Monitor memory usage** in long-running containers
5. **Set appropriate timeouts** to control costs

### Cost Considerations
- **Timeout billing**: You pay for full timeout period
- **Memory billing**: Based on configured memory size
- **Container reuse**: Reduces cold start costs
- **Resource leaks**: Can increase memory costs over time

## 🔍 Troubleshooting

### Common Issues

1. **Platform Mismatch Warnings**
   - Solution: Use `platform-configuration.json` or `DockerBuildArgs`

2. **Class Not Found Errors**
   - Solution: Ensure JAR is in correct location (`/lib/` directory)
   - Solution: Use shadow JAR with all dependencies

3. **Java Version Errors**
   - Solution: Set `targetCompatibility = JavaVersion.VERSION_11`

4. **Build Failures**
   - Solution: Run `./gradlew clean shadowJar` before `sam build`

## 📚 References

- [AWS Lambda Java Runtime](https://docs.aws.amazon.com/lambda/latest/dg/java-programming-model.html)
- [SAM Local Development](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
- [Docker Platform Configuration](https://docs.docker.com/engine/reference/commandline/build/#platform)
- [Gradle Shadow Plugin](https://github.com/johnrengelman/shadow)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Note**: This project is designed for educational purposes to understand Lambda runtime behavior. The resource leak scenarios demonstrated should not be used in production environments.
