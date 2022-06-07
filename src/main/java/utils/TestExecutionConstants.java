package utils;

import configuration.Role;
import helpers.appsapi.accountresource.LoginHelper;
import helpers.flows.AuthenticationFlowHelper;

public class TestExecutionConstants {

    public static final String SUPPORT_TOKEN;

    static {
        SUPPORT_TOKEN = LoginHelper.login(Role.SUPPORT).then().extract().path("token");
    }

}
