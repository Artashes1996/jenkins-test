package e2e.gatewayapps.accountresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import helpers.DBHelper;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.flows.AccountFlows;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.testng.annotations.Test;
import utils.Xray;

import java.util.Collections;
import java.util.UUID;

import static configuration.Role.ADMIN;
import static configuration.Role.SUPPORT;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;

public class GetAccountRestoreTokenTest extends BaseTest {

    @SneakyThrows
    @Xray(test = "PEG-1546")
    @Test
    public void getAccountRestoreToken() {
        final String organizationId = new OrganizationFlows().createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final UserFlows userFlows = new UserFlows();
        for (Role role : Role.values()) {
            if (!role.equals(SUPPORT)) {
                final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
                final String userId = user.getString("id");
                userFlows.deleteUser(organizationId, userId);

                final AccountFlows accountFlows = new AccountFlows();
                accountFlows.restoreRequest(user.getString("email"), organizationId);
                Thread.sleep(20000);
                final Object restoreTokenDBHelper = DBHelper.getRestoreTokenById(userId);
                AccountHelper.getRestoreAccountByToken(restoreTokenDBHelper)
                        .then()
                        .statusCode(SC_OK)
                        .assertThat()
                        .body(matchesJsonSchemaInClasspath("schemas/getRestoreToken.json"));
            }
        }

    }

    @Xray(test = "PEG-1547")
    @Test
    public void getAccountRestoreTokenEmptyToken() {
        final Object invalidToken = null;

        AccountHelper.getRestoreAccountByToken(invalidToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(test = "PEG-1549")
    @Test
    public void getAccountRestoreTokenInvalidToken() {
        final Object invalidToken = UUID.randomUUID();

        AccountHelper.getRestoreAccountByToken(invalidToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    // TODO add XRay test case
    @SneakyThrows
    @Test
    public void getAccountRestoreTokenPausedOrganization() {

        final String organizationId = new OrganizationFlows().createPausedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final UserFlows userFlows = new UserFlows();

        final JSONObject user = userFlows.createUser(organizationId, ADMIN, Collections.singletonList(locationId));
        final String userId = user.getString("id");
        userFlows.deleteUser(organizationId, userId);

        final AccountFlows accountFlows = new AccountFlows();
        accountFlows.restoreRequest(user.getString("email"), organizationId);
        Thread.sleep(20000);


        final Object restoreTokenDBHelper = DBHelper.getRestoreTokenById(userId);

        AccountHelper.getRestoreAccountByToken(restoreTokenDBHelper)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getRestoreToken.json"));

    }


    // TODO add XRay test case
    @SneakyThrows
    @Test
    public void getAccountRestoreTokenBlockedOrganization() {
        final String organizationId = new OrganizationFlows().createBlockedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final UserFlows userFlows = new UserFlows();

        final JSONObject user = userFlows.createUser(organizationId, ADMIN, Collections.singletonList(locationId));
        final String userId = user.getString("id");
        userFlows.deleteUser(organizationId, userId);

        final AccountFlows accountFlows = new AccountFlows();
        accountFlows.restoreRequest(user.getString("email"), organizationId);
        Thread.sleep(20000);
        final Object restoreTokenDBHelper = DBHelper.getRestoreTokenById(userId);

        AccountHelper.getRestoreAccountByToken(restoreTokenDBHelper)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getRestoreToken.json"));
    }

    // TODO we need to move this into Delete Organization endpoint tests
    @SneakyThrows
    @Test
    public void getAccountRestoreTokenDeletedOrganization() {
        final String organizationId = new OrganizationFlows().createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final UserFlows userFlows = new UserFlows();

        final JSONObject user = userFlows.createUser(organizationId, ADMIN, Collections.singletonList(locationId));
        final String userId = user.getString("id");
        userFlows.deleteUser(organizationId, userId);

        final AccountFlows accountFlows = new AccountFlows();
        accountFlows.restoreRequest(user.getString("email"), organizationId);
        Thread.sleep(20000);
        final Object restoreTokenDBHelper = DBHelper.getRestoreTokenById(userId);
        new OrganizationFlows().deleteOrganization(organizationId);
        AccountHelper.getRestoreAccountByToken(restoreTokenDBHelper)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
