package net.ttddyy;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.support.LifeCycleListener;
import net.ttddyy.dsproxy.r2dbc.support.QueryExecutionInfoFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Listener to populate micrometer metrics and logs slow query.
 *
 * @author Tadaya Tsuyukubo
 */
public class MetricsExecutionListener implements LifeCycleListener {

    private static final Logger logger = LoggerFactory.getLogger(MetricsExecutionListener.class);

    private MeterRegistry registry;
    private String metricNamePrefix = "r2dbc.";
    private Duration slowQueryThreshold = Duration.ofSeconds(-1);  // default won't match any query
    private QueryExecutionInfoFormatter queryFormatter = new QueryExecutionInfoFormatter()
            .showTime()
            .showConnection()
            .showQuery();

    public MetricsExecutionListener(MeterRegistry registry) {
        this.registry = registry;
    }

    public MetricsExecutionListener(MeterRegistry registry, Duration slowQueryThreshold) {
        this.registry = registry;
        this.slowQueryThreshold = slowQueryThreshold;
    }

    @Override
    public void beforeCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
        Timer.Sample sample = Timer.start();
        methodExecutionInfo.addCustomValue("connectionCreate", sample);
    }

    @Override
    public void afterCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
        Timer.Sample sample = methodExecutionInfo.getCustomValue("connectionCreate", Timer.Sample.class);

        Timer timer = Timer
                .builder(this.metricNamePrefix + "connection")
                .description("Time to create(acquire) a connection")
                .tags("event", "create")
                .register(this.registry);

        sample.stop(timer);
    }

    @Override
    public void afterCommitTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
        Counter counter = Counter
                .builder(this.metricNamePrefix + "transaction")
                .description("Num of transactions")
                .tags("event", "commit")
                .register(registry);
        counter.increment();
    }

    @Override
    public void afterRollbackTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
        incrementRollbackCounter();
    }

    @Override
    public void afterRollbackTransactionToSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
        incrementRollbackCounter();
    }

    private void incrementRollbackCounter() {
        Counter counter = Counter
                .builder(this.metricNamePrefix + "transaction")
                .description("Num of transactions")
                .tags("event", "rollback")
                .register(registry);
        counter.increment();
    }


    @Override
    public void afterExecuteOnBatch(QueryExecutionInfo queryExecutionInfo) {
        afterExecuteQuery(queryExecutionInfo);
    }

    @Override
    public void afterExecuteOnStatement(QueryExecutionInfo queryExecutionInfo) {
        afterExecuteQuery(queryExecutionInfo);
    }

    private void afterExecuteQuery(QueryExecutionInfo queryExecutionInfo) {
        Counter success = Counter
                .builder(this.metricNamePrefix + "query")
                .description("Num of executed queries")
                .register(registry);
        success.increment();


        // slow query
        if (this.slowQueryThreshold.minus(queryExecutionInfo.getExecuteDuration()).isNegative()) {
            Counter slowQueryCounter = Counter
                    .builder(this.metricNamePrefix + "query.slow")
                    .description("Slow query count that took more than threshold")
                    .register(registry);
            slowQueryCounter.increment();


            StringBuilder sb = new StringBuilder();
            sb.append("SlowQuery: ");
            sb.append(this.queryFormatter.format(queryExecutionInfo));
            logger.info(sb.toString());
        }
    }

    public void setRegistry(MeterRegistry registry) {
        this.registry = registry;
    }

    public void setMetricNamePrefix(String metricNamePrefix) {
        this.metricNamePrefix = metricNamePrefix;
    }

    public void setSlowQueryThreshold(Duration slowQueryThreshold) {
        this.slowQueryThreshold = slowQueryThreshold;
    }

}
