package e2e.gatewayapps.accountresource.data;

import org.testng.annotations.DataProvider;

public class PasswordDataProvider {

    @DataProvider(name = "invalidPasswords", parallel = true)
    public static Object[][] invalidPasswords() {
        return new Object[][]{
                {null},
                {"!123456"},
                {"Qnlajrr123"},
                {"blahblah@sdsd111"},
                {"bbb bbb bbb"}
        };
    }
}
