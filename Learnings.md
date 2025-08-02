# 🚀 Debugging Java Lambda Container Failures Locally: What I Learned the Hard Way

When testing a Java AWS Lambda function packaged as a Docker container, I spent days running into a frustrating error:

```
Class not found: helloworld.App
java.lang.ClassNotFoundException: helloworld.App
```

Despite having the correct `App.class`, handler, and Docker image — the function just wouldn’t run locally with `sam local invoke`.

This document is a breakdown of what went wrong, what I learned, and how to avoid it.

---

## ❌ Symptoms

* `sam local invoke` gave me `ClassNotFoundException`
* Docker logs showed `handler=` was blank
* The function seemed to build fine, but never actually ran

---

## 🔍 Root Causes

| Problem                 | Explanation                                                                                                           |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------- |
| **Platform mismatch**   | My Mac uses Apple Silicon (`arm64`), but the Lambda image defaults to `amd64`, causing subtle runtime breakage        |
| **Incorrect JAR path**  | Lambda expects JARs in `/var/task/lib/` — I had placed it in `/var/task/` directly                                    |
| **Not a fat JAR**       | I was building a regular JAR without dependencies, so the AWS runtime couldn't find Lambda APIs like `RequestHandler` |
| **Java 21 bytecode**    | My code was compiled with Java 21, but the Lambda runtime only supports Java 11                                       |
| **SAM handler missing** | Even though my Dockerfile had a handler, `sam local invoke` ignored it unless explicitly set in `template.yaml`       |

---

## ✅ Fixes That Made It Work

### Dockerfile

```dockerfile
FROM public.ecr.aws/lambda/java:11
COPY build/libs/my-java-lambda.jar ${LAMBDA_TASK_ROOT}/lib/my-java-lambda.jar
ENV _HANDLER=helloworld.App::handleRequest
ENTRYPOINT ["/lambda-entrypoint.sh"]
```

### `build.gradle`

```groovy
plugins {
    id 'java'
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

shadowJar {
    manifest {
        attributes 'Main-Class': 'helloworld.App'
    }
}
```

### `template.yaml`

```yaml
Resources:
  FailingFunction:
    Type: AWS::Serverless::Function
    Properties:
      PackageType: Image
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

---

## 🧠 How I’ll Debug Faster Next Time

| Situation                 | What I’ll Check                                      |
| ------------------------- | ---------------------------------------------------- |
| Java function not running | `jar tf` and confirm `App.class` exists              |
| `handler=` is blank       | Use `template.yaml > ImageConfig.Command` explicitly |
| Weird Docker bugs         | Match platform with `DOCKER_DEFAULT_PLATFORM`        |
| AWS SDKs missing          | Use `shadowJar` to bundle dependencies               |
| Java version mismatch     | Set Java 11 in `build.gradle` every time             |

---

## 🧵 Why This Made Me Better

This wasn't about “getting it right” the first time. It was about:

* Learning how Lambda’s Java runtime actually works
* Understanding container build systems and platforms
* Getting better at trusting-but-verifying SAM and Docker behavior
* Becoming methodical in debugging

---

## ✅ TL;DR

If you're getting `ClassNotFoundException` in a containerized Java Lambda:

* ✅ Use `shadowJar`
* ✅ Put your JAR in `/var/task/lib/`
* ✅ Match Docker build platform (`arm64` vs `amd64`)
* ✅ Compile with Java 11
* ✅ Set `ImageConfig.Command` in `template.yaml`

You can’t debug what you don’t understand. But now I do.
