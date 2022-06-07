package helpers.flows;

import configuration.Role;
import helpers.appsapi.accountresource.LoginHelper;

import java.util.List;

public class AuthenticationFlowHelper {

    public String getToken(Role userRole) {
        return LoginHelper.login(userRole).then().extract().path("token");
    }

    public String getTokenWithEmailPassword(String email, String password) {
        return LoginHelper.login(email, password).then().extract().path("token");
    }

    public String getTokenWithEmail(String email) {
        return getTokenWithEmailPassword(email, "Qw!123456");
    }

    public String getUserTokenByRole(String organizationId, Role role, List<String> locationIds) {
        return new UserFlows().createUserAndGetFullResponse(organizationId, role, locationIds).getString("token");
    }
}
