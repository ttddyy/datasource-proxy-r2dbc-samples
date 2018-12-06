package net.ttddyy;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.r2dbc.client.R2dbc;
import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.ProxyConnectionFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;

/**
 * Metrics sample application
 */
@SpringBootApplication
@Timed
@RestController
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Autowired
    R2dbc r2dbc;

    static Mono<List<Integer>> extractColumns(Result result) {
        return Flux.from(result
                .map((row, rowMetadata) -> {
                    return row.get("value", Integer.class);
                }))
                .collectList();
    }

    @RequestMapping("/")
    Flux<?> select() {
        return this.r2dbc.withHandle(handle -> handle
                .createQuery("SELECT value FROM test;")
                .mapResult(Application::extractColumns)
        );
    }

    @RequestMapping("/batch")
    Flux<?> batch() {
        return this.r2dbc.withHandle(handle -> handle
                .createBatch()
                .add("INSERT INTO test VALUES(200)")
                .add("SELECT value FROM test")
                .mapResult(Mono::just));
    }

    @RequestMapping("/transaction")
    Flux<?> transaction() {
        return this.r2dbc.withHandle(handle -> handle
                .inTransaction(h1 -> h1.execute("INSERT INTO test VALUES ($1)", 200))
        );
    }

    @RequestMapping("/rollback")
    Flux<?> rollback() {
        return this.r2dbc.withHandle(handle -> handle
                .inTransaction(h1 -> h1
                        .select("SELECT value FROM test")
                        .<Object>mapResult(Application::extractColumns)

                        .concatWith(h1.execute("INSERT INTO test VALUES ($1)", 200))
                        .concatWith(h1.select("SELECT value FROM test")
                                .mapResult(Application::extractColumns))

                        .concatWith(Mono.error(new Exception())))

                .onErrorResume(t -> handle.select("SELECT value FROM test")
                        .mapResult(Application::extractColumns)));

    }

    @RequestMapping("/slow")
    Flux<?> slow() {
        return this.r2dbc.withHandle(handle -> handle
                .createQuery("CALL SLEEP(700);")  // sleep more than 500ms threshold
                .mapResult(Mono::just));
    }

    @Bean
    DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }

    @Bean
    CommandLineRunner bootstrap(DataSource dataSource) {
        return args -> {
            JdbcOperations jdbcOperations = new JdbcTemplate(dataSource);
            jdbcOperations.execute("DROP TABLE IF EXISTS test");
            jdbcOperations.execute("CREATE TABLE test ( value INTEGER )");
            jdbcOperations.execute("INSERT INTO test VALUES (100)");
            jdbcOperations.execute("INSERT INTO test VALUES (200)");

            // create sleep function for slow query
            jdbcOperations.execute("CREATE ALIAS SLEEP FOR \"java.lang.Thread.sleep(long)\"");
        };
    }

    @Bean
    ConnectionFactory connectionFactory(MeterRegistry registry) {
        H2ConnectionConfiguration h2Configuration = H2ConnectionConfiguration.builder()
                .username("sa")
                .password("")
                .inMemory("testdb")
                .build();


        ConnectionFactory connectionFactory = new H2ConnectionFactory(h2Configuration);

        MetricsExecutionListener metricsListener = new MetricsExecutionListener(registry, Duration.ofMillis(500));

        ConnectionFactory proxyConnectionFactory =
                ProxyConnectionFactoryBuilder.create(connectionFactory)
                        .listener(metricsListener)
                        .build();

        return proxyConnectionFactory;
    }

    @Bean
    R2dbc r2dbcClient(ConnectionFactory connectionFactory) {
        return new R2dbc(connectionFactory);
    }


}
