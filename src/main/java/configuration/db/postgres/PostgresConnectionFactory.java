package configuration.db.postgres;

import configuration.Config;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostgresConnectionFactory {

    private static final Map<PostgresSchema, BasicDataSource> DATASOURCES = new ConcurrentHashMap<>();

    private static volatile boolean isInitialized = false;

    private static synchronized void initializeDataSourcesIfNeeded() {
        if (isInitialized) {
            return;
        }

        for (final PostgresSchema schema : PostgresSchema.values()) {
            final BasicDataSource dataSource = new BasicDataSource();
            dataSource.setUrl(schema.getUrl());
            dataSource.setUsername(Config.POSTGRES_USER);
            dataSource.setPassword(Config.POSTGRES_PASSWORD);
            dataSource.setMinIdle(Config.POSTGRES_MIN_IDLE_CONNECTIONS);
            dataSource.setMaxIdle(Config.POSTGRES_MAX_IDLE_CONNECTIONS);
            dataSource.setMaxOpenPreparedStatements(Config.POSTGRES_MAX_OPEN_PREPARED_STATEMENTS);
            dataSource.setDefaultAutoCommit(false);
            DATASOURCES.put(schema, dataSource);
        }

        isInitialized = true;
    }

    @SneakyThrows
    public static Connection getConnectionForSchema(final PostgresSchema schema) {
        initializeDataSourcesIfNeeded();
        return DATASOURCES.get(schema).getConnection();
    }
}