package e2e.gatewayapps.locationservicelinksresources;

import configuration.Role;
import e2e.gatewayapps.locationservicelinksresources.data.LocationServiceDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationservicelinksresource.LocationServiceLinksHelper;
import helpers.appsapi.locationservicelinksresource.payloads.LocationServiceLinksBody;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static configuration.Role.*;
import static helpers.appsapi.locationservicelinksresource.payloads.LocationServiceLinksBody.*;
import static helpers.appsapi.servicesresource.payloads.ServiceCreationRequestBody.ResourceSelection.*;
import static helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody.INTERNAL_NAME;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class LocationServiceEditTest {

    private OrganizationFlows organizationFlows;

    private JSONObject organizationWithUsers;
    private LocationFlows locationFlows;
    private ServiceFlows serviceFlows;

    private String organizationId;
    private String locationId;
    private String groupId;

    private final ThreadLocal<JSONObject> serviceThread = new ThreadLocal<>();
    private final ThreadLocal<JSONObject> updateBodyThread = new ThreadLocal<>();
    private final ThreadLocal<String> pathTemplateThread = new ThreadLocal<>();

    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        serviceFlows = new ServiceFlows();

        organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationWithUsers.getJSONObject("LOCATION").getString("id");

        final JSONObject group = new LocationServiceGroupFlows().createGroup(organizationId, locationId, null);
        groupId = group.getString("id");
    }

    @BeforeMethod
    public void dataPreparation() {
        serviceThread.set(serviceFlows.createService(organizationId));
        serviceFlows.linkLocationsToService(organizationId, serviceThread.get().getString("id"), Collections.singletonList(locationId));
        updateBodyThread.set(new LocationServiceLinksBody().bodyBuilder());
        updateBodyThread.get().put(DURATION, serviceThread.get().getInt(DURATION));
        updateBodyThread.get().put(RESOURCE_SELECTION, serviceThread.get().getString(RESOURCE_SELECTION));
        updateBodyThread.get().put(VISIBILITY, new JSONObject(serviceThread.get().getJSONObject(VISIBILITY).toString()));
        pathTemplateThread.set("children.find{it.name=='" + serviceThread.get().getString(INTERNAL_NAME) + "'}.%s");
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4626")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void changeResource(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        updateBodyThread.get().put(RESOURCE_SELECTION, REQUIRED);

        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_OK)
                .body("resourceSelection", equalTo(REQUIRED.name()))
                .body(matchesJsonSchemaInClasspath("schemas/updateLocationService.json"));

        ServicesHelper.getServiceById(token, organizationId, serviceThread.get().getString("id"))
                .then()
                .statusCode(SC_OK)
                .body("resourceSelection", equalTo(ALLOWED.name()));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4627")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void changeDuration(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final int serviceDurationNewValue = getRandomInt(600) + 300;
        updateBodyThread.get().put(DURATION, serviceDurationNewValue);

        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_OK)
                .body("duration", equalTo(serviceDurationNewValue))
                .body(matchesJsonSchemaInClasspath("schemas/updateLocationService.json"));

        ServicesHelper.getServiceById(token, organizationId, serviceThread.get().getString("id"))
                .then()
                .statusCode(SC_OK)
                .body("duration", equalTo(serviceThread.get().getInt("duration")));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4628")
    @Test(dataProviderClass = LocationServiceDataProvider.class, dataProvider = "validVisibilityFields")
    public void changeVisibility(String visibilityField) {
        updateBodyThread.get().getJSONObject(VISIBILITY).put(visibilityField, false);

        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_OK)
                .body("visibility." + visibilityField, equalTo(false))
                .body(matchesJsonSchemaInClasspath("schemas/updateLocationService.json"));

        ServicesHelper.getServiceById(SUPPORT_TOKEN, organizationId, serviceThread.get().getString("id"))
                .then()
                .statusCode(SC_OK)
                .body("visibility." + visibilityField, equalTo(serviceThread.get().getJSONObject("visibility").getBoolean(visibilityField)));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4629")
    @Test
    public void changeResourceLocationAndOrganizationLevel() {
        updateBodyThread.get().put(RESOURCE_SELECTION, DISABLED.name());
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_OK);

        final JSONObject organizationLevelUpdateBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.RANDOM_FIELDS);
        organizationLevelUpdateBody.put(RESOURCE_SELECTION, REQUIRED.name());
        serviceFlows.updateService(organizationId, serviceThread.get().getString("id"), organizationLevelUpdateBody);

        final JSONObject actualOrgLevelVisibility = new JSONObject((Map<?, ?>) ServicesHelper.getServiceById(SUPPORT_TOKEN, organizationId, serviceThread.get().getString("id"))
                .then()
                .body("resourceSelection", is(organizationLevelUpdateBody.getString(RESOURCE_SELECTION)))
                .body("duration", is(organizationLevelUpdateBody.getInt(DURATION)))
                .extract()
                .path("visibility"));
        Assert.assertTrue(actualOrgLevelVisibility.similar(organizationLevelUpdateBody.getJSONObject(VISIBILITY)));

        final JSONObject actualLocationLevelVisibility = new JSONObject((Map<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(DISABLED.name()))
                .body(String.format(pathTemplateThread.get(), "duration"), is(organizationLevelUpdateBody.getInt(DURATION)))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(organizationLevelUpdateBody.getJSONObject(VISIBILITY)));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4630")
    @Test
    public void changeDurationLocationAndOrganizationLevel() {
        final int serviceDurationNewValue = getRandomInt(600) + 300;
        updateBodyThread.get().put(DURATION, serviceDurationNewValue);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_OK);

        final JSONObject organizationLevelUpdateBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.RANDOM_FIELDS);
        serviceFlows.updateService(organizationId, serviceThread.get().getString("id"), organizationLevelUpdateBody);

        final JSONObject actualOrgLevelVisibility = new JSONObject((HashMap<?, ?>) ServicesHelper.getServiceById(SUPPORT_TOKEN, organizationId, serviceThread.get().getString("id"))
                .then()
                .body("resourceSelection", is(organizationLevelUpdateBody.getString(RESOURCE_SELECTION)))
                .body("duration", is(organizationLevelUpdateBody.getInt(DURATION)))
                .extract()
                .path("visibility"));

        Assert.assertTrue(actualOrgLevelVisibility.similar(organizationLevelUpdateBody.getJSONObject(VISIBILITY)));

        final JSONObject actualLocationLevelVisibility = new JSONObject((HashMap<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(organizationLevelUpdateBody.getString(RESOURCE_SELECTION)))
                .body(String.format(pathTemplateThread.get(), "duration"), is(serviceDurationNewValue))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(organizationLevelUpdateBody.getJSONObject(VISIBILITY)));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4631")
    @Test
    public void changeVisibilityLocationAndOrganizationLevel() {
        final JSONObject visibilityLocationLevel = new JSONObject();
        visibilityLocationLevel.put(MONITOR, true);
        visibilityLocationLevel.put(WEB_KIOSK, false);
        visibilityLocationLevel.put(PHYSICAL_KIOSK, false);
        final JSONObject visibilityOrganizationLevel = new JSONObject();
        visibilityOrganizationLevel.put(MONITOR, false);
        visibilityOrganizationLevel.put(WEB_KIOSK, false);
        visibilityOrganizationLevel.put(PHYSICAL_KIOSK, true);

        updateBodyThread.get().put(VISIBILITY, visibilityLocationLevel);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_OK);

        final JSONObject organizationLevelUpdateBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.RANDOM_FIELDS);
        organizationLevelUpdateBody.put(VISIBILITY, visibilityOrganizationLevel);
        serviceFlows.updateService(organizationId, serviceThread.get().getString("id"), organizationLevelUpdateBody);
        final JSONObject actualOrgLevelVisibility = new JSONObject((HashMap<?, ?>) ServicesHelper.getServiceById(SUPPORT_TOKEN, organizationId, serviceThread.get().getString("id"))
                .then()
                .body("resourceSelection", is(organizationLevelUpdateBody.getString(RESOURCE_SELECTION)))
                .body("duration", is(organizationLevelUpdateBody.getInt(DURATION)))
                .extract()
                .path("visibility"));

        Assert.assertTrue(actualOrgLevelVisibility.similar(visibilityOrganizationLevel));

        final JSONObject actualLocationLevelVisibility = new JSONObject((HashMap<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(organizationLevelUpdateBody.getString(RESOURCE_SELECTION)))
                .body(String.format(pathTemplateThread.get(), "duration"), is(organizationLevelUpdateBody.getInt(DURATION)))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(visibilityLocationLevel));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4632")
    @Test
    public void changeAllWithSameValuesLocationLevel() {
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_OK);

        final JSONObject organizationLevelUpdateBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.RANDOM_FIELDS);
        serviceFlows.updateService(organizationId, serviceThread.get().getString("id"), organizationLevelUpdateBody);

        final JSONObject actualLocationLevelVisibility = new JSONObject((HashMap<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(organizationLevelUpdateBody.getString(RESOURCE_SELECTION)))
                .body(String.format(pathTemplateThread.get(), "duration"), is(organizationLevelUpdateBody.getInt(DURATION)))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(organizationLevelUpdateBody.getJSONObject(VISIBILITY)));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4633")
    @Test
    public void updateAndChangeBackToOrganizationLevelValues() {
        final JSONObject newUpdateBody = new JSONObject(updateBodyThread.get().toString());
        newUpdateBody.put(DURATION, 600);
        newUpdateBody.put(RESOURCE_SELECTION, DISABLED);
        newUpdateBody.getJSONObject(VISIBILITY).put(MONITOR, false);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), newUpdateBody)
                .then()
                .statusCode(SC_OK);

        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_OK);

        final JSONObject organizationLevelUpdateBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.RANDOM_FIELDS);
        serviceFlows.updateService(organizationId, serviceThread.get().getString("id"), organizationLevelUpdateBody);

        final JSONObject actualLocationLevelVisibility = new JSONObject((HashMap<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(updateBodyThread.get().getString(RESOURCE_SELECTION)))
                .body(String.format(pathTemplateThread.get(), "duration"), is(updateBodyThread.get().getInt(DURATION)))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(updateBodyThread.get().getJSONObject(VISIBILITY)));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4634")
    @Test
    public void changeDurationInvalidValueLocationLevel() {
        final int serviceDurationInvalidValue = 10;
        final String serviceDurationInvalidType = "ten";
        updateBodyThread.get().put(DURATION, serviceDurationInvalidValue);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types", hasItem("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
        updateBodyThread.get().put(DURATION, serviceDurationInvalidType);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types", hasItem("NOT_READABLE_REQUEST_BODY"));

        final JSONObject organizationLevelUpdateBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.RANDOM_FIELDS);
        serviceFlows.updateService(organizationId, serviceThread.get().getString("id"), organizationLevelUpdateBody);

        final JSONObject actualLocationLevelVisibility = new JSONObject((HashMap<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .statusCode(SC_OK)
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(organizationLevelUpdateBody.getString(RESOURCE_SELECTION)))
                .body(String.format(pathTemplateThread.get(), "duration"), is(organizationLevelUpdateBody.getInt(DURATION)))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(organizationLevelUpdateBody.getJSONObject(VISIBILITY)));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4635")
    @Test
    public void changeResourceInvalidValueLocationLevel() {
        final String serviceResourceInvalidValue = "ResourceR";
        final int serviceResourceInvalidType = 12;
        updateBodyThread.get().put(DURATION, serviceResourceInvalidValue);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types", hasItem("NOT_READABLE_REQUEST_BODY"));
        updateBodyThread.get().put(DURATION, serviceResourceInvalidType);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types", hasItem("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));

        final JSONObject organizationLevelUpdateBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.RANDOM_FIELDS);
        serviceFlows.updateService(organizationId, serviceThread.get().getString("id"), organizationLevelUpdateBody);

        final JSONObject actualLocationLevelVisibility = new JSONObject((HashMap<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .statusCode(SC_OK)
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(organizationLevelUpdateBody.getString(RESOURCE_SELECTION)))
                .body(String.format(pathTemplateThread.get(), "duration"), is(organizationLevelUpdateBody.getInt(DURATION)))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(organizationLevelUpdateBody.getJSONObject(VISIBILITY)));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4636")
    @Test
    public void changeVisibilityInvalidValueLocationLevel() {
        final String serviceVisibilityInvalidValue = "visibility";
        updateBodyThread.get().getJSONObject(VISIBILITY).put(MONITOR, serviceVisibilityInvalidValue);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types", hasItem("NOT_READABLE_REQUEST_BODY"));

        final JSONObject organizationLevelUpdateBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.RANDOM_FIELDS);
        serviceFlows.updateService(organizationId, serviceThread.get().getString("id"), organizationLevelUpdateBody);

        final JSONObject actualLocationLevelVisibility = new JSONObject((HashMap<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .statusCode(SC_OK)
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(organizationLevelUpdateBody.getString(RESOURCE_SELECTION)))
                .body(String.format(pathTemplateThread.get(), "duration"), is(organizationLevelUpdateBody.getInt(DURATION)))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(organizationLevelUpdateBody.getJSONObject(VISIBILITY)));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4637")
    @Test
    public void changeLocationLevelServiceByStaff() {
        LocationServiceLinksHelper.linkServiceToGroup(organizationWithUsers.getJSONObject(STAFF.name()).getString("token"),
                organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4638")
    @Test
    public void changeOtherLocationLevelServiceByLocationAdmin() {
        final String otherLocationId = locationFlows.createLocation(organizationId).getString("id");
        serviceFlows.linkLocationsToService(organizationId, serviceThread.get().getString("id"), Collections.singletonList(otherLocationId));

        LocationServiceLinksHelper.linkServiceToGroup(organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token"),
                organizationId, otherLocationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4640")
    @Test
    public void changeOtherOrganizationLocationLevelService() {
        final JSONObject otherOrganization = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String locationId = otherOrganization.getJSONObject("LOCATION").getString("id");
        LocationServiceLinksHelper.linkServiceToGroup(otherOrganization.getJSONObject(LOCATION_ADMIN.name()).getString("token"),
                organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_FORBIDDEN);
        LocationServiceLinksHelper.linkServiceToGroup(otherOrganization.getJSONObject(OWNER.name()).getString("token"),
                organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_FORBIDDEN);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4641")
    @Test
    public void changeDeletedOrganizationLocationLevelService() {
        final JSONObject otherOrganization = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String otherOrganizationId = otherOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = otherOrganization.getJSONObject("LOCATION").getString("id");
        final JSONObject service = serviceFlows.createService(otherOrganizationId);

        serviceFlows.linkLocationsToService(otherOrganizationId, service.getString("id"), Collections.singletonList(locationId));
        organizationFlows.deleteOrganization(otherOrganizationId);
        LocationServiceLinksHelper.linkServiceToGroup(otherOrganization.getJSONObject(Role.getRandomOrganizationAdminRole().name()).getString("token"),
                otherOrganizationId, locationId, service.getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_UNAUTHORIZED);

        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, otherOrganizationId, locationId, service.getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4642")
    @Test
    public void updateUnlinkLinkServiceAndCheckValues() {
        updateBodyThread.get().put(DURATION, 600);
        updateBodyThread.get().put(RESOURCE_SELECTION, REQUIRED);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_OK);
        serviceFlows.unlinkLocationsFromService(organizationId, serviceThread.get().getString("id"), Collections.singletonList(locationId));
        serviceFlows.linkLocationsToService(organizationId, serviceThread.get().getString("id"), Collections.singletonList(locationId));

        JSONObject actualLocationLevelVisibility = new JSONObject((HashMap<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .statusCode(SC_OK)
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(serviceThread.get().getString(RESOURCE_SELECTION)))
                .body(String.format(pathTemplateThread.get(), "duration"), is(serviceThread.get().getInt(DURATION)))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(serviceThread.get().getJSONObject(VISIBILITY)));

        final JSONObject organizationLevelUpdateBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.RANDOM_FIELDS);
        serviceFlows.updateService(organizationId, serviceThread.get().getString("id"), organizationLevelUpdateBody);

        actualLocationLevelVisibility = new JSONObject((HashMap<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(organizationLevelUpdateBody.getString(RESOURCE_SELECTION)))
                .body(String.format(pathTemplateThread.get(), "duration"), is(organizationLevelUpdateBody.getInt(DURATION)))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(organizationLevelUpdateBody.getJSONObject(VISIBILITY)));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4643")
    @Test(dataProviderClass = LocationServiceDataProvider.class, dataProvider = "validVisibilityFields")
    public void setNullValues(String field) {
        updateBodyThread.get().getJSONObject("visibility").put(field, JSONObject.NULL);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4644")
    @Test
    public void changeUnlinkedService() {
        final String serviceId = serviceFlows.createService(organizationId).getString("id");
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceId, updateBodyThread.get())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4645")
    @Test
    public void changeInDifferentLocations() {
        final String otherLocationId = locationFlows.createLocation(organizationId).getString("id");
        serviceFlows.linkLocationsToService(organizationId, serviceThread.get().getString("id"), Collections.singletonList(otherLocationId));
        updateBodyThread.set(new LocationServiceLinksBody().bodyBuilder());

        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_OK);

        JSONObject actualLocationLevelVisibility = new JSONObject((HashMap<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .statusCode(SC_OK)
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(updateBodyThread.get().get(RESOURCE_SELECTION)))
                .body(String.format(pathTemplateThread.get(), "duration"), is(updateBodyThread.get().getInt(DURATION)))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(updateBodyThread.get().getJSONObject(VISIBILITY)));

        actualLocationLevelVisibility = new JSONObject((HashMap<?, ?>) LocationServiceLinksHelper.searchServicesInGroups(SUPPORT_TOKEN, organizationId, otherLocationId)
                .then()
                .statusCode(SC_OK)
                .body(String.format(pathTemplateThread.get(), "resourceSelection"), is(serviceThread.get().getString(RESOURCE_SELECTION)))
                .body(String.format(pathTemplateThread.get(), "duration"), is(serviceThread.get().getInt(DURATION)))
                .extract()
                .path(String.format(pathTemplateThread.get(), "visibility")));
        Assert.assertTrue(actualLocationLevelVisibility.similar(serviceThread.get().getJSONObject(VISIBILITY)));
    }

    @Xray(requirement = "PEG-2967", test = "PEG-4646")
    @Test
    public void editAndMoveToGroup() {
        updateBodyThread.set(new LocationServiceLinksBody().bodyBuilder());
        updateBodyThread.get().put(DESTINATION_GROUP_ID, groupId);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceThread.get().getString("id"), updateBodyThread.get())
                .then()
                .statusCode(SC_OK);
    }
}
