package e2e.gatewayapps.invitationresource.data;

import org.json.JSONObject;
import org.testng.annotations.DataProvider;

public class InvitationDataProvider {

    @DataProvider(name = "invalidEmail")
    protected static Object[][] emailInvalidData() {
        return new Object[][]{
                {"invalid"},
                {JSONObject.NULL},
                {"invalid.1.test"},
                {"invalid@test"}
        };
    }

    @DataProvider(name = "invalidId")
    protected static Object[][] roleInvalidData() {
        return new Object[][]{
                {"id"},
                {null},
                {"[--1]"}
        };
    }

    @DataProvider(name = "notExistingRoleId")
    protected static Object[][] notExistingRoleData() {
        return new Object[][]{
                {3000000},
                {0}
        };
    }

    @DataProvider(name = "notExistingGroupId")
    protected static Object[][] notExistingGroupData() {
        return new Object[][]{
                {5000000},
                {0}
        };
    }

    @DataProvider(name = "validPhoneNumber")
    protected static Object[][] validPhoneNumberData() {
        return new Object[][]{
                {"+1 302 5551 098"},
                {"+1-410-5557-963"},
                {"+16179821732"},
                {"+44 70 7593 3750"},
                {"+1-202-555-0132"},
                {"+1-613-555-0142"},
                {"+61 1900 654 321"}
        };
    }

    @DataProvider(name = "invalidPhoneNumber", parallel = true)
    protected static Object[][] invalidPhoneNumberData() {
        return new Object[][]{
                {"-+()1234567"},
                {"+37889900"},
                {"null"},
                {"  "},
                {"077489494"}
        };
    }

    @DataProvider(name = "invalidPayload")
    protected static Object[][] invalidPayloadData() {
        return new Object[][]{
                {"{ a: null }"},
                {"{ a: [ null ] }"},
                {1},
                {null},
                {"test"},
                {false}
        };
    }

    @DataProvider(name = "invalidToken")
    protected static Object[][] invalidTokenData() {
        return new Object[][]{
                {"a"},
                {1},
                {"111-2222-3333zzz-xxx"},
                {false}
        };
    }

    @DataProvider(name = "invalidPassword", parallel = true)
    protected static Object[][] invalidPasswordDataProvider() {
        return new Object[][]{
                {"Qw123456"},
                {"qw@123456"},
                {"Qw&1234"},
                {"!@1234567$"},
                {null}
        };
    }

    @DataProvider(name = "invalid Email for Support")
    protected static Object[][] invalidEmailForSupport() {
        return new Object[][]{
                {null},
                {"invalid.1.test"},
                {"invalid@test"},
                {"invalid@gmail.com"},
                {"invalid@qless.co"}
        };
    }
}
