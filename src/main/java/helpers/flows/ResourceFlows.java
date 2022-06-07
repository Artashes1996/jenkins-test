package helpers.flows;

import helpers.appsapi.resourcesresource.ResourcesHelper;
import helpers.appsapi.resourcesresource.payloads.*;
import io.restassured.response.Response;
import org.json.*;
import utils.commons.ToggleAction;

import java.util.*;

import static helpers.appsapi.resourcesresource.payloads.ResourceCreationBody.STATUS;
import static helpers.appsapi.resourcesresource.payloads.SearchResourceRequestBody.*;
import static helpers.appsapi.resourcesresource.payloads.SearchResourceRequestBody.ResourceSearchCombination.REQUIRED_FIELDS;
import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class ResourceFlows {

    public JSONObject createActiveResource(String organizationId, List<String> locationIds) {
        final JSONObject createResource = new ResourceCreationBody().bodyBuilder(ResourceCreationBody.RESOURCE_CREATION_COMBINATION.REQUIRED, organizationId);
        createResource.put(LOCATION_IDS, locationIds);
        final Response response = ResourcesHelper.createResource(SUPPORT_TOKEN, organizationId, createResource);
        response.then().statusCode(SC_CREATED);
        return new JSONObject(response.getBody().asString());
    }

    public JSONObject createInactiveResource(String organizationId, List<String> locationIds) {
        final JSONObject createResource = new ResourceCreationBody().bodyBuilder(ResourceCreationBody.RESOURCE_CREATION_COMBINATION.REQUIRED, organizationId);
        createResource.put(LOCATION_IDS, locationIds);
        createResource.put(STATUS, "INACTIVE");
        final Response response = ResourcesHelper.createResource(SUPPORT_TOKEN, organizationId, createResource);
        response.then().statusCode(SC_CREATED);
        return new JSONObject(response.getBody().asString());
    }

    public JSONObject inactivateResourceById(String organizationId, String resourceId) {
        final JSONObject inactivateResourceBody = new ResourceUpdateRequestBody().bodyBuilder(ResourceUpdateRequestBody.Status.INACTIVE);
        return new JSONObject(ResourcesHelper.updateResource(SUPPORT_TOKEN, organizationId, resourceId, inactivateResourceBody)
                .then()
                .statusCode(SC_OK)
                .extract().body().asString());
    }

    public void linkUnlinkLocationToResource(String organizationId, String locationId, String resourceId, ToggleAction action) {
        ResourcesHelper.locationToResourceToggleLink(SUPPORT_TOKEN, organizationId, resourceId, locationId, action.name())
                .then()
                .statusCode(SC_OK);
    }

    public void linkUnlinkServiceToResource(String organizationId, String locationId, String resourceId, String serviceId, ToggleAction action) {
        ResourcesHelper.serviceToResourceToggleLink(SUPPORT_TOKEN, organizationId, locationId, resourceId, serviceId, action.name())
                .then()
                .statusCode(SC_OK);
    }

    public void linkUnlinkServicesToResource(String organizationId, String locationId, String resourceId, List<String> serviceIds, ToggleAction action) {
        for (String serviceId : serviceIds) {
            linkUnlinkServiceToResource(organizationId, locationId, resourceId, serviceId, action);
        }
    }

    public String getResourceIdFromUserId(String organizationId, String userId) {
        final JSONObject searchBody = new SearchResourceRequestBody().bodyBuilder(REQUIRED_FIELDS);
        searchBody.put(QUERY, userId);

        return ResourcesHelper.searchAndFilterResources(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .extract()
                .path("content.id[0]");
    }




}
