package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.usersresource.UserHelper;
import helpers.appsapi.usersresource.payloads.SearchUsersLinkedLocationsBody;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.*;

public class UserLinkUnlinkToLocationTest {

    private JSONObject organizationAndUsers;
    private UserFlows userFlows;
    private String organizationId;
    private SearchUsersLinkedLocationsBody searchUsersLinkedLocationsBody;
    private LocationFlows locationFlows;

    @BeforeTest
    public void setup() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        userFlows = new UserFlows();
        locationFlows = new LocationFlows();
        searchUsersLinkedLocationsBody = new SearchUsersLinkedLocationsBody();

    }

    @Xray(requirement = "PEG-2234", test = "PEG-3305")
    @Test(dataProvider = "organizationAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void linkUnlinkLocationToAdminOwnerUser(Role userRoleToUpdate) {

        final String userId = organizationAndUsers.getJSONObject(userRoleToUpdate.name()).getString("id");
        final String locationId = locationFlows.createLocation(organizationId).getString("id");

        UserHelper.linkToLocation(SUPPORT_TOKEN, organizationId, userId, locationId, LINK)
                .then()
                .statusCode(SC_OK);

        final JSONObject searchBody = searchUsersLinkedLocationsBody.bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.REQUIRED_FIELDS);

        UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.id", hasItems(locationId))
                .body("content.find { it.id == '" + locationId + "' }.linked", equalTo(true));

        UserHelper.linkToLocation(SUPPORT_TOKEN, organizationId, userId, locationId, UNLINK)
                .then()
                .statusCode(SC_OK);

        UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.id", hasItem(locationId))
                .body("content.find { it.id == '" + locationId + "' }.linked", equalTo(false))
                .body("content.find { it.id == '" + locationId + "' }.linked", equalTo(false));

    }

    @Xray(requirement = "PEG-2234", test = "PEG-3310")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void lastLocationUnlink(Role role) {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");

        UserHelper.linkToLocation(token, organizationId, userId, locationId, UNLINK)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(requirement = "PEG-2234", test = "PEG-2814")
    @Test
    public void linkWithStaffRole() {
        final ArrayList<String> locationsIds = new ArrayList<>();
        locationsIds.add(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
        final String staffToken = new AuthenticationFlowHelper().getUserTokenByRole(organizationId, Role.STAFF, locationsIds);
        final String newLocationId = new LocationFlows().createLocation(organizationId).getString("id");
        final String locationAdminId = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id");
        UserHelper.linkToLocation(staffToken, organizationId, locationAdminId, newLocationId, LINK)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-2234", test = "PEG-2813")
    @Test
    public void linkToLocationWhenThereIsNoPermissionInLocation() {

        final ArrayList<String> locationsIds = new ArrayList<>();
        locationsIds.add(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
        final String locationAdminToken = new AuthenticationFlowHelper().getUserTokenByRole(organizationId, Role.LOCATION_ADMIN, locationsIds);
        final String newLocationId = new LocationFlows().createLocation(organizationId).getString("id");
        final String locationAdminFromNewLocation = new UserFlows().createUser(organizationId, Role.LOCATION_ADMIN, List.of(newLocationId)).getString("id");

        UserHelper.linkToLocation(locationAdminToken, organizationId, locationAdminFromNewLocation, newLocationId, LINK)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

}