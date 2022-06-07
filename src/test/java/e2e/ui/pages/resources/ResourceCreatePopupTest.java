package e2e.ui.pages.resources;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.appsapi.resourcesresource.payloads.ResourceCreationBody;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.ResourceFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.ResourceListPage;
import pages.SignInPage;
import utils.Xray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static helpers.appsapi.resourcesresource.payloads.ResourceCreationBody.NAME_TRANSLATION;
import static helpers.appsapi.resourcesresource.payloads.ResourceCreationBody.STATUS;
import static helpers.appsapi.resourcesresource.payloads.ResourceUpdateRequestBody.Status.INACTIVE;

public class ResourceCreatePopupTest extends BasePageTest {

    private JSONObject organizationWithUsers;
    private String organizationId;
    private JSONObject virtualLocation;
    private JSONObject physicalLocation;

    @BeforeClass
    public void setUp() {
        final LocationFlows locationFlows = new LocationFlows();
        organizationWithUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        JSONObject organization = organizationWithUsers.getJSONObject("ORGANIZATION");
        organizationId = organization.getString("id");
        virtualLocation = locationFlows.createLocation(organizationId);
        physicalLocation = locationFlows.createLocation(organizationId);
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-3037", test = "PEG-4966")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "extendedAdminRoles")
    public void createActiveResource(Role role) {
        final JSONObject resourceCreationBody = new ResourceCreationBody().bodyBuilder(ResourceCreationBody.RESOURCE_CREATION_COMBINATION.REQUIRED, organizationId);
        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT) ? new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        resourceListPage.openPage();
        resourceListPage.openCreateResourcePopup();
        resourceListPage.fillResourceFields(resourceCreationBody);
        resourceListPage.saveResourcePopup();
        resourceListPage.checkCreateSuccessToast();
    }

    @Xray(requirement = "PEG-3037", test = "PEG-4967")
    @Test
    public void createInactiveResource() {
        final JSONObject pausedOrganization = new OrganizationFlows().createPausedOrganizationWithAllUsers();
        final String pausedOrganizationId = pausedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject resourceCreationBody = new ResourceCreationBody().bodyBuilder(ResourceCreationBody.RESOURCE_CREATION_COMBINATION.REQUIRED, pausedOrganizationId);
        resourceCreationBody.put(STATUS, INACTIVE.name());

        final Role role = Role.getRandomAdminRole();
        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT) ? new ResourceListPage(browserToUse, versionToBe, pausedOrganizationId, supportToken) :
                new ResourceListPage(browserToUse, versionToBe, pausedOrganization.getJSONObject(role.name()).getString("token"));

        resourceListPage.openPage();
        resourceListPage.openCreateResourcePopup();
        resourceListPage.fillResourceFields(resourceCreationBody);
        resourceListPage.saveResourcePopup();
        resourceListPage.checkCreateSuccessToast();
        resourceListPage.checkResourceInList(0, resourceCreationBody);
    }

    @Xray(requirement = "PEG-3037", test = "PEG-4968")
    @Test
    public void createResourceWithLocations() {
        final JSONObject resourceCreationBody = new ResourceCreationBody().bodyBuilder(ResourceCreationBody.RESOURCE_CREATION_COMBINATION.REQUIRED, organizationId);
        final List<String> locationNames = new ArrayList<>();
        locationNames.add(physicalLocation.getString("internalName"));
        locationNames.add(virtualLocation.getString("internalName"));

        final Role role = Role.getRandomAdminRole();
        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT) ? new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        resourceListPage.openPage();
        resourceListPage.openCreateResourcePopup();
        resourceListPage.fillResourceFields(resourceCreationBody, locationNames);
        resourceListPage.saveResourcePopup();
        resourceListPage.checkCreateSuccessToast();
    }

    @Xray(requirement = "PEG-3037", test = "PEG-4968")
    @Test
    public void checkSearchLocationsInCreateResourcePopup() {
        final JSONObject resourceCreationBody = new ResourceCreationBody().bodyBuilder(ResourceCreationBody.RESOURCE_CREATION_COMBINATION.REQUIRED, organizationId);
        final String locationName = virtualLocation.getString("internalName");

        final Role role = Role.getRandomAdminRole();
        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT) ? new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        resourceListPage.openPage();
        resourceListPage.openCreateResourcePopup();
        resourceListPage.clickOnLocationDropDownInsideCreatePopup();
        resourceListPage.fillLocationDropDownSearchField(locationName);
        resourceListPage.checkLocationDropDownItems(Collections.singletonList(locationName));
        resourceListPage.clearLocationDropDownSearchField();
        resourceListPage.closeLocationDropdown();
        resourceListPage.fillResourceFields(resourceCreationBody);
        resourceListPage.saveResourcePopup();
        resourceListPage.checkCreateSuccessToast();
    }

    @Xray(requirement = "PEG-3037", test = "PEG-5055")
    @Test
    public void checkEmptyNameState() {
        final ResourceListPage resourceListPage = new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken);
        resourceListPage.openPage();
        resourceListPage.openCreateResourcePopup();
        resourceListPage.checkCreateButtonIsDisabled();
    }

    @Xray(requirement = "PEG-3037", test = "PEG-5056")
    @Test
    public void createResourceWithExistingName() {
        final JSONObject resourceExisting = new ResourceFlows().createActiveResource(organizationId, null);
        final Role role = Role.getRandomAdminRole();
        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT) ? new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        resourceListPage.openPage();
        resourceListPage.openCreateResourcePopup();
        resourceListPage.fillResourceFields(resourceExisting);
        resourceListPage.saveResourcePopup();
        resourceListPage.checkAlreadyExistingResourceToast();
    }

    @Xray
    @Test
    public void createResourceWithSameDisplayAndInternalName() {
        final JSONObject resourceCreationBody = new ResourceCreationBody().bodyBuilder(ResourceCreationBody.RESOURCE_CREATION_COMBINATION.REQUIRED, organizationId);
        resourceCreationBody.put(NAME_TRANSLATION, "");
        resourceCreationBody.put(STATUS, INACTIVE);

        final Role role = Role.getRandomAdminRole();
        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT) ? new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        resourceListPage.openPage();
        resourceListPage.openCreateResourcePopup();
        resourceListPage.fillResourceFields(resourceCreationBody);
        resourceListPage.saveResourcePopup();
        resourceListPage.checkCreateSuccessToast();
    }

    @Xray
    @Test
    public void checkCreateResourcePopupCloseButton() {
        final JSONObject resourceCreationBody = new ResourceCreationBody().bodyBuilder(ResourceCreationBody.RESOURCE_CREATION_COMBINATION.REQUIRED, organizationId);

        final Role role = Role.getRandomAdminRole();
        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT) ? new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        resourceListPage.openPage();
        resourceListPage.openCreateResourcePopup();
        resourceListPage.fillResourceFields(resourceCreationBody);
        resourceListPage.closeResourcePopup();
        resourceListPage.checkNoResourceCreated();
    }

}
