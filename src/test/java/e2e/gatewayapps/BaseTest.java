package e2e.gatewayapps;

import configuration.Role;
import helpers.flows.AuthenticationFlowHelper;

public class BaseTest {

    public static String getToken(Role byRole) {

        return new AuthenticationFlowHelper().getToken(byRole);
    }

}
