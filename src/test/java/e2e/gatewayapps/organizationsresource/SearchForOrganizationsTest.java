package e2e.gatewayapps.organizationsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.organizationsresource.data.OrganizationsDataProvider;
import helpers.appsapi.support.organizationsresource.OrganizationsHelper;
import helpers.appsapi.support.organizationsresource.payloads.SearchOrganizationRequestBody;
import helpers.flows.OrganizationFlows;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.json.*;
import org.testng.annotations.*;

import java.util.*;

import static helpers.appsapi.support.organizationsresource.payloads.SearchOrganizationRequestBody.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertEquals;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class SearchForOrganizationsTest extends BaseTest {

    private JSONArray allOrganizations;

    @BeforeClass
    void setUp() {
        final int maxSearchSize = 5;
        final OrganizationFlows organizationFlows = new OrganizationFlows();

        final ExtractableResponse<Response> extractableResponse = OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, SearchOrganizationRequestBody.bodyBuilder(OrganizationSearchCombination.DEFAULT))
                .then()
                .statusCode(SC_OK)
                .extract();

        final int totalElements = extractableResponse.path("totalElements");
        final ArrayList<JSONObject> listOfAllOrganizations = extractableResponse.path("content");
        allOrganizations = new JSONArray(listOfAllOrganizations);

        if (totalElements < maxSearchSize) {
            for (int i = 0; i < maxSearchSize; i++) {
                if (i % 2 == 0) {
                    final JSONObject deletedOrganization = organizationFlows.createAndDeletePublishedOrganization().getJSONObject("ORGANIZATION");
                    allOrganizations.put(deletedOrganization);
                } else {
                    final JSONObject organization = organizationFlows.createAndPublishOrganizationWithAllUsers().getJSONObject("ORGANIZATION");

                    allOrganizations.put(organization);
                }
            }
        }

    }

    // TODO add XRay
    @Test(dataProvider = "invalid page", dataProviderClass = OrganizationsDataProvider.class)
    public void searchForOrganizationWithInvalidPage(Object page) {
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(PAGE, page);

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    // TODO add XRay
    @Test
    public void searchForOrganizationWithInvalidPageSize() {
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(PAGE, -1);

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    // TODO add XRay
    @Test(dataProvider = "invalid size", dataProviderClass = OrganizationsDataProvider.class)
    public void searchForOrganizationWithInvalidSize(Object size) {
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, size);

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    // TODO add XRay, Wait discussion from QA team
    @Test(dataProvider = "invalid sorting params", dataProviderClass = OrganizationsDataProvider.class)
    public void searchForOrganizationWithInvalidSort(Object sort) {
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_FULL_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SORT, sort);

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    // TODO add XRay, Wait discussion from QA team
    @Test
    public void searchForOrganizationWithInvalidSortValue() {
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_FULL_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SORT, "aaaa");

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }


    // TODO add XRay
    @Test
    public void searchForOrganizationByFullName() {
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.DEFAULT);
        final JSONObject randomOrganization = allOrganizations.getJSONObject(getRandomInt(allOrganizations.length() - 1));
        final String randomOrganizationName = randomOrganization.getString("internalName");
        searchBody.put(QUERY, randomOrganizationName);

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body("content.size", is(1))
                .body("content.internalName", hasItem(randomOrganizationName))
                .body("content.id", hasItem(randomOrganization.getString("id")))
                .body(matchesJsonSchemaInClasspath("schemas/searchOrganizations.json"));
    }

    // TODO add XRay
    @Test
    public void searchForOrganizationByPartialNameInside() {
        final JSONObject randomOrganization = allOrganizations.getJSONObject(getRandomInt(allOrganizations.length() - 1));
        final String randomOrganizationName = randomOrganization.getString("internalName");
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 50);
        final int start = randomOrganizationName.length() / 3;
        final int end = 2 * randomOrganizationName.length() / 3;
        searchBody.put(QUERY, randomOrganizationName.substring(start, end));

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("content.internalName", hasItem(randomOrganizationName))
                .body(matchesJsonSchemaInClasspath("schemas/searchOrganizations.json"));

    }

    // TODO add XRay
    @Test
    public void searchForOrganizationByPartialNameEnd() {
        final JSONObject randomOrganization = allOrganizations.getJSONObject(getRandomInt(allOrganizations.length() - 1));
        final String randomOrganizationName = randomOrganization.getString("internalName");
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 100);
        searchBody.put(QUERY, randomOrganizationName.substring(randomOrganizationName.length() / 2));

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("content.internalName", hasItem(randomOrganizationName))
                .body(matchesJsonSchemaInClasspath("schemas/searchOrganizations.json"));

    }

    // TODO add XRay
    @Test
    public void searchForOrganizationByPartialNameBeginning() {
        final JSONObject randomOrganization = allOrganizations.getJSONObject(getRandomInt(allOrganizations.length() - 1));
        final String randomOrganizationName = randomOrganization.getString("internalName");
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 50);
        searchBody.put(QUERY, randomOrganizationName.substring(0, randomOrganizationName.length() / 2));

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("content.internalName", hasItem(randomOrganizationName))
                .body(matchesJsonSchemaInClasspath("schemas/searchOrganizations.json"));
    }

    // TODO add XRay
    @Test
    public void searchForOrganizationByPartialIdBeginning() {
        final JSONObject randomOrganization = allOrganizations.getJSONObject(getRandomInt(allOrganizations.length() - 1));
        final String randomOrganizationId = randomOrganization.getString("id");

        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 50);
        searchBody.put(QUERY, randomOrganizationId.substring(0, 5));

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("content.id", hasItem(randomOrganizationId))
                .body(matchesJsonSchemaInClasspath("schemas/searchOrganizations.json"));

    }

    // TODO add XRay
    @Test
    public void searchForOrganizationByPartialIdEnd() {
        final JSONObject randomOrganization = allOrganizations.getJSONObject(getRandomInt(allOrganizations.length() - 1));
        final String randomOrganizationId = randomOrganization.getString("id");

        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 100);
        searchBody.put(QUERY, randomOrganizationId.substring(4));

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("content.id", hasItem(randomOrganizationId))
                .body(matchesJsonSchemaInClasspath("schemas/searchOrganizations.json"));
    }

    // TODO add XRay
    @Test
    public void searchForOrganizationByPartialIdInside() {
        final JSONObject randomOrganization = allOrganizations.getJSONObject(getRandomInt(allOrganizations.length() - 1));
        final String randomOrganizationId = randomOrganization.getString("id");

        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 100);
        searchBody.put(QUERY, randomOrganizationId.substring(3, randomOrganizationId.length() - 2));

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("content.id", hasItem(randomOrganizationId))
                .body(matchesJsonSchemaInClasspath("schemas/searchOrganizations.json"));
    }

    // TODO add XRay
    @Test
    public void searchForOrganizationByPartialUrlBeginning() {
        final JSONObject randomOrganization = allOrganizations.getJSONObject(getRandomInt(allOrganizations.length() - 1));
        final String url = randomOrganization.getString("websiteUrl");
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 50);
        searchBody.put(QUERY, url.substring(0, url.length() - 4));

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("content.websiteUrl", hasItem(url))
                .body(matchesJsonSchemaInClasspath("schemas/searchOrganizations.json"));
    }

    // TODO add XRay
    @Test
    public void searchForOrganizationByDeletedFlag() {
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.DEFAULT);
        searchBody.put(DELETED, true);
        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("content.deleted", hasItem(true))
                .body("content.deleted", not(hasItem(false)))
                .body(matchesJsonSchemaInClasspath("schemas/searchOrganizations.json"));

        searchBody.put(DELETED, false);

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("content.deleted", hasItem(false))
                .body("content.deleted", not(hasItem(true)))
                .body(matchesJsonSchemaInClasspath("schemas/searchOrganizations.json"));
    }

    // TODO add XRay
    @Test
    public void searchForOrganizationByPartialUrlEnd() {
        final JSONObject randomOrganization = allOrganizations.getJSONObject(getRandomInt(allOrganizations.length() - 1));
        final String randomOrganizationUrl = randomOrganization.getString("websiteUrl");
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 100);
        searchBody.put(QUERY, randomOrganizationUrl.substring(3));

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("content.websiteUrl", hasItem(randomOrganizationUrl))
                .body(matchesJsonSchemaInClasspath("schemas/searchOrganizations.json"));
    }

    // TODO add XRay
    @Test
    public void searchForOrganizationByPartialUrlInside() {
        final JSONObject randomOrganization = allOrganizations.getJSONObject(getRandomInt(allOrganizations.length() - 1));
        final String randomOrganizationUrl = randomOrganization.getString("websiteUrl");
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_EMPTY_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 100);
        searchBody.put(QUERY, randomOrganizationUrl.substring(10, randomOrganizationUrl.length() - 1));

        OrganizationsHelper.searchOrganizations(SUPPORT_TOKEN, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("content.websiteUrl", hasItem(randomOrganizationUrl));
    }

    // TODO add XRay
    @Test(dataProvider = "valid size", dataProviderClass = OrganizationsDataProvider.class)
    public void searchForOrganizationWithValidSize(int size) {
        final String token = getToken(Role.SUPPORT);
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_FULL_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, size);

        OrganizationsHelper.searchOrganizations(token, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("numberOfElements", equalTo(size))
                .body("content.size", is(size));

    }

    // TODO add XRay
    @Test(dataProvider = "valid page", dataProviderClass = OrganizationsDataProvider.class)
    public void searchForOrganizationWithValidPage(int page) {
        final String token = getToken(Role.SUPPORT);
        final int size = 10;
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.WITH_FULL_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 10);
        searchBody.getJSONObject(PAGINATION).put(PAGE, page);

        OrganizationsHelper.searchOrganizations(token, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("numberOfElements", equalTo(size));

    }

    // TODO add XRay
    @Test(dataProvider = "valid sorting asc params", dataProviderClass = OrganizationsDataProvider.class, enabled = false)
    public void searchForOrganizationWithValidASCSort(String sort) {
        final String token = getToken(Role.SUPPORT);
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.PAGINATION_AND_SORTING);
        searchBody.getJSONObject(PAGINATION).put(SORT, sort);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 100);
        searchBody.put(DELETED, JSONObject.NULL);
        final String sortKey = getTranslation(sort.split(":")[0]);
        final String searchPath = "content.findAll{it." + sortKey + "}." + sortKey;
        final ArrayList<String> names = OrganizationsHelper.searchOrganizations(token, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path(searchPath);

        final ArrayList<String> copy = new ArrayList<>(names);
        Collections.sort(names);
        assertEquals(copy, names);

    }

    // TODO add XRay
    @Test(dataProvider = "valid sorting desc params", dataProviderClass = OrganizationsDataProvider.class, enabled = false)
    public void searchForOrganizationWithValidDESCSort(String sort) {

        final String token = getToken(Role.SUPPORT);
        final JSONObject searchBody = bodyBuilder(OrganizationSearchCombination.PAGINATION_AND_SORTING);
        searchBody.getJSONObject(PAGINATION).put(SORT, sort);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 100);
        searchBody.put(DELETED, JSONObject.NULL);
        final String sortKey = getTranslation(sort.split(":")[0]);
        final String searchPath = "content.findAll{it." + sortKey + "}." + sortKey;
        final ArrayList<String> names = OrganizationsHelper.searchOrganizations(token, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path(searchPath);

        final ArrayList<String> copy = new ArrayList<>(names);
        Collections.sort(names);
        Collections.reverse(names);
        assertEquals(copy, names);
    }

}
