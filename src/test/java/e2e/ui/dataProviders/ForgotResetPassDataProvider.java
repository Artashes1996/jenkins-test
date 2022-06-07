package e2e.ui.dataProviders;

import org.testng.annotations.DataProvider;
import pages.ResetPasswordPage;

public class ForgotResetPassDataProvider {

    @DataProvider(name = "invalid emails")
    public static Object[][] invalidEmails() {

        return new Object[][]{
                {"test"},
                {"test@"},
                {"@a.com"},
                {"test@acom"}
        };
    }

    @DataProvider(name = "missing fields")
    public static Object[][] missingFields() {
        return new Object[][]{
                {ResetPasswordPage.FieldName.PASSWORD, "", "Qw123456!"},
                {ResetPasswordPage.FieldName.REPEAT_PASSWORD, "Qw123456!", ""},

        };
    }

    @DataProvider(name = "invalid passwords")
    public static Object[][] invalidPasswords() {

        return new Object[][]{
                {"Qw123456"},
                {"qw123456!"},
                {"Qwwwwwww!"},
                {"Qw123!"},
                {"Qw123456"}
        };
    }

}
