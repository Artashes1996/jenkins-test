package helpers.flows;

import helpers.appsapi.locationservicelinksresource.LocationServiceLinksHelper;
import helpers.appsapi.locationservicelinksresource.payloads.LocationServiceLinksBody;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.appsapi.servicesresource.payloads.LinkUnlinkLocationsToServiceRequestBody;
import helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import static helpers.appsapi.locationservicelinksresource.payloads.LocationServiceLinksBody.DESTINATION_GROUP_ID;
import static helpers.appsapi.servicesresource.payloads.ServiceCreationRequestBody.*;
import static org.apache.hc.core5.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.*;

public class ServiceFlows {
    final LocationFlows locationFlows = new LocationFlows();
    final ResourceFlows resourceFlows = new ResourceFlows();
    final UserFlows userFlows = new UserFlows();
    final ServiceUpdateRequestBody serviceUpdateRequestBody = new ServiceUpdateRequestBody();

    public JSONObject createService(String organizationId) {
        final JSONObject serviceCreationBody = bodyBuilder();
        return new JSONObject(ServicesHelper.createService(SUPPORT_TOKEN, organizationId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }

    public JSONObject createInactiveService(String organizationId) {
        final JSONObject serviceCreationBody = bodyBuilder();
        serviceCreationBody.put(STATUS, Status.INACTIVE);
        return new JSONObject(ServicesHelper.createService(SUPPORT_TOKEN, organizationId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }

    public JSONObject createServiceByDuration(String organizationId, int serviceDuration) {
        final JSONObject serviceCreationBody = bodyBuilder();
        serviceCreationBody.put(DURATION, serviceDuration);
        return new JSONObject(ServicesHelper.createService(SUPPORT_TOKEN, organizationId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }

    public JSONArray getFieldLinks(String organizationId, String serviceId) {
        return new JSONObject(ServicesHelper.getServiceById(SUPPORT_TOKEN, organizationId, serviceId)
                .then().statusCode(SC_OK)
                .extract().body().asString()).getJSONArray("fieldLinks");
    }

    public JSONObject createHiddenService(String organizationId) {
        final JSONObject serviceCreationBody = bodyBuilder();
        final JSONObject visibilityBody = serviceCreationBody.getJSONObject(VISIBILITY);
        visibilityBody.put(MONITOR, false);
        visibilityBody.put(PHYSICAL_KIOSK, false);
        visibilityBody.put(WEB_KIOSK, false);
        return new JSONObject(ServicesHelper.createService(SUPPORT_TOKEN, organizationId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }

    public List<String> createServices(String organizationId, int servicesCount) {
        final ArrayList<String> serviceIds = new ArrayList<>();
        for (int i = 0; i < servicesCount; i++) {
            serviceIds.add(createService(organizationId).getString("id"));
        }
        return serviceIds;
    }

    public List<JSONObject> createServices(int servicesCount, String organizationId) {
        final ArrayList<JSONObject> services = new ArrayList<>();
        for (int i = 0; i < servicesCount; i++) {
            services.add(createService(organizationId));
        }
        return services;
    }

    public void inactivateServices(String organizationId, List<String> serviceIds) {
        final JSONObject body = serviceUpdateRequestBody.bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.RANDOM_FIELDS);
        body.put(STATUS, "INACTIVE");
        for (String serviceId : serviceIds) {
            ServicesHelper.updateService(SUPPORT_TOKEN, organizationId, serviceId, body)
                    .then()
                    .statusCode(SC_OK);
        }
    }

    public JSONObject inactivateService(String organizationId, JSONObject service) {
        final JSONObject serviceInactivationBody = serviceUpdateRequestBody.bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.REQUIRED);
        serviceInactivationBody.put(INTERNAL_NAME, service.getString(INTERNAL_NAME));
        serviceInactivationBody.put(DISPLAY_NAME, service.getString(DISPLAY_NAME));
        serviceInactivationBody.put(DURATION, service.getInt(DURATION));
        serviceInactivationBody.put(STATUS, ServiceUpdateRequestBody.Status.INACTIVE.name());
        serviceInactivationBody.put(VISIBILITY, service.getJSONObject(VISIBILITY));
        serviceInactivationBody.put(RESOURCE_SELECTION, service.getString(RESOURCE_SELECTION));
        final String serviceId = service.getString("id");

        return new JSONObject(ServicesHelper.updateService(SUPPORT_TOKEN, organizationId, serviceId, serviceInactivationBody)
                .then()
                .statusCode(SC_OK)
                .extract().body().asString());
    }

    public void linkLocationsToService(String organizationId, String serviceId, List<String> locationIds) {
        final JSONObject linkBody = LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, new JSONArray(locationIds));
        ServicesHelper.linkUnlinkLocationsToService(SUPPORT_TOKEN, organizationId, serviceId, linkBody)
                .then()
                .statusCode(SC_OK);
    }

    public void unlinkLocationsFromService(String organizationId, String serviceId, List<String> locationIds) {
        final JSONObject linkBody = LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(UNLINK, new JSONArray(locationIds));
        ServicesHelper.linkUnlinkLocationsToService(SUPPORT_TOKEN, organizationId, serviceId, linkBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }


    public void linkServiceToGroup(String organizationId, String locationId, String serviceId, String groupId) {
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, groupId);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, serviceId, groupToAttach)
                .then()
                .statusCode(SC_OK);
    }

    public void linkServiceToLocationGroup(String organizationId, String serviceId, String locationId, String groupId) {
        linkLocationsToService(organizationId, serviceId, Collections.singletonList(locationId));
        linkServiceToGroup(organizationId, locationId, serviceId, groupId);
    }

    public void linkSeveralServicesToLocationGroup(List<String> serviceIds, String organizationId, String locationId, String groupId) {
        serviceIds.forEach(serviceId -> linkServiceToLocationGroup(organizationId, serviceId, locationId, groupId));
    }

    public void updateService(String organizationId, String serviceId, JSONObject serviceToUpdate) {
        ServicesHelper.updateService(SUPPORT_TOKEN, organizationId, serviceId, serviceToUpdate)
                .then()
                .statusCode(SC_OK);
    }

    public void linkServiceToUser(String organizationId, String locationId, String serviceId, String userId) {
        userFlows.linkUnlinkUserToLocationService(organizationId, userId, locationId, serviceId, LINK);
    }

    public void linkServiceToLocationAndUser(String organizationId, String locationId, String serviceId, String userId) {
        locationFlows.linkUnlinkServicesToLocation(organizationId, locationId, Collections.singletonList(serviceId), LINK);
        userFlows.linkUnlinkUserToLocationService(organizationId, userId, locationId, serviceId, LINK);
    }

    public void linkServiceToLocationAndResource(String organizationId, String locationId, String serviceId, String resourceId) {
        locationFlows.linkUnlinkServicesToLocation(organizationId, locationId, Collections.singletonList(serviceId), LINK);
        resourceFlows.linkUnlinkServiceToResource(organizationId, locationId, resourceId, serviceId, LINK);
    }

    public void linkServiceToResource(String organizationId, String locationId, String serviceId, String resourceId) {
        resourceFlows.linkUnlinkServiceToResource(organizationId, locationId, resourceId, serviceId, LINK);
    }

    public JSONObject createServiceWithFields(String organizationId, List<JSONObject> fields) {
        final JSONObject serviceCreationBody = bodyBuilder();
        final JSONArray allFieldsToLink = new JSONArray();

        for (int i = 0; i < fields.size(); i++) {
            final JSONObject fieldLinks = new JSONObject();
            fieldLinks.put(DISPLAY_TO, ServiceUpdateRequestBody.DisplayTo.EVERYONE.name());
            fieldLinks.put(FIELD_ID, fields.get(i).getInt("id"));
            fieldLinks.put(OPTIONAL, true);
            fieldLinks.put(ORDER, i + 1);
            allFieldsToLink.put(fieldLinks);
        }
        serviceCreationBody.put(FIELD_LINK_CREATION_REQUEST, allFieldsToLink);
        return new JSONObject(ServicesHelper.createService(SUPPORT_TOKEN, organizationId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }

    public void addFieldsToService(String organizationId, String serviceId, List<JSONObject> fields) {
        final JSONObject serviceUpdateBody = serviceUpdateRequestBody.bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.REQUIRED);
        final JSONArray allFieldsToLink = new JSONArray();

        for (int i = 0; i < fields.size(); i++) {
            final JSONObject fieldLinks = new JSONObject();
            fieldLinks.put(DISPLAY_TO, fields.get(i).getString(DISPLAY_TO));
            fieldLinks.put(FIELD_ID, fields.get(i).getInt("id"));
            fieldLinks.put(OPTIONAL, fields.get(i).getBoolean(OPTIONAL));
            fieldLinks.put(ORDER, i + 1);
            allFieldsToLink.put(fieldLinks);
        }
        serviceUpdateBody.put(ServiceUpdateRequestBody.FIELD_LINK_CREATION_REQUESTS, allFieldsToLink);
        ServicesHelper.updateService(SUPPORT_TOKEN, organizationId, serviceId, serviceUpdateBody)
                .then()
                .statusCode(SC_OK);
    }

    public boolean isServiceVisible(JSONObject service) {
        return service.getJSONObject("visibility").getBoolean("monitor") ||
                service.getJSONObject("visibility").getBoolean("physicalKiosk") ||
                service.getJSONObject("visibility").getBoolean("webKiosk");
    }

    public String getServiceDurationInMinutes(JSONObject service) {
        return String.valueOf((int) Math.ceil(service.getFloat("duration") / 60));
    }

    public JSONObject getServiceById(String organizationId, String serviceId) {
        return new JSONObject(ServicesHelper
                .getServiceById(SUPPORT_TOKEN, organizationId, serviceId)
                .then()
                .statusCode(SC_OK).extract().body().asString());
    }

}
