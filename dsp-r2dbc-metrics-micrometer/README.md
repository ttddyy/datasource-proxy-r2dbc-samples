# About

[datasource-proxy-r2dbc][datasource-proxy-r2dbc] metrics sample application

This sample populates following metrics:

- Time took to create a connection
- Commit and rollback counts
- Executed query count
- Slow query count

Metrics are accessible via JMX and metrics endpoint(`/actuator/metrics`).

Also, logs slow queries that took more than 500ms.


## Implementation

**[MetricsExecutionListener](./src/main/java/net/ttddyy/MetricsExecutionListener.java)**

An implementation of [`LifeCycleListener`][LifeCycleListener] which populates micrometer metrics.


## Sample metrics images

*JMX entries:*

![JMX entries](images/jmx-entries.png)

*Connection metrics on JMX:*

![JMX Connection](images/jmx-connection.png)

*Query metrics on JMX:*

![JMX Query](images/jmx-query.png)

*Connection metrics on actuator (`/actuator/metrics/r2dbc.connection`):*

![Actuator Connection](images/actuator-connection.png)

*Transaction metrics on actuator (`/actuator/metrics/r2dbc.transaction`):*

![Actuator Transaction](images/actuator-transaction.png)

*Slow query log:*
![Slow query log](images/slow-query-log.png)

## How to run

Start `Application`

Access endpoints
```shell
> curl localhost:8080
> curl localhost:8080/batch
> curl localhost:8080/transaction
> curl localhost:8080/rollback
> curl localhost:8080/slow
```

Metrics actuator endpoint

```shell
> curl localhost:8080/actuator/metrics
```

----

[datasource-proxy-r2dbc]: https://github.com/ttddyy/datasource-proxy-r2dbc 
[LifeCycleListener]: https://github.com/ttddyy/datasource-proxy-r2dbc/blob/master/src/main/java/net/ttddyy/dsproxy/r2dbc/support/LifeCycleListener.java