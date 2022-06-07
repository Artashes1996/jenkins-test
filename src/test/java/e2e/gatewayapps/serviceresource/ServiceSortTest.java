package e2e.gatewayapps.serviceresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.appsapi.servicesresource.payloads.LinkawareSearchServiceRequestBody;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import io.restassured.response.ValidatableResponse;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.testng.AssertJUnit.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;


public class ServiceSortTest extends BaseTest {

    private String password;
    private String orgId;
    private String ownerToken;

    private int serviceCount;

    @BeforeClass
    public void setUp() {
        password = "Qw!123456";
        JSONObject organizationUser = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        orgId = organizationUser.getJSONObject("ORGANIZATION").getString("id");
        String email = organizationUser.getJSONObject("user").getString("email");
        ownerToken = new AuthenticationFlowHelper().getTokenWithEmailPassword(email, password);
        serviceCount = 4;

    }


    @Test(testName = "PEG-2051, PEG-2067", dataProviderClass = RoleDataProvider.class, dataProvider = "adminRoles")
    public void checkDefaultOrderOfServices(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final ValidatableResponse validatableResponse = ServicesHelper.sortAndSearchService(token, orgId, new JSONObject())
                .then()
                .statusCode(SC_OK);
        validatableResponse.assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));
    }

    @Test(testName = "PEG-2068")
    public void checkOrderOfServicesByAdmin() {
        final String employeeEmail = new UserFlows().createUser(orgId, ADMIN, null).getString("email");
        final String token = new AuthenticationFlowHelper().getTokenWithEmailPassword(employeeEmail, password);
        ServicesHelper.sortAndSearchService(token, orgId, new JSONObject())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test(testName = "PEG-2071, PEG-2072, PEG-2073, PEG-2074", dataProviderClass = RoleDataProvider.class, dataProvider = "adminRoles")
    public void checkWithDifferentSizeAndPage(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final JSONObject sortJson = LinkawareSearchServiceRequestBody.bodyBuilder();
        sortJson.getJSONObject("pagination").put(LinkawareSearchServiceRequestBody.SIZE, serviceCount + 10);
        final ValidatableResponse validatableResponse1 = ServicesHelper.sortAndSearchService(token, orgId, sortJson)
                .then()
                .statusCode(SC_OK);
        validatableResponse1.assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));

        sortJson.getJSONObject("pagination").put(LinkawareSearchServiceRequestBody.SIZE, serviceCount);
        final ValidatableResponse validatableResponse = ServicesHelper.sortAndSearchService(token, orgId, sortJson)
                .then()
                .statusCode(SC_OK);
        validatableResponse.assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));


        sortJson.getJSONObject("pagination").put(LinkawareSearchServiceRequestBody.SIZE, serviceCount + 1);
        sortJson.getJSONObject("pagination").put(LinkawareSearchServiceRequestBody.PAGE, 1);
        final ValidatableResponse validatableResponse2 = ServicesHelper.sortAndSearchService(token, orgId, sortJson)
                .then()
                .statusCode(SC_OK);
        validatableResponse2.assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));
        final ArrayList<String> parentContent = validatableResponse2.extract().path("content");
        assertEquals(0, parentContent.size());

        sortJson.getJSONObject("pagination").put(LinkawareSearchServiceRequestBody.SIZE, serviceCount);
        sortJson.getJSONObject("pagination").put(LinkawareSearchServiceRequestBody.PAGE, 1);
        final ValidatableResponse validatableResponse3 = ServicesHelper.sortAndSearchService(token, orgId, sortJson)
                .then()
                .statusCode(SC_OK);
        validatableResponse3.assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));
        final ArrayList<String> lastParentName = validatableResponse3.extract().path("content");
        assertEquals(lastParentName.get(0), parentContent.get(parentContent.size() - 1));
    }

    @Test(testName = "PEG-2076")
    public void checkOrderWithNonExistingOrgId() {
        final String nonExistingOrgId = UUID.randomUUID().toString();
        ServicesHelper.sortAndSearchService(SUPPORT_TOKEN, nonExistingOrgId, new JSONObject())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test(testName = "PEG-2075")
    public void checkOrderWithOtherOrgOwner() {
        final String otherOrgOwnerToken = getToken(OWNER);
        ServicesHelper.sortAndSearchService(otherOrgOwnerToken, orgId, new JSONObject())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test(testName = "PEG-2077, PEG-2078", dataProviderClass = RoleDataProvider.class, dataProvider = "adminRoles")
    public void checkDescNameOrder(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;

        final JSONObject sortJson = new JSONObject();
        sortJson.put(LinkawareSearchServiceRequestBody.PAGINATION, new JSONObject().put(LinkawareSearchServiceRequestBody.SORT, "INTERNAL_NAME:" + LinkawareSearchServiceRequestBody.SortDirection.DESC));

        final ValidatableResponse validatableResponse = ServicesHelper.sortAndSearchService(token, orgId, sortJson)
                .then()
                .statusCode(SC_OK);
        validatableResponse.assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));
    }


    @Test(testName = "PEG-2079")
    public void checkWithIncorrectSort() {
        final String incorrectSort = "INTERNAL_NAME:NVAZOX";
        final JSONObject sortJson = new JSONObject();
        sortJson.put(LinkawareSearchServiceRequestBody.PAGINATION, new JSONObject().put(LinkawareSearchServiceRequestBody.SORT, incorrectSort));

        ServicesHelper.sortAndSearchService(SUPPORT_TOKEN, orgId, sortJson)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

}
