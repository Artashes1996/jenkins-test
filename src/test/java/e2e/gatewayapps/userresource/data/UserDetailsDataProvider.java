package e2e.gatewayapps.userresource.data;

import org.json.JSONObject;
import org.testng.annotations.DataProvider;

import java.util.Random;
import java.util.UUID;

public class UserDetailsDataProvider {
    final static Random random = new Random();

    @DataProvider(name = "organizationId")
    public static Object[][] organizationIdDataProvider() {
        return new Object[][]{
                {random.nextInt(1000000)},
                {false},
                {""},
                {UUID.randomUUID().toString()}
        };
    }

    @DataProvider(name = "invalidName")
    public static Object[][] invalidNameDataProvider() {
        return new Object[][]{
                {""},
                {JSONObject.NULL},
                {" "}
        };
    }

    @DataProvider(name = "validAccountStatus")
    public static Object[][] validAccountStatusDataProvider() {
        return new Object[][]{
                {"INACTIVE"},
                {"ACTIVE"}
        };
    }

    @DataProvider(name = "invalidAccountStatus")
    public static Object[][] invalidAccountStatusDataProvider() {
        return new Object[][]{
                {10},
                {"DEACTIVATE"},
                {"DEACTIVATED"}
        };
    }

    @DataProvider(name = "invalid Account Statuses")
    public static Object[][] invalidAccountStatusesDataProvider() {
        return new Object[][]{
                {"ACTIVE", 10},
                {"DEACTIVATE", true},
                {"ACTIVE", "DEACTIVATED"}
        };
    }

    @DataProvider(name = "validName")
    public static Object[][] validNameDataProvider() {
        return new Object[][]{
                {"QA"},
                {"QA`testing"},
                {"QA Wan Der Ween"}
        };
    }

    @DataProvider(name = "validPassword")
    public static Object[][] validPasswordDataProvider() {
        return new Object[][]{
                {"Qw123456!"},
                {"1Wsssss&"}
        };
    }

    @DataProvider(name = "invalidPassword")
    public static Object[][] invalidPasswordDataProvider() {
        return new Object[][]{
                {"Qw123456"},
                {"1Wssssss"},

        };
    }

    @DataProvider(name = "invalidId")
    public static Object[][] invalidIdDataProvider() {
        return new Object[][]{
                {""},
                {JSONObject.NULL},
                {"test"},
                {true}
        };
    }

    @DataProvider(name = "invalidAccountId")
    public static Object[][] invalidAccountIdDataProvider() {
        return new Object[][]{
                {JSONObject.NULL},
                {"test"},
                {true}
        };
    }

    @DataProvider(name = "validInvitationStatus")
    public static Object[][] validInvitationStatusDataProvider() {
        return new Object[][]{
                {"PENDING"},
                {"ACCEPTED"},
                {"EXPIRED"}
        };
    }

    @DataProvider(name = "invalidInvitationStatus")
    public static Object[][] invalidInvitationStatusDataProvider() {
        return new Object[][]{
                {10},
                {"EXPIRE"},
                {"NEW"}
        };
    }

    @DataProvider(name = "invitation2StatusMix")
    public static Object[][] invitation2StatusMixDataProvider() {
        return new Object[][]{
                {"ACCEPTED","PENDING"},
                {"ACCEPTED","EXPIRED"},
                {"PENDING","EXPIRED"},
                {"PENDING","ACCEPTED"},
                {"EXPIRED","ACCEPTED"},
                {"EXPIRED", "PENDING"}
        };
    }

    @DataProvider(name = "invitation3StatusMix")
    public static Object[][] invitation3StatusMixDataProvider() {
        return new Object[][]{
                {"ACCEPTED","PENDING","EXPIRED"},
                {"ACCEPTED","EXPIRED","PENDING"},
                {"PENDING","EXPIRED","ACCEPTED"},
                {"PENDING","ACCEPTED","EXPIRED"},
                {"EXPIRED","ACCEPTED","PENDING"},
                {"EXPIRED","PENDING","ACCEPTED"}
        };
    }

    @DataProvider(name = "invalidInvitation2StatusMix")
    public static Object[][] invalid2StatusMixDataProvider() {
        return new Object[][]{
                {"EXPIRED","Boom"},
                {"PENDING",21}
        };
    }
}
