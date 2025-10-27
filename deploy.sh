#!/bin/bash

# 1. Build the project
echo "Building the project..."
./gradlew build

# Check if the build was successful
if [ $? -ne 0 ]; then
  echo "Gradle build failed. Aborting deployment."
  exit 1
fi

# Variables for deployment
PEM_KEY_PATH="~/Downloads/linkbig-ht-20.pem"
JAR_FILE="build/libs/hiresense-0.0.1-SNAPSHOT.jar"
EC2_USER="ec2-user"
EC2_HOST="ec2-54-234-31-195.compute-1.amazonaws.com"
REMOTE_JAR_PATH="~/hiresense-0.0.1-SNAPSHOT.jar"

# 2. Deploy the JAR file
echo "Deploying JAR file to EC2 instance..."
scp -i "$PEM_KEY_PATH" "$JAR_FILE" "${EC2_USER}@${EC2_HOST}:${REMOTE_JAR_PATH}"

if [ $? -ne 0 ]; then
  echo "SCP failed. Aborting deployment."
  exit 1
fi

# 3. Connect to EC2 and run the application
echo "Connecting to EC2 and running the application..."
ssh -i "$PEM_KEY_PATH" "${EC2_USER}@${EC2_HOST}" << EOF
  echo "Killing any existing application process..."
  pkill -f hiresense-0.0.1-SNAPSHOT.jar

  echo "Starting the application..."
  nohup java -jar ${REMOTE_JAR_PATH} > ~/app.log 2>&1 &
  echo "Application started. Check app.log for details."
EOF

echo "Deployment complete."
