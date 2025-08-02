#!/bin/bash

echo "Running Lambda function and capturing evidence..."

# Run the function in background
sam local invoke FailingFunction --event events/event.json &
SAM_PID=$!

# Wait a moment for the container to be created
sleep 3

# Find the most recent lambda container
CONTAINER_ID=$(docker ps --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Names}}" | grep lambda | tail -1 | awk '{print $1}')

if [ ! -z "$CONTAINER_ID" ]; then
    echo "Found container: $CONTAINER_ID"
    
    # Wait for function to complete
    wait $SAM_PID
    
    # Copy evidence files
    echo "Copying evidence files..."
    docker cp $CONTAINER_ID:/tmp/runtime_reuse_evidence.txt ./runtime_reuse_evidence.txt 2>/dev/null || echo "Could not copy runtime_reuse_evidence.txt"
    docker cp $CONTAINER_ID:/tmp/accumulated_resources.txt ./accumulated_resources.txt 2>/dev/null || echo "Could not copy accumulated_resources.txt"
    docker cp $CONTAINER_ID:/tmp/simple_evidence.txt ./simple_evidence.txt 2>/dev/null || echo "Could not copy simple_evidence.txt"
    
    echo "Evidence files copied to current directory:"
    ls -la *.txt 2>/dev/null || echo "No evidence files found"
else
    echo "No lambda container found"
fi 