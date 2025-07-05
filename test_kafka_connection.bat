@echo off
echo Testing Kafka Connection...
echo.

echo 1. Checking if Docker containers are running...
docker ps | findstr kafka
if %errorlevel% neq 0 (
    echo Kafka containers not found! Starting them...
    docker-compose up -d zookeeper kafka kafka-ui
    echo Waiting for Kafka to start...
    timeout /t 60 /nobreak > nul
) else (
    echo Kafka containers are running.
)

echo.
echo 2. Testing Kafka connection...
echo Creating test topic...
docker exec -it kafka kafka-topics --create --topic test-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

echo.
echo 3. Listing topics...
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

echo.
echo 4. Testing producer/consumer...
echo Testing message | docker exec -i kafka kafka-console-producer --topic test-topic --bootstrap-server localhost:9092
docker exec -it kafka kafka-console-consumer --topic test-topic --bootstrap-server localhost:9092 --from-beginning --max-messages 1

echo.
echo 5. Cleaning up test topic...
docker exec -it kafka kafka-topics --delete --topic test-topic --bootstrap-server localhost:9092

echo.
echo If all tests passed, Kafka is working correctly!
pause