package configuration.db.postgres;

import configuration.Config;
import lombok.Getter;

public enum PostgresSchema {
    SECURITY_SERVICE(Config.POSTGRES_URL + "?currentSchema=security_service"),
    CONFIGURATION_MANAGER(Config.POSTGRES_URL + "?currentSchema=configuration_manager"),
    CONTENT_MANAGER(Config.POSTGRES_URL + "?currentSchema=content_manager");

    @Getter
    private final String url;

    PostgresSchema(String url) {
        this.url = url;
    }
}
