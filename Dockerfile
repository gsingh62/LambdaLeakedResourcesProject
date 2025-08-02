FROM public.ecr.aws/lambda/java:11

# Copy the built JAR file to the lib directory
COPY build/libs/my-java-lambda.jar ${LAMBDA_TASK_ROOT}/lib/my-java-lambda.jar

# Set the handler class
ENV _HANDLER=helloworld.App::handleRequest

# Use the Lambda entrypoint
ENTRYPOINT ["/lambda-entrypoint.sh"]
