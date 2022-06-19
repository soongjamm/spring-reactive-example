# Spring Reactive 
[(JPoint 2021) Josh Long - Spring Reactive](https://www.youtube.com/watch?v=aM-l68NqguA)
```bash
.
├── customers
├── gateway
└── orders
```

### Command
```gradle
# 실행
./gradlew customers
./gradlew orders
./gradlew gateway
```

```bash
# API Gateway proxy (get customers)
curl -H"Host:hello.spring.io" http://localhost:9999/proxy | jq

# API Gateway orchestration (get orders by customers)
curl -H"Host:hello.spring.io" http://localhost:9999/cos | jq 
```

### 실험
API Gateway 에서 요청(모든 고객의 주문정보를 조회)을 받으면 Customer * 1, Order * 9 회 조회를 하게 되어있다.  
이때, Order 서버는 요청당 3초의 지연이 발생한다. 

만약 Blocking IO 환경이라면 다음과 같이 진행된다.
- Customer 로 전체 고객을 조회한다.
- Customer1의 Order 조회를 요청한다.
- 3초 뒤 조회가 끝나면 Customer2의 Order 조회를 요청한다.
- 3초 뒤 조회가 끝나면 Customer3의 Order 조회를 요청한다.
- 3초 뒤 조회가 끝나면 Customer4의 Order 조회를 요청한다.
- ... 이런식으로 Order 조회를 요청하는데만 27초가 소요된다.
- 모든 작업이 끝나고 Controller 의 종료 로그가 찍힌다. 
- 클라이언트에게 결과가 노출된다.

Spring Reactive 을 이용한 Async/Non-Blocking IO 환경에서는 다음과 같았다.
- Customer 로 전체 고객을 조회한다.
- _아직 Order 조회가 처리되지 않았지만, 컨트롤러에서는 종료 로그가 찍혔다._
- Customer1의 Order 조회를 요청한다.
- Customer2의 Order 조회를 요청한다.
- Customer3의 Order 조회를 요청한다.
- ... 이런식으로 비동기적으로 Order에 요청을 전달한다. (이때, 모든 Order 조회 요청을 하나의 쓰레드가 처리하였다.)
- 27초 후, Order의 조회가 끝나자 API Gateway가 결과를 클라이언트에게 전달했다.

### 결과  
API Gateway 에서 모든 요청을 처리하는데는 Reactive : 3초, Blocking : 27초가 소요되었다.  
- RSocket 관련된 이슈인지, 요청은 빠르게 전달되었으나 Order에서 이상하게 요청을 병렬적으로 처리하지 못했다.   
- 그래서 최종적으로 모두 27초 이상 소요되었지만, 만약 Order 서버가 요청을 병렬로 처리했다면 Reactive 환경에서는 3-4초 내에 처리되었을 것이다.  
- 반면 Blocking 환경에서는 Order가 병렬로 작업을 처리하여도, 애초에 요청이 동시에 들어오지 못하기때문에 똑같이 27초 후에 종료되었을 것이다.

### 로그
API Gateway 의 작업별로 로그를 찍어보았는데, 이벤트루프 방식 답게 모든 `getOrdersForCustomer` 요청을 하나의 쓰레드로 처리했다.  
- Spring MVC 는 Thread Per Request 모델이기 때문에 `getOrdersForCustomer` 요청을 각각 다른 쓰레드가 처리했을 것이다.

또 다른 (별거아닌) 재밌는 점은 API Gateway 로 들어오는 요청의 처음(`getCustomerOrders`)은 쓰레드가 번갈아가면서 받다가, Order로 조회 요청(`getOrdersForCustomer`)은 이전에 요청을 처리하던 쓰레드가 다시 요청을 처리했다는 점이다.  
이유는 잘 모르겠지만.. 나중에 알아보자.  
```bash
2022-06-19 17:48:37.371  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getCustomerOrders || reactor-http-nio-3
2022-06-19 17:48:37.372  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getCustomers || reactor-http-nio-3
2022-06-19 17:48:37.377  INFO 75033 --- [ctor-http-nio-3] c.s.g.CustomerOrdersRestController       : Done with get CustomerOrder Request || reactor-http-nio-3 
2022-06-19 17:48:37.441  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=1 || reactor-http-nio-3
2022-06-19 17:48:37.453  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=2 || reactor-http-nio-3
2022-06-19 17:48:37.453  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=3 || reactor-http-nio-3
2022-06-19 17:48:37.453  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=4 || reactor-http-nio-3
2022-06-19 17:48:37.453  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=5 || reactor-http-nio-3
2022-06-19 17:48:37.453  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=6 || reactor-http-nio-3
2022-06-19 17:48:37.453  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=7 || reactor-http-nio-3
2022-06-19 17:48:37.453  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=8 || reactor-http-nio-3
2022-06-19 17:48:37.454  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=9 || reactor-http-nio-3
2022-06-19 17:48:38.821  INFO 75033 --- [ctor-http-nio-4] com.soongjamm.gateway.CrmClient          : getCustomerOrders || reactor-http-nio-4
2022-06-19 17:48:38.821  INFO 75033 --- [ctor-http-nio-4] com.soongjamm.gateway.CrmClient          : getCustomers || reactor-http-nio-4
2022-06-19 17:48:38.821  INFO 75033 --- [ctor-http-nio-4] c.s.g.CustomerOrdersRestController       : Done with get CustomerOrder Request || reactor-http-nio-4 
2022-06-19 17:48:38.825  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=1 || reactor-http-nio-3
2022-06-19 17:48:38.825  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=2 || reactor-http-nio-3
2022-06-19 17:48:38.826  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=3 || reactor-http-nio-3
2022-06-19 17:48:38.826  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=4 || reactor-http-nio-3
2022-06-19 17:48:38.826  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=5 || reactor-http-nio-3
2022-06-19 17:48:38.826  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=6 || reactor-http-nio-3
2022-06-19 17:48:38.827  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=7 || reactor-http-nio-3
2022-06-19 17:48:38.827  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=8 || reactor-http-nio-3
2022-06-19 17:48:38.827  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=9 || reactor-http-nio-3
2022-06-19 17:48:40.710  INFO 75033 --- [ctor-http-nio-5] com.soongjamm.gateway.CrmClient          : getCustomerOrders || reactor-http-nio-5
2022-06-19 17:48:40.710  INFO 75033 --- [ctor-http-nio-5] com.soongjamm.gateway.CrmClient          : getCustomers || reactor-http-nio-5
2022-06-19 17:48:40.711  INFO 75033 --- [ctor-http-nio-5] c.s.g.CustomerOrdersRestController       : Done with get CustomerOrder Request || reactor-http-nio-5 
2022-06-19 17:48:40.727  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=1 || reactor-http-nio-3
2022-06-19 17:48:40.728  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=2 || reactor-http-nio-3
2022-06-19 17:48:40.731  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=3 || reactor-http-nio-3
2022-06-19 17:48:40.732  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=4 || reactor-http-nio-3
2022-06-19 17:48:40.732  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=5 || reactor-http-nio-3
2022-06-19 17:48:40.733  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=6 || reactor-http-nio-3
2022-06-19 17:48:40.733  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=7 || reactor-http-nio-3
2022-06-19 17:48:40.734  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=8 || reactor-http-nio-3
2022-06-19 17:48:40.734  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=9 || reactor-http-nio-3
2022-06-19 17:48:42.319  INFO 75033 --- [ctor-http-nio-6] com.soongjamm.gateway.CrmClient          : getCustomerOrders || reactor-http-nio-6
2022-06-19 17:48:42.320  INFO 75033 --- [ctor-http-nio-6] com.soongjamm.gateway.CrmClient          : getCustomers || reactor-http-nio-6
2022-06-19 17:48:42.320  INFO 75033 --- [ctor-http-nio-6] c.s.g.CustomerOrdersRestController       : Done with get CustomerOrder Request || reactor-http-nio-6 
2022-06-19 17:48:42.325  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=1 || reactor-http-nio-3
2022-06-19 17:48:42.326  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=2 || reactor-http-nio-3
2022-06-19 17:48:42.326  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=3 || reactor-http-nio-3
2022-06-19 17:48:42.326  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=4 || reactor-http-nio-3
2022-06-19 17:48:42.327  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=5 || reactor-http-nio-3
2022-06-19 17:48:42.327  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=6 || reactor-http-nio-3
2022-06-19 17:48:42.327  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=7 || reactor-http-nio-3
2022-06-19 17:48:42.327  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=8 || reactor-http-nio-3
2022-06-19 17:48:42.328  INFO 75033 --- [ctor-http-nio-3] com.soongjamm.gateway.CrmClient          : getOrdersForCustomer - customerId=9 || reactor-http-nio-3
```