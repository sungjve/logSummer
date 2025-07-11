import boto3
import concurrent.futures
import time

# Lambda 함수 이름
FUNCTION_NAME = 'logSummer_sender_lamda'
# 호출할 인스턴스 수
INVOCATION_COUNT = 800

# Lambda 클라이언트 초기화 (서울 리전 명시)
lambda_client = boto3.client('lambda', region_name='ap-northeast-2')

def invoke_single_lambda(i):
    try:
        # InvocationType='Event'는 비동기 호출을 의미합니다. 응답을 기다리지 않습니다.
        response = lambda_client.invoke(
            FunctionName=FUNCTION_NAME,
            InvocationType='Event',  # 비동기 호출
            Payload='{}'  # Lambda 함수가 이벤트를 사용하지 않으므로 빈 JSON
        )
        # print(f"Invocation {i+1} successful. StatusCode: {response['StatusCode']}") # 너무 많은 출력을 막기 위해 주석 처리
        return True
    except Exception as e:
        print(f"Invocation {i+1} failed: {e}")
        return False

if __name__ == "__main__":
    print(f"Invoking {INVOCATION_COUNT} instances of {FUNCTION_NAME}...")
    start_time = time.time()

    # ThreadPoolExecutor를 사용하여 병렬로 Lambda 함수 호출
    # max_workers를 INVOCATION_COUNT와 동일하게 설정하여 최대한 병렬로 실행
    with concurrent.futures.ThreadPoolExecutor(max_workers=INVOCATION_COUNT) as executor:
        futures = [executor.submit(invoke_single_lambda, i) for i in range(INVOCATION_COUNT)]
        
        # 모든 호출 요청이 완료될 때까지 기다립니다.
        # 비동기 호출이므로 실제 Lambda 실행 완료를 의미하지는 않습니다.
        # 단지 호출 요청이 AWS Lambda 서비스에 전달되었음을 의미합니다.
        results = [f.result() for f in concurrent.futures.as_completed(futures)]

    end_time = time.time()
    
    success_count = sum(results)
    failure_count = INVOCATION_COUNT - success_count

    print(f"\nFinished invoking {INVOCATION_COUNT} Lambda instances in {end_time - start_time:.2f} seconds.")
    print(f"Successful invocations: {success_count}")
    print(f"Failed invocations: {failure_count}")
    print("Check Lambda CloudWatch logs for actual execution details.")