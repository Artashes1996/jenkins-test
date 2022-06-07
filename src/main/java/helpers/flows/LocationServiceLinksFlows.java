package helpers.flows;

import helpers.appsapi.locationservicelinksresource.LocationServiceLinksHelper;
import helpers.appsapi.locationservicelinksresource.payloads.LocationServiceLinksBody;
import org.json.JSONObject;

import static helpers.appsapi.locationservicelinksresource.payloads.LocationServiceLinksBody.*;
import static org.apache.http.HttpStatus.SC_OK;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class LocationServiceLinksFlows {

    public JSONObject linkToGroup(String organizationId, String locationId, String groupId, JSONObject service) {
        final JSONObject linkRequestBody = new LocationServiceLinksBody().bodyBuilder();
        linkRequestBody.put(DESTINATION_GROUP_ID, groupId);
        linkRequestBody.put(DURATION, service.getString(DURATION));
        linkRequestBody.put(RESOURCE_SELECTION, service.getString(RESOURCE_SELECTION));
        linkRequestBody.put(VISIBILITY, service.getString(VISIBILITY));

        return new JSONObject(LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId, service, linkRequestBody)
                .then()
                .statusCode(SC_OK)
                .extract().body().asString());
    }
}
