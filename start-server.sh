#!/bin/bash

echo "=========================================="
echo "Hiresense Backend Server 시작"
echo "=========================================="

# 로컬 IP 주소 확인
LOCAL_IP=$(ifconfig | grep "inet " | grep -v 127.0.0.1 | head -1 | awk '{print $2}')
if [ -z "$LOCAL_IP" ]; then
    LOCAL_IP="localhost"
fi

echo ""
echo "서버 주소:"
echo "  - 로컬: http://localhost:8000"
echo "  - 네트워크: http://$LOCAL_IP:8000"
echo ""
echo "Expo Go에서 사용할 API Base URL:"
echo "  http://$LOCAL_IP:8000"
echo ""
echo "Swagger UI:"
echo "  http://localhost:8000/swagger-ui.html"
echo ""
echo "=========================================="
echo ""

# 서버 실행
./gradlew bootRun

