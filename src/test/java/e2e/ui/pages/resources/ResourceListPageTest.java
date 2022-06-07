package e2e.ui.pages.resources;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.ResourceFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.ResourceListPage;
import pages.SignInPage;
import utils.RetryAnalyzer;
import utils.TestUtils;
import utils.Xray;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceListPageTest extends BasePageTest {

    private JSONObject organizationWithUsers;
    private String organizationId;
    private JSONObject initiallyCreatedLocation;
    private JSONObject virtualLocation;
    private JSONObject physicalLocation;

    private final List<JSONObject> linkedActiveResources = new ArrayList<>();
    private final List<JSONObject> linkedInactiveResources = new ArrayList<>();
    private final List<JSONObject> unlinkedResources = new ArrayList<>();

    @BeforeClass
    public void setUp() {
        final LocationFlows locationFlows = new LocationFlows();
        final ResourceFlows resourceFlows = new ResourceFlows();
        organizationWithUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final JSONObject organization = organizationWithUsers.getJSONObject("ORGANIZATION");
        organizationId = organization.getString("id");
        initiallyCreatedLocation = organizationWithUsers.getJSONObject("LOCATION");
        virtualLocation = locationFlows.createLocation(organizationId);
        physicalLocation = locationFlows.createLocation(organizationId);

        for (int i = 0; i < 10; i++) {
            linkedActiveResources.add(resourceFlows.createActiveResource(organizationId, Collections.singletonList(virtualLocation.getString("id"))));
            linkedInactiveResources.add(resourceFlows.createInactiveResource(organizationId, Collections.singletonList(physicalLocation.getString("id"))));
            unlinkedResources.add(resourceFlows.createActiveResource(organizationId, null));
        }
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-3041", test = "PEG-4875")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkPaginationTextAndNumbers(){
        final int expectedResourceCount = linkedActiveResources.size() + linkedInactiveResources.size() + unlinkedResources.size();
        final ResourceListPage resourceListPage = new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken);
        resourceListPage.openPage();
        resourceListPage.checkResourceCountInPaginationText(expectedResourceCount);
    }

    @Xray(requirement = "PEG-3041", test = "PEG-4876")
    @Test(retryAnalyzer = RetryAnalyzer.class, dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void checkDefaultAscendingOrder(Role role){
        final List<JSONObject> expectedResourceList = Stream.of(linkedActiveResources, linkedInactiveResources, unlinkedResources)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT)?new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken):
        new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        resourceListPage.openPage();
        resourceListPage.checkResourcesInList(expectedResourceList);
    }

    @Xray(requirement = "PEG-3041", test = "PEG-4877")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkResourceOrderChange(){
        final List<JSONObject> expectedResourceList = Stream.of(linkedActiveResources, linkedInactiveResources, unlinkedResources)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        Collections.reverse(expectedResourceList);

        final ResourceListPage resourceListPage = new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken);
        resourceListPage.openPage();
        resourceListPage.changeOrderOfResources();
        resourceListPage.checkResourcesInList(expectedResourceList);
    }

    @Xray(requirement = "PEG-3043", test = "PEG-4878")
    @Test(retryAnalyzer = RetryAnalyzer.class, dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void checkDropdownSelectAllFunctionality(Role role){
        final int existingLocationCount = 3;
        final List<JSONObject> expectedResourceList = Stream.of(linkedActiveResources, linkedInactiveResources)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());

        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT)?new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken):
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        resourceListPage.openPage();
        resourceListPage.clickOnLocationDropDown();
        resourceListPage.clickOnSelectAllOptionFromLocationsDropDown(existingLocationCount);

        resourceListPage.checkResourcesInList(expectedResourceList);
    }

    @Xray(requirement = "PEG-3043", test = "PEG-4879")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkDropdownNoResourceOptionFunctionality(){
        unlinkedResources.sort(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")));

        final ResourceListPage resourceListPage = new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken);
        resourceListPage.openPage();
        resourceListPage.clickOnLocationDropDown();
        resourceListPage.clickOnNoLocationsFromLocationsDropDown();

        resourceListPage.checkResourcesInList(unlinkedResources);
    }


    @Xray(requirement = "PEG-3043", test = "PEG-5226")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkDropdownNoResourceDoubleClickFunctionality(){
        final List<JSONObject> expectedResourceList = Stream.of(linkedActiveResources, linkedInactiveResources, unlinkedResources)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());

        final ResourceListPage resourceListPage = new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken);
        resourceListPage.openPage();
        resourceListPage.clickOnLocationDropDown();
        resourceListPage.clickOnNoLocationsFromLocationsDropDown();
        resourceListPage.unselectAllOptionsInLocationDropdown();
        resourceListPage.closeLocationDropdown();

        resourceListPage.checkResourcesInList(expectedResourceList);
    }

    @Xray(requirement = "PEG-3043", test = "PEG-4881")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkEmptyDropDownText (){
        final Role role = Role.getRandomRole();

        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT)?new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken):
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        resourceListPage.openPage();
        resourceListPage.clickOnLocationDropDown();
        resourceListPage.fillLocationDropDownSearchField(TestUtils.getRandomPhoneNumber());
        resourceListPage.checkEmptyLocationDropDownText();
    }

    @Xray(requirement = "PEG-3043", test = "PEG-4880")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkLocationFilterWithSeveralLocations() {
        final List<JSONObject> expectedResourceList = Stream.of(linkedInactiveResources, linkedActiveResources)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());

        final Role role = Role.getRandomRole();

        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT)?new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken):
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        resourceListPage.openPage();
        resourceListPage.clickOnLocationDropDown();
        resourceListPage.clickOnOptionFromLocationsDropDownByName(virtualLocation.getString("internalName"));
        resourceListPage.clickOnOptionFromLocationsDropDownByName(physicalLocation.getString("internalName"));

        resourceListPage.checkResourcesInList(expectedResourceList);
    }

    @Xray(requirement = "PEG-3041", test = "PEG-4883")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void searchForResource() {
        final Role role = Role.getRandomRole();
        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT)?new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken):
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        resourceListPage.openPage();
        resourceListPage.searchForResource(linkedActiveResources.get(0).getString("internalName"));
        resourceListPage.checkResourceInList(0, linkedActiveResources.get(0));
    }

    @Xray(requirement = "PEG-3041", test = "PEG-4884")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void searchWithInvalidValue() {
        final String searchBy = TestUtils.getRandomPhoneNumber();

        final Role role = Role.getRandomRole();
        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT)?new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken):
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        resourceListPage.openPage();
        resourceListPage.searchForResource(searchBy);
        resourceListPage.checkEmptySearchPageText(searchBy);
    }

    @Xray(requirement = "PEG-3041", test = "PEG-4885")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkActionColumnInvisibilityByRoles() {
        final Role role = Role.getRandomRolesWithLocation();
        final ResourceListPage resourceListPage = new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        resourceListPage.openPage();
        resourceListPage.checkActionsColumnIsMissing();
    }

    @Xray(requirement = "PEG-3043", test = "PEG-4882")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkLocationDropDownSearchClean(){
        final List<String> locationNames = Arrays.asList(physicalLocation.getString("internalName"),
                                                        virtualLocation.getString("internalName"),
                                                        initiallyCreatedLocation.getString("internalName"));

        final ResourceListPage resourceListPage = new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken);
        resourceListPage.openPage();
        resourceListPage.clickOnLocationDropDown();
        resourceListPage.checkLocationDropDownItems(locationNames);
        resourceListPage.fillLocationDropDownSearchField(virtualLocation.getString("internalName"));
        resourceListPage.checkLocationDropDownItems(Collections.singletonList(virtualLocation.getString("internalName")));
        resourceListPage.clearLocationDropDownSearchField();
        resourceListPage.checkLocationDropDownItems(locationNames);
    }

}