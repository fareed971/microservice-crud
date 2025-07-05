@echo off
echo === Kafka Troubleshooting Script ===
echo.

echo 1. Checking current container status...
docker ps -a | findstr -i "kafka\|zookeeper"
echo.

echo 2. Checking Kafka logs for errors...
echo --- Kafka Logs (last 20 lines) ---
docker logs kafka --tail 20
echo.

echo 3. Checking Zookeeper logs...
echo --- Zookeeper Logs (last 10 lines) ---
docker logs zookeeper --tail 10
echo.

echo 4. Stopping all containers...
docker-compose down
echo.

echo 5. Removing old containers and networks...
docker container prune -f
docker network prune -f
echo.

echo 6. Starting with fixed configuration...
echo Starting Zookeeper first...
docker-compose -f docker-compose-fixed.yml up -d zookeeper
echo Waiting 15 seconds for Zookeeper...
timeout /t 15 /nobreak > nul

echo Starting Kafka...
docker-compose -f docker-compose-fixed.yml up -d kafka
echo Waiting 30 seconds for Kafka...
timeout /t 30 /nobreak > nul

echo Starting Kafka UI...
docker-compose -f docker-compose-fixed.yml up -d kafka-ui
echo Waiting 10 seconds for UI...
timeout /t 10 /nobreak > nul

echo.
echo 7. Final status check...
docker ps | findstr -i "kafka\|zookeeper"
echo.

echo 8. Testing Kafka connection...
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
if %errorlevel% equ 0 (
    echo SUCCESS: Kafka is working!
) else (
    echo FAILED: Kafka is still not working
    echo Check logs above for errors
)

echo.
echo 9. Kafka UI should be available at: http://localhost:8081
echo.
pause