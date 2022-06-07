package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.UserDataProvider;
import helpers.appsapi.support.invitationresource.flows.SupportFlows;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;

import java.util.*;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class UserAvatarUploadTest extends BaseTest {

    private UserFlows userFlows;
    private JSONObject organizationWithAllUsers;
    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private String locationId;

    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        locationFlows = new LocationFlows();
        organizationWithAllUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        locationId = organizationWithAllUsers.getJSONObject("LOCATION").getString("id");
        final String supportToken = getToken(SUPPORT);
        final String email = new Random().nextInt() + "@qless.com";
        final String password = "Qw!123456";
        new SupportFlows().createSupport(supportToken, email, password);
    }

    @Test(testName = "PEG-1433, PEG-1429", dataProviderClass = UserDataProvider.class, dataProvider = "valid avatar paths")
    public void uploadAvatarAsOwner(String filePath) {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationWithAllUsers.getJSONObject(randomRole.name()).getString("token");
        UserHelper.uploadUserAvatar(token, filePath)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/avatarUpload.json"));
    }

    @Test(testName = "PEG-1435")
    public void uploadAvatarWithWrongContentType() {
        final String filePath = "src/test/resources/files/pics/charmander.png";
        final String incorrectContentType = "text/html";
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationWithAllUsers.getJSONObject(randomRole.name()).getString("token");
        UserHelper.uploadUserAvatar(token, filePath, incorrectContentType)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-1442", dataProviderClass = UserDataProvider.class, dataProvider = "unsupported files")
    public void uploadAvatarUnsupportedFile(String filePath) {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationWithAllUsers.getJSONObject(randomRole.name()).getString("token");
        UserHelper.uploadUserAvatar(token, filePath)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-1443")
    public void uploadAvatarUnsupportedFileWithCorrespondingType() {
        final String unsupportedFilePath = "src/test/resources/files/Captain Marvel.html";
        final String contentType = "text/html";

        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationWithAllUsers.getJSONObject(randomRole.name()).getString("token");
        UserHelper.uploadUserAvatar(token, unsupportedFilePath, contentType)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-1444")
    public void uploadAvatarUnsupportedSize() {
        final String unsupportedSizeFilePath = "src/test/resources/files/pics/bubble.jpg";
        final String contentType = "text/html";
        final Role randomRole = getRandomRole();
        final String token =
                organizationWithAllUsers.getJSONObject(randomRole.name()).getString("token");
        UserHelper.uploadUserAvatar(token, unsupportedSizeFilePath, contentType)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-1448, PEG-1449")
    public void uploadAvatarInactiveUser() {
        final String filePath = "src/test/resources/files/pics/atom-solid.svg";
        final JSONObject newOrganizationWithOwner = organizationFlows.createUnpublishedOrganizationWithOwner();
        final String newOrganizationId = newOrganizationWithOwner.getJSONObject("ORGANIZATION").getString("id");
        final String newLocationId = locationFlows.createLocation(newOrganizationId).getString("id");
        userFlows.createUser(newOrganizationId, OWNER, Collections.singletonList(newLocationId));
        final JSONObject randomUser = userFlows.createUser(newOrganizationId, getRandomOrganizationRole(),
                Collections.singletonList(newLocationId));
        userFlows.inactivateUserById(newOrganizationId, randomUser.getString("id"));
        UserHelper.uploadUserAvatar(randomUser.getString("token"), filePath)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Test(testName = "PEG-1446, PEG-1447")
    public void uploadAvatarDeletedUser() {
        final String filePath = "src/test/resources/files/pics/atom-solid.svg";

        userFlows.createUser(organizationWithAllUsers.getJSONObject("ORGANIZATION").getString("id"), OWNER, null);
        final JSONObject user = userFlows.createUser(organizationWithAllUsers.getJSONObject("ORGANIZATION").getString("id"),
                getRandomOrganizationRole(), Collections.singletonList(locationId));
        final String userId = user.getString("id");
        final String token = user.getString("token");

        userFlows.deleteUser(organizationWithAllUsers.getJSONObject("ORGANIZATION").getString("id"), userId);
        UserHelper.uploadUserAvatar(token, filePath)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Test
    public void uploadAvatarDeletedOrganization() {
        final String filePath = "src/test/resources/files/pics/atom-solid.svg";
        final JSONObject deletedOrgWithOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String deletedOrganizationId = deletedOrgWithOwner.getJSONObject("ORGANIZATION").getString("id");
        final String deletedOrganizationLocationId = locationFlows.createLocation(deletedOrganizationId).getString("id");
        final JSONObject user = userFlows.createUser(deletedOrganizationId, getRandomOrganizationRole(),
                Collections.singletonList(deletedOrganizationLocationId));
        final String token = user.getString("token");

        organizationFlows.deleteOrganization(deletedOrganizationId);
        UserHelper.uploadUserAvatar(token, filePath)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Test
    public void uploadAvatarBlockedOrganization() {
        final String filePath = "src/test/resources/files/pics/atom-solid.svg";
        final JSONObject blockedOrganizationAndUsers = organizationFlows.createAndPublishOrganizationWithOwner();
        final String blockedOrganizationId = blockedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String blockedOrganizationLocationId = locationFlows.createLocation(blockedOrganizationId).getString("id");
        final JSONObject user = userFlows.createUser(blockedOrganizationId, getRandomOrganizationRole(),
                Collections.singletonList(blockedOrganizationLocationId));
        final String token = user.getString("token");
        organizationFlows.blockOrganization(blockedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id"));
        UserHelper.uploadUserAvatar(token, filePath)
                .then()
                .statusCode(SC_OK);
    }

    @Test(testName = "PEG-1481")
    public void uploadAvatarWithInvalidToken() {
        final String filePath = "src/test/resources/files/pics/atom-solid.svg";
        final String invalidToken = UUID.randomUUID().toString();

        UserHelper.uploadUserAvatar(invalidToken, filePath)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }
}
