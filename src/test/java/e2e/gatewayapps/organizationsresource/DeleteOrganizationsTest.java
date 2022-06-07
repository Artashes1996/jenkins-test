package e2e.gatewayapps.organizationsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.organizationsresource.data.OrganizationsDataProvider;
import helpers.appsapi.support.organizationsresource.OrganizationsHelper;
import helpers.appsapi.support.organizationsresource.payloads.DeleteRestoreOrganizationRequestBody;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;

import static configuration.Role.*;
import static helpers.appsapi.support.organizationsresource.payloads.DeleteRestoreOrganizationRequestBody.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class DeleteOrganizationsTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private ThreadLocal<JSONObject> organizationWithUsers;
    private ThreadLocal<String> organizationId;


    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        organizationWithUsers = new ThreadLocal<>();
        organizationId = new ThreadLocal<>();
    }

    @BeforeMethod(onlyForGroups = {"Precondition: Deleted organization and users"})
    public void dataPreparation() {
        organizationWithUsers.set(organizationFlows.createAndDeletePublishedOrganization());
        organizationId.set(organizationWithUsers.get().getJSONObject("ORGANIZATION").getString("id"));
    }

    @BeforeMethod(onlyForGroups = {"Precondition: Organization And Users created"})
    public void init() {
        organizationWithUsers.set(organizationFlows.createAndPublishOrganizationWithAllUsers());
        organizationId.set(organizationWithUsers.get().getJSONObject("ORGANIZATION").getString("id"));
    }

    @Xray(test = "PEG-7292")
    @Test(groups = {"Precondition: Organization And Users created"})
    public void deleteOrganizationWithNonSupportedUser() {

        final String ownerToken = organizationWithUsers.get().getJSONObject(OWNER.name()).getString("token");
        final String adminToken = organizationWithUsers.get().getJSONObject(ADMIN.name()).getString("token");
        final String locationAdminToken = organizationWithUsers.get().getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String staffToken = organizationWithUsers.get().getJSONObject(STAFF.name()).getString("token");

        final JSONObject deleteOrganizationBody = new JSONObject();
        final String comment = "Valid reason";
        deleteOrganizationBody.put(COMMENT, comment);
        deleteOrganizationBody.put(REASONS, Collections.singletonList(DeleteReasons.OTHER.name()));
        organizationFlows.deleteOrganization(organizationId.get());

        OrganizationsHelper.deleteOrganization(staffToken, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        OrganizationsHelper.deleteOrganization(locationAdminToken, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        OrganizationsHelper.deleteOrganization(adminToken, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        OrganizationsHelper.deleteOrganization(ownerToken, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-929")
    @Test(groups = {"Precondition: Organization And Users created"})
    public void deleteOrganizationWithoutReason() {
        final JSONObject deleteOrganizationBody = new JSONObject();
        deleteOrganizationBody.put(COMMENT, "");
        deleteOrganizationBody.put(REASONS, Collections.singletonList(""));

        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray(test = "PEG-917, PEG-919, PEG-920")
    @Test(dataProvider = "delete reasons", dataProviderClass = OrganizationsDataProvider.class)
    public void deleteOrganization(String validReason) {

        final String organizationId = organizationFlows.createUnpublishedOrganization().getString("id");
        final JSONObject deleteOrganizationBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreCombination.DELETE);
        deleteOrganizationBody.put(REASONS, Collections.singletonList(validReason));
        if (validReason.equals(DeleteReasons.OTHER.name())) deleteOrganizationBody.put(COMMENT, "valid comment");

        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId, deleteOrganizationBody)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Xray(test = "PEG-920")
    @Test
    public void deleteOrganizationUsingOtherReason() {
        final String organizationId = organizationFlows.createUnpublishedOrganization().getString("id");
        final JSONObject deleteOrganizationBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreCombination.DELETE);
        deleteOrganizationBody.put(COMMENT, "valid comment");
        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId, deleteOrganizationBody)
                .then()
                .statusCode(SC_NO_CONTENT);
    }


    @Xray(test = "PEG-918")
    @Test(groups = {"Precondition: Deleted organization and users"})
    public void deleteDeletedOrganization() {

        final JSONObject deleteOrganizationBody = new JSONObject();
        final String comment = "Valid reason";
        deleteOrganizationBody.put(COMMENT, comment);
        deleteOrganizationBody.put(REASONS, Collections.singletonList(DeleteReasons.OTHER.name()));

        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-921")
    @Test(groups = {"Precondition: Organization And Users created"})
    public void deleteOrganizationOtherReasonNoComment() {
        final JSONObject deleteOrganizationBody = new JSONObject();
        final String blankComment = " ";
        deleteOrganizationBody.put(COMMENT, blankComment);
        deleteOrganizationBody.put(REASONS, Collections.singletonList(DeleteReasons.OTHER.name()));

        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-922")
    @Test
    public void deleteNonExistingOrganization() {
        final String nonExistingOrganizationId = getRandomInt() + "org";
        final JSONObject deleteOrganizationBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreCombination.DELETE);

        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, nonExistingOrganizationId, deleteOrganizationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-921")
    @Test(groups = {"Precondition: Organization And Users created"})
    public void deleteOrganizationEmptyReason() {
        final JSONObject deleteOrganizationBody = new JSONObject();
        final String comment = "Invalid reason";
        deleteOrganizationBody.put(COMMENT, comment);
        deleteOrganizationBody.put(REASONS, Collections.singletonList(null));

        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-923")
    @Test(dataProvider = "invalid reasons", dataProviderClass = OrganizationsDataProvider.class, groups = {"Precondition: Organization And Users created"})
    public void deleteOrganizationNonExistingReason(Object reason) {
        final JSONObject deleteOrganizationBody = new JSONObject();
        final String comment = "Invalid reason";
        deleteOrganizationBody.put(COMMENT, comment);
        deleteOrganizationBody.put(REASONS, Collections.singletonList(reason));

        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));

    }

    @Xray(test = "PEG-927")
    @Test(groups = {"Precondition: Organization And Users created"})
    public void deleteOrganizationWithAllValidReasonsNoComment() {
        final List<String> reasons = Arrays.asList(DeleteReasons.LACK_OF_PAYMENT.name(), DeleteReasons.CONTRACT_CANCELLATION.name(), DeleteReasons.OTHER.name());
        final JSONObject deleteOrganizationBody = new JSONObject();
        final String comment = " ";
        deleteOrganizationBody.put(COMMENT, comment);
        deleteOrganizationBody.put(REASONS, reasons);

        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-926")
    @Test(groups = {"Precondition: Organization And Users created"})
    public void deleteOrganizationWithAllReasons() {
        final List<String> reasons = Arrays.asList(DeleteReasons.LACK_OF_PAYMENT.name(), DeleteReasons.CONTRACT_CANCELLATION.name(), DeleteReasons.OTHER.name());
        final JSONObject deleteOrganizationBody = new JSONObject();
        final String comment = "Valid Comment";
        deleteOrganizationBody.put(COMMENT, comment);
        deleteOrganizationBody.put(REASONS, reasons);

        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Xray(test = "PEG-928")
    @Test(groups = {"Precondition: Organization And Users created"})
    public void deleteOrganizationWithReasonsIncludingInvalid() {
        final List<String> reasons = Arrays.asList(DeleteReasons.LACK_OF_PAYMENT.name(), DeleteReasons.CONTRACT_CANCELLATION.name(), DeleteReasons.OTHER.name(), "INVALID_REASON");
        final JSONObject deleteOrganizationBody = new JSONObject();
        final String comment = "Valid Comment";
        deleteOrganizationBody.put(COMMENT, comment);
        deleteOrganizationBody.put(REASONS, reasons);

        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));

    }

    @Xray(test = "PEG-944")
    @Test(groups = {"Precondition: Organization And Users created"})
    public void deletePausedOrganization() {
        organizationFlows.pauseOrganization(organizationId.get());
        final JSONObject deleteOrganizationBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreCombination.DELETE);
        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Xray(test = "PEG-925")
    @Test(groups = {"Precondition: Organization And Users created"})
    public void deleteBlockedOrganization() {
        organizationFlows.blockOrganization(organizationId.get());
        final JSONObject deleteOrganizationBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreCombination.DELETE);
        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId.get(), deleteOrganizationBody)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Xray(requirement = "PEG-4707", test = "PEG-5252")
    @Test(groups = {"Precondition: Deleted organization and users"})
    public void oldTokenIsInvalidInCaseOfDeletedOrganization() {

        final String userToken = organizationWithUsers.get().getJSONObject(Role.getRandomOrganizationRole().name()).getString("token");

        UserHelper.searchForUsers(userToken, organizationId, new JSONObject())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }
}
