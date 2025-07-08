
import json
import requests
import os
import random
import time

# 요청을 보낼 목표 URL을 직접 지정
TARGET_URL = "http://3.39.24.170:8080/log"

# 요청 헤더
HEADERS = {
    "Content-Type": "application/json"
}

# 요청을 보낼 횟수 (Lambda 한 번 실행당)
REQUEST_COUNT = 100

def lambda_handler(event, context):
    print(f"Target URL: {TARGET_URL}")
    print(f"Sending {REQUEST_COUNT} requests...")

    success_count = 0
    failure_count = 0

    # 미리 정의된 서비스 이름과 메시지 샘플
    services = ["order-service", "payment-service", "user-service", "delivery-service"]
    messages = ["User logged in", "Payment processed successfully", "Order created", "Failed to connect to database", "Item added to cart"]

    for i in range(REQUEST_COUNT):
        # 보낼 로그 데이터 랜덤 생성 (serviceName을 카멜 케이스로)
        log_data = {
            "serviceName": random.choice(services),
            "message": f"Log entry {i+1}: {random.choice(messages)}"
        }

        try:
            response = requests.post(TARGET_URL, headers=HEADERS, data=json.dumps(log_data), timeout=10)
            if response.status_code in [200, 201, 202]:
                success_count += 1
            else:
                failure_count += 1
                print(f"Request failed with status {response.status_code}: {response.text}")

        except requests.exceptions.RequestException as e:
            failure_count += 1
            print(f"Request exception: {e}")

    print(f"Finished sending requests. Success: {success_count}, Failure: {failure_count}")

    return {
        "statusCode": 200,
        "body": json.dumps(f"Completed. Success: {success_count}, Failure: {failure_count}")
    }
