# About

[datasource-proxy-r2dbc][datasource-proxy-r2dbc] tracing sample application

## Implementation

**[TracingExecutionListener](./src/main/java/net/ttddyy/TracingExecutionListener.java)**

An implementation of [`LifeCycleListener`][LifeCycleListener] which creates tracing spans.


## Sample tracing images

Tracing query

![Tracing query](images/tracing-query.png)

Tracing transaction

![Tracing transaction](images/tracing-transaction.png)

Tracing transaction rollback

![Tracing transaction rollback](images/tracing-rollback.png)

Connection Span

![Connection span](images/span-connection.png)

Transaction Span

![Transaction span](images/span-transaction.png)

Quey Span (Single Query)

![Query span](images/span-query.png)

Quey Span (Batch Query)

![Query batch span](images/span-batch-query.png)

## How to run

Start zipkin
```shell
> docker run -d -p 9411:9411 openzipkin/zipkin
```

Start `Application`

Access endpoints
```shell
> curl localhost:8080
> curl localhost:8080/batch
> curl localhost:8080/transaction
> curl localhost:8080/rollback
```

----

[datasource-proxy-r2dbc]: https://github.com/ttddyy/datasource-proxy-r2dbc 
[LifeCycleListener]: https://github.com/ttddyy/datasource-proxy-r2dbc/blob/master/src/main/java/net/ttddyy/dsproxy/r2dbc/support/LifeCycleListener.java