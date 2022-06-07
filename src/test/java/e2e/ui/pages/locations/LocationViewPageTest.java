package e2e.ui.pages.locations;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.*;
import utils.Xray;

import static configuration.Role.getRandomOrganizationRole;
import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;


public class LocationViewPageTest extends BasePageTest {

    private String organizationId;
    private LocationFlows locationFlows;
    private JSONObject organizationWithAllUsers;
    private CreateLocationRequestBody createLocationRequestBody;


    @BeforeClass
    public void setUp() {
        locationFlows = new LocationFlows();
        createLocationRequestBody = new CreateLocationRequestBody();
        organizationWithAllUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithAllUsers.getJSONObject("ORGANIZATION").getString("id");
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }


    @Xray(requirement = "PEG-6481", test = "7196")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void checkLocationAllDetails(Role role) {
        final String token = role.equals(Role.SUPPORT)? SUPPORT_TOKEN: organizationWithAllUsers.getJSONObject(role.name()).getString("token");
        final JSONObject location = locationFlows.createLocation(organizationId);
        final String locationId = location.getString("id");
        final LocationDetailsViewModePage locationDetailsViewModePage = role.equals(Role.SUPPORT) ?
                new LocationDetailsViewModePage(browserToUse, versionToBe, organizationId, token, locationId):
                new LocationDetailsViewModePage(browserToUse, versionToBe, token, locationId);

        locationDetailsViewModePage.openPage();
        locationDetailsViewModePage.checkLocationAllDetails(location);
    }

    @Xray(requirement = "PEG-6481", test = "7197")
    @Test
    public void checkLocationDetailsWithoutAddressLine2() {
        final Role randomRole = getRandomOrganizationRole();
        final String token = organizationWithAllUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject locationCreationRequestBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        locationCreationRequestBody.getJSONObject(ADDRESS).remove(ADDRESS_LINE_2);
        final JSONObject location = locationFlows.createLocation(organizationId, locationCreationRequestBody);

        final String locationId = location.getString("id");
        final LocationDetailsViewModePage locationDetailsViewModePage =
                new LocationDetailsViewModePage(browserToUse, versionToBe, token, locationId);
        locationDetailsViewModePage.openPage();
        locationDetailsViewModePage.checkLocationDetailsWithoutAddressLine2();
    }

    @Xray(requirement = "PEG-6481", test = "7198")
    @Test()
    public void checkLocationDetailsWithoutPhoneNumber() {
        final JSONObject locationCreationRequestBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        locationCreationRequestBody.remove(PHONE_NUMBER);
        final JSONObject location = locationFlows.createLocation(organizationId, locationCreationRequestBody);

        final String locationId = location.getString("id");
        final LocationDetailsViewModePage locationDetailsViewModePage = new LocationDetailsViewModePage(browserToUse, versionToBe, organizationId, SUPPORT_TOKEN, locationId);
        locationDetailsViewModePage.openPage();
        locationDetailsViewModePage.checkLocationDetailsWithoutPhoneNumber();
    }

    @Xray(requirement = "PEG-6481", test = "7199")
    @Test()
    public void checkLocationDetailsWithoutDescription() {
        final JSONObject locationCreationRequestBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        locationCreationRequestBody.remove(DESCRIPTION);
        final JSONObject location = locationFlows.createLocation(organizationId, locationCreationRequestBody);

        final String locationId = location.getString("id");
        final LocationDetailsViewModePage locationDetailsViewModePage = new LocationDetailsViewModePage(browserToUse, versionToBe, organizationId, SUPPORT_TOKEN, locationId);
        locationDetailsViewModePage.openPage();
        locationDetailsViewModePage.checkLocationDetailsWithoutDescription();
    }

    @Xray(requirement = "PEG-6481", test = "7228")
    @Test()
    public void checkWithInactiveLocationAllDetails() {
        final Role randomeRole = getRandomOrganizationRole();
        final String token = organizationWithAllUsers.getJSONObject(randomeRole.name()).getString("token");
        final JSONObject location = locationFlows.createInactiveLocation(organizationId);
        final String locationId = location.getString("id");
        final LocationDetailsViewModePage locationDetailsViewModePage =
                new LocationDetailsViewModePage(browserToUse, versionToBe, token, locationId);

        locationDetailsViewModePage.openPage();
        locationDetailsViewModePage.checkLocationAllDetails(location);
    }
}
