@echo off
echo Creating MongoDB collections by generating test data...
echo.

echo 1. Checking if application is running...
curl -s http://localhost:8080/api/events/health
if %errorlevel% neq 0 (
    echo Application is not running! Please start it first with: mvn spring-boot:run
    pause
    exit /b 1
)

echo.
echo 2. Generating test data to create collections...
curl -X POST -H "Content-Type: application/json" http://localhost:8080/api/events/generate-test-data

echo.
echo 3. Creating user events...
curl -X POST -H "Content-Type: application/json" -d "{\"userId\":\"user1\",\"eventType\":\"USER_LOGIN\",\"userName\":\"John Doe\",\"email\":\"john@example.com\"}" http://localhost:8080/api/events/users

echo.
echo 4. Creating order events...
curl -X POST -H "Content-Type: application/json" -d "{\"orderId\":\"order1\",\"userId\":\"user1\",\"productName\":\"Laptop\",\"quantity\":1,\"price\":999.99}" http://localhost:8080/api/events/orders

echo.
echo 5. Verifying collections were created...
echo User Events:
curl -s http://localhost:8080/api/events/users | jq .

echo.
echo Order Events:
curl -s http://localhost:8080/api/events/orders | jq .

echo.
echo Collections created successfully!
pause