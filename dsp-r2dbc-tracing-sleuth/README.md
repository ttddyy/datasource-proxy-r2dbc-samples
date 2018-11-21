
Sample tracing application

## 

```shell
# Start zipkin
> docker run -d -p 9411:9411 openzipkin/zipkin
```

> curl localhost:8080
> curl localhost:8080/batch
> curl localhost:8080/transaction
> curl localhost:8080/rollback