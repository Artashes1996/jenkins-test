package e2e.ui.dataProviders;

import configuration.Role;
import org.testng.annotations.DataProvider;

public class LoginDataProvider {

    @DataProvider(name = "valid users")
    public static Object[][] validUsers() {

        return new Object[][]{
                {Role.OWNER},
          //      {Role.EMPLOYEE},
                {Role.SUPPORT}
        };
    }

    @DataProvider(name = "invalid username/password")
    public static Object[][] invalidCredentials() {
        return new Object[][]{
                {"test", "test"},
                {"test@qa.com", "invalid"},
                {"test", "Qw123456!"}
        };
    }
}

