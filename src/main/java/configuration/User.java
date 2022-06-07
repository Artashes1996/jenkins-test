package configuration;

import lombok.Data;
import lombok.Getter;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

@Data
@Getter
public class User {

    private String email;
    private String password;

    private static final String ENV = System.getProperty("env", "dev");

    private static final String PROPS_FILE_NAME = "configs/users_" + ENV + ".properties";

    public static final Properties PROPERTIES = new Properties();

    public User(Role role) {
        try (final InputStream inputStream = Config.class.getClassLoader().getResourceAsStream(PROPS_FILE_NAME)) {
            if (inputStream != null) {
                PROPERTIES.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + PROPS_FILE_NAME + "' not found in the classpath");
            }

            email = PROPERTIES.getProperty(role.toString() + ".email");
            password = PROPERTIES.getProperty(role.toString() + ".password");
        } catch (final Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }
}