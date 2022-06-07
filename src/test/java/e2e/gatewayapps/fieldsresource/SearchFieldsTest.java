package e2e.gatewayapps.fieldsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.fieldsresource.data.FieldsDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.fieldsresource.FieldsHelper;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.appsapi.fieldsresource.payloads.FieldsSearchBody;
import helpers.flows.FieldsFlows;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;
import java.util.stream.*;

import static configuration.Role.*;
import static helpers.appsapi.fieldsresource.payloads.FieldsSearchBody.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class SearchFieldsTest extends BaseTest {

    private String organizationId;
    private JSONObject usersAndOrganization;
    private FieldsFlows fieldsFlows;

    private List<String> defaultFieldsNames;
    private List<String> serviceDefaultFieldsNames;
    private List<JSONObject> customFields;
    private List<String> customFieldNames;
    private List<JSONObject> customFieldsWithOptions;
    private FieldsSearchBody fieldsSearchBody;

    @BeforeClass
    public void setUp() {
        defaultFieldsNames = new ArrayList<>();
        serviceDefaultFieldsNames = new ArrayList<>();
        customFields = new ArrayList<>();
        customFieldNames = new ArrayList<>();
        fieldsSearchBody = new FieldsSearchBody();

        usersAndOrganization = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = usersAndOrganization.getJSONObject("ORGANIZATION").getString("id");
        fieldsFlows = new FieldsFlows();

        for (int i = 0; i < 10; i++) {
            customFields = Stream.concat(customFields.stream(), fieldsFlows.createAllTypesOfFields(organizationId).stream())
                    .collect(Collectors.toList());
        }

        customFields = customFields.stream()
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        customFieldNames = customFields.stream().map(obj -> obj.getString("internalName")).collect(Collectors.toList());
        Collections.sort(customFieldNames);

        customFieldsWithOptions = customFields.stream().filter(obj -> !(obj.getString("type").equals(FieldTypes.TEXT.name()) || obj.getString("type").equals(FieldTypes.NUMBER.name()))).collect(Collectors.toList());

        defaultFieldsNames.add("Phone Number");
        defaultFieldsNames.add("Email");
        defaultFieldsNames.add("First Name");
        defaultFieldsNames.add("Last Name");
        defaultFieldsNames.add("Address");
        defaultFieldsNames.add("City");
        defaultFieldsNames.add("Postal Code");
        Collections.sort(defaultFieldsNames);

        serviceDefaultFieldsNames.add("First Name");
        serviceDefaultFieldsNames.add("Last Name");
        serviceDefaultFieldsNames.add("Phone Number");
        serviceDefaultFieldsNames.add("Email");
    }

    @Xray(requirement = "PEG-1725", test = "PEG-1892")
    @Test(dataProviderClass = FieldsDataProvider.class, dataProvider = "invalid page")
    public void searchWithInvalidPage(Object page) {
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.WITH_PAGINATION);
        searchBody.getJSONObject(FieldsSearchBody.PAGINATION).put(PAGE, page);

        FieldsHelper.searchFields(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray(requirement = "PEG-1725", test = "PEG-1894")
    @Test(dataProviderClass = FieldsDataProvider.class, dataProvider = "invalid size")
    public void searchWithInvalidSize(Object size) {
        final String token = usersAndOrganization.getJSONObject(getRandomOrganizationRole().name()).getString("token");
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.WITH_PAGINATION);
        searchBody.getJSONObject(FieldsSearchBody.PAGINATION).put(SIZE, size);

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-1725", test = "PEG-1893")
    @Test(dataProviderClass = FieldsDataProvider.class, dataProvider = "invalid sort")
    public void searchWithInvalidSort(Object sort) {
        final String token = usersAndOrganization.getJSONObject(getRandomOrganizationRole().name()).getString("token");
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.WITH_PAGINATION);
        searchBody.getJSONObject(FieldsSearchBody.PAGINATION).put(SORT, sort);

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-1725", test = "PEG-1902")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchByCustomFieldName(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(role.name()).getString("token");
        final String fieldName = customFieldNames.get(0);
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.REQUIRED_FIELDS);
        searchBody.put(QUERY, fieldName);
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(1))
                .body("content.internalName", hasItem(fieldName));

        searchBody.put(QUERY, fieldName.substring(4, 20));
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", hasItem(fieldName));
    }

    @Xray(requirement = "PEG-1725", test = "PEG-1903")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchByDefaultFieldName(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(role.name()).getString("token");
        final String fieldName = defaultFieldsNames.get(6);
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.REQUIRED_FIELDS);
        searchBody.put(QUERY, fieldName);
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(1))
                .body("content.internalName", hasItem(fieldName));

        searchBody.put(QUERY, fieldName.substring(0, 5));
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", hasItem(fieldName));
    }

    @Xray(requirement = "PEG-1724", test = "PEG-1906")
    @Test
    public void searchById() {
        final String fieldId = Integer.toString(customFields.get(0).getInt("id"));
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.REQUIRED_FIELDS);

        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        searchBody.put(QUERY, fieldId);
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.id", hasItem(fieldId));
    }

    @Xray(requirement = "PEG-1725", test = "PEG-1908")
    @Test
    public void searchByOptionName() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final String fieldOptionName = customFieldsWithOptions.get(0).getJSONArray("options").getJSONObject(0).getString("internalName");
        final String fieldName = customFieldsWithOptions.get(0).getString("internalName");
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.REQUIRED_FIELDS);

        searchBody.put(QUERY, fieldOptionName);
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", hasItem(fieldName));

        searchBody.put(QUERY, fieldName.substring(5));
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", hasItem(fieldName));
    }

    @Xray(requirement = "PEG-1724", test = "PEG-1916")
    @Test
    public void checkInitialOrderOfFields() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.ALL_FIELDS);
        searchBody.getJSONObject(PAGINATION).remove(SORT);

        final List<String> allNames = Stream.concat(defaultFieldsNames.stream(), customFieldNames.stream())
                .collect(Collectors.toList());
        final List<String> firstPageList = allNames.subList(0, 50);
        final List<String> secondPageList = allNames.subList(50, allNames.size());

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", is(firstPageList));

        searchBody.getJSONObject(PAGINATION).put(PAGE, 1);
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", is(secondPageList));
    }

    @Xray(requirement = "PEG-1724", test = "PEG-1917")
    @Test
    public void searchWithNoPagination() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.REQUIRED_FIELDS);

        final List<String> allNames = Stream.concat(defaultFieldsNames.stream(), customFieldNames.stream())
                .collect(Collectors.toList());
        final List<String> firstPageList = allNames.subList(0, 20);

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", is(firstPageList));
    }

    @Xray(requirement = "PEG-1724", test = "PEG-1918")
    @Test
    public void orderByNameDescending() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.ALL_FIELDS);
        searchBody.getJSONObject(PAGINATION).put(SORT, SortingBy.INTERNAL_NAME.getDescending());
        final List<String> allNamesDesc = Stream.concat(defaultFieldsNames.stream(), customFieldNames.stream()).sorted(Collections.reverseOrder()).collect(Collectors.toList());
        searchBody.getJSONObject(PAGINATION).put(SIZE, allNamesDesc.size());

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", is(allNamesDesc));
    }

    @Xray(requirement = "PEG-1724", test = "PEG-1922")
    @Test
    public void orderByCustomFieldsOrderProperty() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.WITH_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SORT, SortingBy.CUSTOM.getAscending());
        searchBody.getJSONObject(PAGINATION).put(SIZE, defaultFieldsNames.size() + customFieldNames.size());

        final List<Boolean> expectedIsCustomField = new ArrayList<>(Collections.nCopies(defaultFieldsNames.size(), false));
        expectedIsCustomField.addAll(Collections.nCopies(customFieldNames.size(), true));

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(defaultFieldsNames.size() + customFieldNames.size()))
                .body("content.custom", is(expectedIsCustomField));
    }

    @Xray(requirement = "PEG-1724", test = "PEG-1907")
    @Test
    public void orderByNameAndCustomFields() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.WITH_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SIZE, customFieldNames.size() + defaultFieldsNames.size());
        searchBody.getJSONObject(PAGINATION).put(SORT, SortingBy.CUSTOM.getDescending() + "," + SortingBy.INTERNAL_NAME.getAscending());
        final List<String> allNames = Stream.concat(customFieldNames.stream(), defaultFieldsNames.stream())
                .collect(Collectors.toList());
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", is(allNames));

        searchBody.getJSONObject(PAGINATION).put(SORT, SortingBy.INTERNAL_NAME.getAscending() + "," + SortingBy.CUSTOM.getAscending());
        Collections.sort(allNames);
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", is(allNames));
    }

    @Xray(requirement = "PEG-1724", test = "PEG-1901")
    @Test
    public void searchForNonExistingField() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.WITH_PAGINATION);
        searchBody.put(QUERY, getRandomInt() + "fffiiiieeeellllddd");
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(0));
    }

    @Xray(test = "PEG-6001", requirement = "PEG-5517")
    @Test
    public void defaultTrueFilter() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.DEFAULT_FIELDS);
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", is(defaultFieldsNames));
    }

    @Xray(test = "PEG-6002", requirement = "PEG-5517")
    @Test
    public void defaultTrueServiceDefaultTrueFilter() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.SERVICE_DEFAULT_FIELDS);
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", is(serviceDefaultFieldsNames));
    }

    @Xray(test = "PEG-6003", requirement = "PEG-5517")
    @Test
    public void defaultFalseFilter() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.CUSTOM_FIELDS);
        searchBody.put(CUSTOM, true);
        final List<String> firstPageList = customFieldNames.subList(0, 20);

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("totalElements", is(customFieldNames.size()))
                .body("content.internalName", is(firstPageList));
    }

    @Xray(test = "PEG-6004", requirement = "PEG-5517")
    @Test
    public void defaultTrueServiceDefaultFalseFilter() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.SERVICE_DEFAULT_FIELDS);
        searchBody.put(SERVICE_DEFAULTS, false);

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("totalElements", is(defaultFieldsNames.size()))
                .body("content.internalName", is(defaultFieldsNames));
    }

    @Xray(test = "PEG-6005", requirement = "PEG-5517")
    @Test
    public void defaultFalseWithServiceDefaultFilter() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.SERVICE_DEFAULT_FIELDS);
        searchBody.put(CUSTOM, true);

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("totalElements", is(customFieldNames.size()));
    }


    @Xray(test = "PEG-6006", requirement = "PEG-5517")
    @Test
    public void serviceDefaultWithoutDefaultFilter() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.SERVICE_DEFAULT_FIELDS);
        searchBody.remove(CUSTOM);

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("totalElements", is(customFieldNames.size() + defaultFieldsNames.size()));
    }

    @Xray(test = "PEG-6036", requirement = "PEG-5517")
    @Test
    public void queryWithDefaultFields() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.DEFAULT_FIELDS);
        searchBody.put(QUERY, defaultFieldsNames.get(0));
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(1))
                .body("content.internalName", hasItem(defaultFieldsNames.get(0)));

        searchBody.put(QUERY, customFieldNames.get(0));
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(0));
    }

    @Xray(test = "PEG-6011", requirement = "PEG-5517")
    @Test
    public void paginationWithDefaultFields() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.ALL_FIELDS);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 1);
        searchBody.put(CUSTOM, false);

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(defaultFieldsNames.size()));
    }

    @Xray(test = "PEG-6038", requirement = "PEG-5517")
    @Test
    public void queryWithServiceDefaultFields() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.SERVICE_DEFAULT_FIELDS);
        searchBody.put(QUERY, serviceDefaultFieldsNames.get(0));
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(1))
                .body("content.internalName", hasItem(serviceDefaultFieldsNames.get(0)));

        searchBody.put(QUERY, customFieldNames.get(0));
        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(0));
    }

    @Xray(test = "PEG-6037", requirement = "PEG-5517")
    @Test
    public void paginationWithServiceDefaultFields() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.ALL_FIELDS);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 1);
        searchBody.put(CUSTOM, false);
        searchBody.put(SERVICE_DEFAULTS, true);

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(serviceDefaultFieldsNames.size()));
    }


    @Xray(test = "PEG-6040", requirement = "PEG-5517")
    @Test
    public void queryWithOnlyCustomFields() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.CUSTOM_FIELDS);
        searchBody.put(QUERY, customFieldNames.get(0));

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(1))
                .body("content.internalName", hasItem(customFieldNames.get(0)));
    }

    @Xray(test = "PEG-6039", requirement = "PEG-5517")
    @Test
    public void paginationWithOnlyCustomFields() {
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : usersAndOrganization.getJSONObject(randomRole.name()).getString("token");

        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.ALL_FIELDS);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 1);
        searchBody.put(CUSTOM, true);

        FieldsHelper.searchFields(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(1))
                .body("content.internalName", hasItem(customFieldNames.get(0)));
    }

    @Xray(test = "PEG-1909", requirement = "PEG-1725")
    @Test
    public void searchForEditedField() {
        final JSONObject organizationWithOwner = new OrganizationFlows().createAndPublishOrganizationWithOwner();
        final String organizationId = organizationWithOwner.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = organizationWithOwner.getJSONObject(OWNER.name()).getString("token");
        final int fieldId = fieldsFlows.createField(organizationId, FieldTypes.TEXT).getInt("id");
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.REQUIRED_FIELDS);
        final String nameToChange = "newName";
        fieldsFlows.editFieldNames(organizationId, fieldId, FieldTypes.TEXT, nameToChange);
        searchBody.put(QUERY, nameToChange);

        FieldsHelper.searchFields(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(1))
                .body("content.internalName", hasItem(nameToChange));
    }

    @Xray(test = "PEG-1910", requirement = "PEG-1725")
    @Test
    public void searchForDeletedField() {
        final JSONObject organizationWithOwner = new OrganizationFlows().createAndPublishOrganizationWithOwner();
        final String organizationId = organizationWithOwner.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = organizationWithOwner.getJSONObject(OWNER.name()).getString("token");
        final int fieldId = fieldsFlows.createField(organizationId, FieldTypes.TEXT).getInt("id");
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.REQUIRED_FIELDS);
        fieldsFlows.deleteField(organizationId, fieldId);
        searchBody.put(QUERY, Integer.toString(fieldId));

        FieldsHelper.searchFields(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(0));
    }

    @Xray(test = "PEG-1911", requirement = "PEG-1725")
    @Test
    public void searchInNonExistingOrganization() {
        final String nonExistingOrgId = getRandomInt() + "IDORG";
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.REQUIRED_FIELDS);

        FieldsHelper.searchFields(SUPPORT_TOKEN, nonExistingOrgId, searchBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));

    }

    @Xray(test = "PEG-1904", requirement = "PEG-1725")
    @Test
    public void searchInUnpublishedOrganization() {
        final JSONObject organizationWithOwner = new OrganizationFlows().createUnpublishedOrganizationWithOwner();
        final String organizationId = organizationWithOwner.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = organizationWithOwner.getJSONObject(OWNER.name()).getString("token");
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.REQUIRED_FIELDS);

        FieldsHelper.searchFields(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(defaultFieldsNames.size()));
    }

    @Xray(test = "PEG-1905", requirement = "PEG-1725")
    @Test
    public void searchInDeletedOrganization() {
        final JSONObject organizationWithOwner = new OrganizationFlows().createAndDeletePublishedOrganization();
        final String organizationId = organizationWithOwner.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject searchBody = fieldsSearchBody.bodyBuilder(FieldsSearchCombination.REQUIRED_FIELDS);

        FieldsHelper.searchFields(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", hasSize(defaultFieldsNames.size()));
    }

}
