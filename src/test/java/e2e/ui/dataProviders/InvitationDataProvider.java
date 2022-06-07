package e2e.ui.dataProviders;

import org.testng.annotations.DataProvider;
import pages.InvitationPage;

public class InvitationDataProvider {

    @DataProvider(name = "missing invitation accept fields")
    public static Object[][] missingFields() {

        return new Object[][]{
                {InvitationPage.FieldName.FIRST_NAME,"", "test", "Qw123456!", "Qw123456!"},
                {InvitationPage.FieldName.LAST_NAME, "test", "", "Qw123456!", "Qw123456!"},
                {InvitationPage.FieldName.PASSWORD,"test", "test", "","Qw123456!"},
                {InvitationPage.FieldName.REPEAT_PASSWORD,"test", "test", "Qw123456!",""}
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
