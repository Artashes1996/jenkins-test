package e2e.gatewayapps.accountresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.appsapi.accountresource.payloads.ForceResetPasswordRequestBody;
import helpers.appsapi.accountresource.payloads.ResetPasswordApplyBody;
import helpers.appsapi.accountresource.payloads.ResetPasswordRequestBody;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.Xray;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static configuration.Role.OWNER;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.equalTo;

public class GetResetPasswordByTokenTest extends BaseTest {

    private String organizationId;
    private String locationId;
    private UserFlows userFlows;
    private OrganizationFlows organizationFlows;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        organizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        locationId = new LocationFlows().createLocation(organizationId).getString("id");
    }

    @Xray(test = "PEG-1487, PEG-1488, PEG-1491")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void getResetToken(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        final JSONObject resetRequest = new JSONObject();
        resetRequest.put(ResetPasswordRequestBody.EMAIL, user.getString("email"));
        AccountHelper.resetPasswordRequest(resetRequest).then().statusCode(SC_OK);
        final String resetToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(user.getString("id"))).toString();

        AccountHelper.getResetPasswordByToken(resetToken)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getResetToken.json"));

        final String validPassword = "We@234567";
        final JSONObject applyResetPasswordBody = new JSONObject();
        applyResetPasswordBody.put(ResetPasswordApplyBody.PASSWORD, validPassword);
        applyResetPasswordBody.put(ResetPasswordApplyBody.RESET_TOKEN, resetToken);
        AccountHelper.applyResetPassword(applyResetPasswordBody)
                .then()
                .statusCode(SC_OK);
        final String usedTokenMessage = "Token already used";
        AccountHelper.getResetPasswordByToken(resetToken)
                .then()
                .statusCode(SC_GONE)
                .body("messages[0]", equalTo(usedTokenMessage));
    }

    @Xray(test = "PEG-1491")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void getExpiredToken(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        final JSONObject resetRequest = new JSONObject();
        resetRequest.put(ResetPasswordRequestBody.EMAIL, user.getString("email"));
        AccountHelper.resetPasswordRequest(resetRequest).then().statusCode(SC_OK);
        final String resetToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(user.getString("id"))).toString();

        DBHelper.expireResetPasswordToken(user.getString("id"));
        final String expiredTokenMessage = "Token expired";
        AccountHelper.getResetPasswordByToken(resetToken)
                .then()
                .statusCode(SC_GONE)
                .body("messages[0]", equalTo(expiredTokenMessage));
    }

    @Xray(test = "PEG-1485")
    @Test
    public void getNonExistingResetToken() {
        final String resetToken = UUID.randomUUID().toString();

        AccountHelper.getResetPasswordByToken(resetToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(test = "PEG-1501")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void getForceResetPasswordToken(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        final String accountId = DBHelper.getAccountIdByEmail(user.getString("email"));

        final JSONObject forceResetBody = new JSONObject();
        forceResetBody.put(ForceResetPasswordRequestBody.ID, accountId);
        AccountHelper.forceResetPasswordRequest(getToken(Role.SUPPORT), organizationId, forceResetBody)
                .then()
                .statusCode(SC_OK);

        final String resetToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(user.getString("id"))).toString();
        AccountHelper.getResetPasswordByToken(resetToken)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getResetToken.json"));
    }

    // TODO XRay is missing
    @Test
    public void getResetPasswordByTokenDeletedOrganization() {
        final JSONObject organizationAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String organizationId = organizationAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = organizationAndOwner.getJSONObject(OWNER.name());

        final JSONObject resetRequest = new JSONObject();
        resetRequest.put(ResetPasswordRequestBody.EMAIL, owner.getString("email"));
        AccountHelper.resetPasswordRequest(resetRequest).then().statusCode(SC_OK);
        final String resetToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(owner.getString("id"))).toString();
        organizationFlows.deleteOrganization(organizationId);

        AccountHelper.getResetPasswordByToken(resetToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    // TODO XRay is missing
    @Test
    public void getResetPasswordByTokenPausedOrganization() {
        final JSONObject organizationAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String organizationId = organizationAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = organizationAndOwner.getJSONObject(OWNER.name());

        final JSONObject resetRequest = new JSONObject();
        resetRequest.put(ResetPasswordRequestBody.EMAIL, owner.getString("email"));
        AccountHelper.resetPasswordRequest(resetRequest).then().statusCode(SC_OK);
        final String resetToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(owner.getString("id"))).toString();
        organizationFlows.pauseOrganization(organizationId);

        AccountHelper.getResetPasswordByToken(resetToken)
                .then()
                .statusCode(SC_OK);
    }

    // TODO XRay is missing
    @Test
    public void getResetPasswordByTokenBlockedOrganization() {
        final JSONObject organizationAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String organizationId = organizationAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = organizationAndOwner.getJSONObject(OWNER.name());

        final JSONObject resetRequest = new JSONObject();
        resetRequest.put(ResetPasswordRequestBody.EMAIL, owner.getString("email"));
        AccountHelper.resetPasswordRequest(resetRequest).then().statusCode(SC_OK);
        final String resetToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(owner.getString("id"))).toString();
        organizationFlows.blockOrganization(organizationId);

        AccountHelper.getResetPasswordByToken(resetToken)
                .then()
                .statusCode(SC_OK);
    }
}
