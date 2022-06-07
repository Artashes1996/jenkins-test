package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.*;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.*;

public class UserLinkUnlinkLocationServicesTest extends BaseTest {

    private String orgId;
    private String linkedLocationId;
    private JSONArray locationServiceIds;
    private String locationServiceIdFromGroup;
    private JSONObject user;


    @BeforeClass
    public void setup(){
        final JSONObject orgWithUserLinkedUnlinkedLocationServices = new OrganizationFlows().orgWithUserLinkedUnlinkedLocationServicesInGroup(Role.getRandomOrganizationAdminRole());
        final JSONObject linkedLocation = orgWithUserLinkedUnlinkedLocationServices.getJSONObject("USER").getJSONObject("LINKED_LOCATION");
        orgId = orgWithUserLinkedUnlinkedLocationServices.getJSONObject("ORGANIZATION").getString("id");
        linkedLocationId = linkedLocation.getString("id");
        locationServiceIds = linkedLocation.getJSONArray("locationServiceIds");
        locationServiceIdFromGroup = linkedLocation.getJSONObject("SERVICE_GROUP").getString("serviceId");
        user = orgWithUserLinkedUnlinkedLocationServices.getJSONObject("USER");
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4913")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void linkUnlinkUserToLocationService(Role role){

        final JSONObject orgWithUserLinkedUnlinkedLocationServices = new OrganizationFlows().orgWithUserLinkedUnlinkedLocationServices(role);
        final JSONObject linkedLocation = orgWithUserLinkedUnlinkedLocationServices.getJSONObject("USER").getJSONObject("LINKED_LOCATION");
        final String orgId = orgWithUserLinkedUnlinkedLocationServices.getJSONObject("ORGANIZATION").getString("id");
        final String linkedLocationId = linkedLocation.getString("id");
        final JSONArray locationServiceIds = linkedLocation.getJSONArray("locationServiceIds");
        final JSONObject user = orgWithUserLinkedUnlinkedLocationServices.getJSONObject("USER");

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), linkedLocationId, locationServiceIds.getString(1), LINK)
                .then()
                .statusCode(SC_OK);

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), linkedLocationId, locationServiceIds.getString(1), UNLINK)
                .then()
                .statusCode(SC_OK);

    }

    @Xray(requirement = "PEG-2380", test = "PEG-4911")
    @Test
    public void linkUnlinkUserToLocationServiceInGroupByStaff(){

        final JSONObject staffUser = new UserFlows().createUser(orgId,Role.STAFF, Collections.singletonList(linkedLocationId));

        UserHelper.linkUnlinkUserToLocationService(staffUser.getString("token"), orgId, staffUser.getString("id"), linkedLocationId, locationServiceIdFromGroup, LINK)
                .then()
                .statusCode(SC_FORBIDDEN)
 .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-5313")
    @Test
    public void linkUnlinkUserToLocationServiceInGroup(){

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), linkedLocationId, locationServiceIdFromGroup, LINK)
                .then()
                .statusCode(SC_OK);

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), linkedLocationId, locationServiceIdFromGroup, UNLINK)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4907")
    @Test
    public void linkUnlinkUserToLocationServiceWithNoAction(){

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), linkedLocationId, locationServiceIds.getString(2), "" )
                .then()
                .statusCode(SC_BAD_REQUEST)
 .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4908")
    @Test
    public void linkUnlinkUserToLocationServiceWithNonExistingAction(){

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), linkedLocationId, locationServiceIds.getString(2), "BLOCKED" )
                .then()
                .statusCode(SC_BAD_REQUEST)
 .body("type", is("REQUEST_DATA_TYPE_MISMATCH"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4909")
    @Test
    public void linkAlreadyLinkedUserToLocationService(){

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), linkedLocationId, locationServiceIds.getString(3), LINK.name() )
                .then()
                .statusCode(SC_OK);

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), linkedLocationId, locationServiceIds.getString(3), LINK.name() )
                .then()
                .statusCode(SC_CONFLICT)
 .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4910")
    @Test
    public void unlinkNotLinkedUserToLocationService(){

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), linkedLocationId, locationServiceIds.getString(0), UNLINK.name())
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4898")
    @Test
    public void linkUserToLocationServiceWithFakeOrgIdBySupport(){

        UserHelper.linkUnlinkUserToLocationService(SUPPORT_TOKEN, UUID.randomUUID(), user.getString("id"), linkedLocationId, locationServiceIds.getString(0), UNLINK.name())
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4899")
    @Test
    public void linkUserToLocationServiceWithFakeOrgIdByUser(){

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), UUID.randomUUID(), user.getString("id"), linkedLocationId, locationServiceIds.getString(0), UNLINK.name())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4900")
    @Test
    public void linkUserToLocationServiceWithoutLocation(){

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), null, locationServiceIds.getString(0), UNLINK.name())
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4901")
    @Test
    public void linkUserToLocationServiceWithFakeLocation(){

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), UUID.randomUUID(), locationServiceIds.getString(0), UNLINK.name())
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4902")
    @Test
    public void linkUserToLocationServiceWithoutServiceLocation(){

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), linkedLocationId,null, UNLINK.name())
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4906")
    @Test
    public void linkUserToLocationServiceWithFakeServiceLocation(){

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, user.getString("id"), linkedLocationId,UUID.randomUUID(), UNLINK.name())
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4905")
    @Test
    public void linkUserToLocationServiceWithFakeUserId(){

        UserHelper.linkUnlinkUserToLocationService(user.getString("token"), orgId, UUID.randomUUID(), linkedLocationId, locationServiceIds.getString(0), UNLINK.name())
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4903")
    @Test
    public void linkUserToLocationServiceToOtherOrgUser(){

        UserHelper.linkUnlinkUserToLocationService(new AuthenticationFlowHelper().getToken(Role.getRandomOrganizationRole()), orgId, UUID.randomUUID(), linkedLocationId, locationServiceIds.getString(0), UNLINK.name())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(requirement = "PEG-2380", test = "PEG-4904")
    @Test
    public void linkUnlinkUserToLocationServiceInDeletedOrg(){

        final OrganizationFlows organizationFlows = new OrganizationFlows();
        final JSONObject orgWithUserLinkedUnlinkedLocationServices = organizationFlows.orgWithUserLinkedUnlinkedLocationServices(Role.OWNER);
        final JSONObject linkedLocation = orgWithUserLinkedUnlinkedLocationServices.getJSONObject("USER").getJSONObject("LINKED_LOCATION");
        final String orgId = orgWithUserLinkedUnlinkedLocationServices.getJSONObject("ORGANIZATION").getString("id");
        final String linkedLocationId = linkedLocation.getString("id");
        final JSONArray locationServiceIds = linkedLocation.getJSONArray("locationServiceIds");
        final JSONObject user = orgWithUserLinkedUnlinkedLocationServices.getJSONObject("USER");
        organizationFlows.deleteOrganization(orgId);

        UserHelper.linkUnlinkUserToLocationService(getToken(Role.SUPPORT), orgId, user.getString("id"), linkedLocationId, locationServiceIds.getString(1), LINK.name())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

}
