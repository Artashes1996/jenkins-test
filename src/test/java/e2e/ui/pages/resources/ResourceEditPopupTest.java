package e2e.ui.pages.resources;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.appsapi.resourcesresource.payloads.ResourceUpdateRequestBody;
import helpers.flows.OrganizationFlows;
import helpers.flows.ResourceFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.ResourceListPage;
import pages.SignInPage;
import utils.RetryAnalyzer;
import utils.Xray;

import java.util.Collections;

import static helpers.appsapi.resourcesresource.payloads.ResourceCreationBody.INTERNAL_NAME;
import static helpers.appsapi.resourcesresource.payloads.ResourceUpdateRequestBody.NAME_TRANSLATION;
import static helpers.appsapi.resourcesresource.payloads.ResourceUpdateRequestBody.Status.*;

public class ResourceEditPopupTest extends BasePageTest {

    private JSONObject organizationWithUsers;
    private String organizationId;
    private JSONObject activeResource;

    @BeforeClass
    public void setUp() {
        organizationWithUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        activeResource = new ResourceFlows().createActiveResource(organizationId, Collections.singletonList(organizationWithUsers.getJSONObject("LOCATION").getString("id")));
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-3250", test = "PEG-5227")
    @Test(retryAnalyzer = RetryAnalyzer.class, dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void changeDisplayName(Role role){
        final JSONObject editResourceJsonObject = new ResourceUpdateRequestBody().bodyBuilder(ACTIVE);
        editResourceJsonObject.put(INTERNAL_NAME, activeResource.getString(INTERNAL_NAME));
        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT)?new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken):
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        resourceListPage.openPage();

        resourceListPage.clickThreeDotItemActionsMenu();
        resourceListPage.openEditPopup();
        resourceListPage.fillInEditResourcePopup(editResourceJsonObject);
        resourceListPage.saveResourcePopup();

        resourceListPage.checkEditConfirmationToastText();
        resourceListPage.confirmResourceChanges();
        resourceListPage.checkResourceInList(0, editResourceJsonObject);
        resourceListPage.checkEditSuccessToast();
    }

    @Xray(requirement = "PEG-3250", test = "PEG-5228")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void removeDisplayName(){
        final JSONObject editResourceJsonObject = new ResourceUpdateRequestBody().bodyBuilder(ACTIVE);
        editResourceJsonObject.put(INTERNAL_NAME, activeResource.getString(INTERNAL_NAME));
        editResourceJsonObject.put(NAME_TRANSLATION, " ");

        final Role role = Role.getRandomAdminRole();

        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT)?new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken):
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        resourceListPage.openPage();

        resourceListPage.clickThreeDotItemActionsMenu();
        resourceListPage.openEditPopup();
        resourceListPage.fillInEditResourcePopup(editResourceJsonObject);
        resourceListPage.saveResourcePopup();

        resourceListPage.checkEditConfirmationToastText();
        resourceListPage.confirmResourceChanges();

        resourceListPage.checkResourceInList(0, editResourceJsonObject);
        resourceListPage.checkEditSuccessToast();
    }

    @Xray(requirement = "PEG-3250", test = "PEG-5229")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkCancelButton(){
        final ResourceListPage resourceListPage = new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken);
        resourceListPage.openPage();

        resourceListPage.clickThreeDotItemActionsMenu();
        resourceListPage.openEditPopup();
        resourceListPage.closeResourcePopup();
        resourceListPage.checkPopupIsClosed();
    }

    @Xray(requirement = "PEG-3250", test = "PEG-5230")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkNoButtonOfWarningToastMessage(){
        final JSONObject editResourceJsonObject = new ResourceUpdateRequestBody().bodyBuilder(ACTIVE);
        editResourceJsonObject.put(INTERNAL_NAME, activeResource.getString(INTERNAL_NAME));

        final Role role = Role.getRandomOrganizationAdminRole();
        final ResourceListPage resourceListPage = new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        resourceListPage.openPage();

        resourceListPage.clickThreeDotItemActionsMenu();
        resourceListPage.openEditPopup();
        resourceListPage.fillInEditResourcePopup(editResourceJsonObject);
        resourceListPage.saveResourcePopup();

        resourceListPage.checkEditConfirmationToastText();
        resourceListPage.rejectResourceChanges();
    }

    @Xray(requirement = "PEG-3250", test = "PEG-5231")
    @Test(retryAnalyzer = RetryAnalyzer.class, dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void checkStatusChange(Role role){
        final String newOrganizationId = new OrganizationFlows().createUnpublishedOrganization().getString("id");
        final JSONObject createdResource = new ResourceFlows().createActiveResource(newOrganizationId, null);

        final JSONObject editResourceJsonObject = new ResourceUpdateRequestBody().bodyBuilder(INACTIVE);
        editResourceJsonObject.put(INTERNAL_NAME, createdResource.getString(INTERNAL_NAME));

        final ResourceListPage resourceListPage = role.equals(Role.SUPPORT)?new ResourceListPage(browserToUse, versionToBe, organizationId, supportToken):
                new ResourceListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        resourceListPage.openPage();

        resourceListPage.clickThreeDotItemActionsMenu();
        resourceListPage.openEditPopup();
        resourceListPage.fillInEditResourcePopup(editResourceJsonObject);
        resourceListPage.saveResourcePopup();

        resourceListPage.checkResourceInList(0, editResourceJsonObject);
        resourceListPage.checkEditSuccessToast();
    }

}
