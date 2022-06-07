package configuration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Config {

    public static final String ENV = System.getProperty("Env", "dev");

    public static final Properties PROPERTIES = new Properties();
    public static final String URI;
    public static final String UI_URI;
    public static final String UI_URI_CONSOLE;
    public static final String UI_URI_BACK_OFFICE;
    public static final String UI_SUBDOMAIN;
    public static final String POSTGRES_URL;
    public static final String POSTGRES_USER;
    public static final String POSTGRES_PASSWORD;
    public static final String HUB_URL;
    public static final String SAUCE_LABS_REST_API;
    public static final String SAUCE_LABS_USER;
    public static final String SAUCE_LABS_KEY;
    public static final String TUNNEL_START_PARAMS;
    public static final String TUNNEL_IDENTIFIER;
    public static final String XRAY_CLIENT_ID;
    public static final String XRAY_CLIENT_SECRET;
    public static final String XRAY_URL;
    public static final int POSTGRES_MIN_IDLE_CONNECTIONS;
    public static final int POSTGRES_MAX_IDLE_CONNECTIONS;
    public static final int POSTGRES_MAX_OPEN_PREPARED_STATEMENTS;
    public static final String REMOTE;
    public static final String REGION;
    private static final String PROPS_FILE_NAME = "configs/environments.properties";
    private static final String HTTPS = "https://";

    static {
        loadProperties();

        URI = PROPERTIES.getProperty(ENV + ".uri");
        UI_URI = PROPERTIES.getProperty(ENV + ".e2e.ui.uri");
        HUB_URL = HTTPS + PROPERTIES.getProperty("sauce.user") + ":"
                + PROPERTIES.getProperty("sauce.key") + "@ondemand."
                + PROPERTIES.getProperty("sauce.region") + ".saucelabs.com:443/wd/hub";
        SAUCE_LABS_REST_API = HTTPS + "api." + PROPERTIES.getProperty("sauce.region") + ".saucelabs.com/rest/v1";
        TUNNEL_IDENTIFIER = "tunnel_" + new Random().nextInt();
        TUNNEL_START_PARAMS = "-u " + PROPERTIES.getProperty("sauce.user") + " -k "
                + PROPERTIES.getProperty("sauce.key") + " -x " + SAUCE_LABS_REST_API + " -i "
                + TUNNEL_IDENTIFIER;
        SAUCE_LABS_USER = PROPERTIES.getProperty("sauce.user");
        SAUCE_LABS_KEY = PROPERTIES.getProperty("sauce.key");

        UI_URI_CONSOLE = PROPERTIES.getProperty(ENV + ".ui.uri.console");
        UI_URI_BACK_OFFICE = PROPERTIES.getProperty(ENV + ".ui.uri.backoffice");
        UI_SUBDOMAIN = PROPERTIES.getProperty(ENV + ".ui.subdomain");

        XRAY_CLIENT_ID = PROPERTIES.getProperty("xray.client.id");
        XRAY_CLIENT_SECRET = PROPERTIES.getProperty("xray.client.secret");
        XRAY_URL = PROPERTIES.getProperty("xray.url");
        POSTGRES_URL = PROPERTIES.getProperty(ENV + ".postgres.url");
        POSTGRES_USER = PROPERTIES.getProperty(ENV + ".postgres.user");
        POSTGRES_PASSWORD = PROPERTIES.getProperty(ENV + ".postgres.password");
        POSTGRES_MIN_IDLE_CONNECTIONS = Integer.parseInt(PROPERTIES.getProperty(ENV + ".postgres.min-idle-connections"));
        POSTGRES_MAX_IDLE_CONNECTIONS = Integer.parseInt(PROPERTIES.getProperty(ENV + ".postgres.max-idle-connections"));
        POSTGRES_MAX_OPEN_PREPARED_STATEMENTS = Integer.parseInt(PROPERTIES.getProperty(ENV + ".postgres.max-open-prepared-statements"));
        REMOTE = PROPERTIES.getProperty("remote");

        REGION = PROPERTIES.getProperty("region");

    }

    private static void loadProperties() {
        try (final InputStream inputStream = Config.class.getClassLoader().getResourceAsStream(PROPS_FILE_NAME)) {
            if (inputStream != null) {
                PROPERTIES.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + PROPS_FILE_NAME + "' not found in the classpath");
            }
        } catch (final Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }

}
