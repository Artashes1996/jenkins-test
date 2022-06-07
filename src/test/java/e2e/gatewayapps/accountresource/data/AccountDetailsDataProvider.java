package e2e.gatewayapps.accountresource.data;

import org.testng.annotations.DataProvider;

public class AccountDetailsDataProvider {

    @DataProvider(name = "invalidEmail", parallel = true)
    public static Object[][] invalidEmailDataProvider() {
        return new Object[][]{
                {null},
                {"test"},
                {"blahblah@sdsd"},
                {"blahblah.com"}
        };
    }

}
