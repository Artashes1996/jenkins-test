package helpers.flows;

import helpers.appsapi.locationservicegroupsresource.LocationServiceGroupsHelper;
import helpers.appsapi.locationservicegroupsresource.payloads.GroupCreationBody;
import helpers.appsapi.locationservicegroupsresource.payloads.GroupUpdateBody;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static helpers.appsapi.locationservicegroupsresource.payloads.GroupCreationBody.PARENT_ID;
import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class LocationServiceGroupFlows {

    public JSONObject createGroup(String organizationId, String locationId, String parentId) {
        final JSONObject creationBody = GroupCreationBody.bodyBuilder();
        creationBody.put(PARENT_ID, parentId);
        return new JSONObject(LocationServiceGroupsHelper.createGroup(SUPPORT_TOKEN, organizationId, locationId, creationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }

    public JSONObject createGroup(String organizationId, String locationId, String parentId, int level) {

        final JSONObject groupHierarchy = createGroup(organizationId,locationId,parentId);
        for(int i=1; i<level; i++) {
            groupHierarchy.put("CHILD", createGroup(organizationId, locationId, parentId));
            parentId = groupHierarchy.getString("id");
        }

       return groupHierarchy;
    }

    public JSONObject updateGroup(String organizationId, String locationId, String serviceGroupId) {
        final JSONObject updateBody = GroupUpdateBody.bodyBuilder();
        return new JSONObject(LocationServiceGroupsHelper.updateGroup(SUPPORT_TOKEN,organizationId,
                        locationId, serviceGroupId, updateBody)
                .then()
                .statusCode(SC_OK)
                .extract().body().asString());
    }


    public List<Map<String, Object>> getGroupsAndServicesInsideGroup(List<Map<String, Object>> group, String groupIdToFilter) {
        return group.stream()
                .filter(obj -> obj.get("id").equals(groupIdToFilter))
                .findFirst()
                .map(obj -> (List<Map<String, Object>>) obj.get("children"))
                .orElseGet(ArrayList::new);
    }

    public List<String> getNamesInsideGroup(List<Map<String, Object>> group) {
        return group.stream()
                .map(obj -> (String) obj.get("name"))
                .collect(Collectors.toList());
    }
}
