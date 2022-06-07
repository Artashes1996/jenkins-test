package helpers.flows;

import configuration.Role;
import helpers.DBHelper;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.appsapi.accountresource.payloads.RestoreApplyBody;
import helpers.appsapi.accountresource.payloads.RestoreRequestBody;
import helpers.appsapi.invitationresource.payloads.InvitationCreationBody;
import helpers.appsapi.usersresource.UserHelper;
import helpers.appsapi.usersresource.payloads.UserSearchBody;
import helpers.appsapi.usersresource.payloads.UserUpdateBody;
import helpers.appsapi.invitationresource.InvitationHelper;
import helpers.appsapi.invitationresource.payloads.InvitationAcceptBody;
import io.restassured.response.Response;
import io.restassured.response.ResponseBodyExtractionOptions;
import lombok.SneakyThrows;
import org.json.*;

import java.util.*;
import java.util.stream.IntStream;

import org.testng.Assert;
import utils.DbWaitUtils;
import utils.commons.ToggleAction;

import static configuration.Role.*;
import static helpers.appsapi.invitationresource.payloads.InvitationCreationBody.*;
import static helpers.appsapi.usersresource.payloads.UserSearchBody.*;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class UserFlows {

    public JSONObject inviteUser(final String organizationId, final Role role, final List<String> locationIds) {
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, role, locationIds);

        final JSONArray invitationResponseArray = new JSONArray(InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());

        final JSONObject invitationResponseBody = invitationResponseArray.getJSONObject(0);
        final String invitationToken = DBHelper.getInvitationToken(invitationResponseBody.getString("email"));
        invitationResponseBody.put("token", invitationToken);
        return invitationResponseBody;
    }

    public JSONObject inviteUserWithoutPOC(final String organizationId, final Role role, final List<String> locationIds) {
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, role, locationIds);

        final JSONArray invitationResponseArray = new JSONArray(InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());

        final JSONObject invitationResponseBody = invitationResponseArray.getJSONObject(0);
        final String userEmail = invitationResponseBody.getString("email");
        final String invitationToken = DBHelper.getInvitationToken(userEmail);
        final String userId = getUserId(userEmail, organizationId);
        invitationResponseBody.put("token", invitationToken);
        invitationResponseBody.put("id", userId);

        return invitationResponseBody;
    }

    public JSONObject createUser(String organizationId, Role role, final List<String> locationIds) {
        final JSONObject invitationResponseBody = inviteUser(organizationId, role, locationIds);
        final String invitationToken = DBHelper.getInvitationToken(invitationResponseBody.getString("email"));
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);
        final JSONObject acceptResponse = new JSONObject(InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
        acceptResponse.getJSONObject("user").put("token", acceptResponse.getString("token"));
        acceptResponse.getJSONObject("user").put("role", role.name());
        acceptResponse.getJSONObject("user").put("workingLocationIds", locationIds);

        return acceptResponse.getJSONObject("user");
    }

    public JSONObject createUserWithoutPOC(String organizationId, Role role, final List<String> locationIds) {
        final JSONObject invitationResponseBody = inviteUserWithoutPOC(organizationId, role, locationIds);
        final String invitationToken = DBHelper.getInvitationToken(invitationResponseBody.getString("email"));
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);
        final JSONObject user = new JSONObject(InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString())
                .getJSONObject("user");
        user.put("role", role.name());
        user.put("workingLocationIds", locationIds);
        return user;
    }

    public JSONObject createOwnerWithOrganizationIdWithoutPOC(String organizationId) {

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, OWNER, null);
        final JSONArray invitationResponseArray = new JSONArray(InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
        final JSONObject invitationResponseBody = invitationResponseArray.getJSONObject(0);
        final String invitationToken = DBHelper.getInvitationToken(invitationResponseBody.getString("email"));

        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);
        final Response response = InvitationHelper.acceptInvite(acceptBody);
        response.then().assertThat().statusCode(SC_CREATED);
        final JSONObject jsonObject = new JSONObject(response.getBody().asString());
        return jsonObject.getJSONObject("user");
    }

    // TODO change flow
    public JSONObject createOwnerWithOrganizationIdWithLocationPOC(String organizationId, String locationId) {
//        final HashMap<String, String> emailToken = inviteUsersWithLocationPOCWithOrganizationId(organizationId, locationId);
        final HashMap<String, String> emailToken = null;
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        if (emailToken != null) {
            for (String token : emailToken.values()) {
                acceptBody.put(InvitationAcceptBody.TOKEN, token);
                final Response response = InvitationHelper.acceptInvite(acceptBody);
                response.then().assertThat().statusCode(SC_CREATED);
                final JSONObject jsonObject = new JSONObject(response.getBody().asString());
                return jsonObject.getJSONObject("user");
            }
        }
        return null;
    }

    @SneakyThrows
    public void inactivateUserById(String organizationId, String userId) {
        final JSONObject updateAccount = new JSONObject(UserHelper.getUserFullDetailsById(SUPPORT_TOKEN, organizationId, userId).then().extract().body().asString());
        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        final String role = updateAccount.getJSONArray(ROLE_LOCATION_PAYLOADS).getJSONObject(0).getString(ROLE_INTERNAL_NAME);
        if (role.equals(LOCATION_ADMIN.name()) || role.equals(STAFF.name())) {
            updateAccount.remove(WORKING_LOCATION_IDS);
        } else if (role.equals(OWNER.name()) || role.equals(ADMIN.name())) {
            updateAccount.getJSONArray(ROLE_LOCATION_PAYLOADS).getJSONObject(0).remove(LOCATION_ID);
        }
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_OK);
        DbWaitUtils.waitForUserStateToBe(() -> DBHelper.getUserStatus(userId), "2");
    }

    public JSONObject changeRoleOfUser(String organizationId, String userId, Role roleToBe) {
        final JSONObject updateAccount = new JSONObject(UserHelper.getUserFullDetailsById(SUPPORT_TOKEN, organizationId, userId).then().extract().body().asString());
        final JSONArray allLocations = updateAccount.getJSONArray(WORKING_LOCATION_IDS);

        final JSONArray roleAndLocations = new JSONArray();
        if (roleToBe.equals(LOCATION_ADMIN) || roleToBe.equals(STAFF)) {
            updateAccount.remove(WORKING_LOCATION_IDS);

            allLocations.forEach(locationId -> {
                final JSONObject roleLocation = new JSONObject();
                roleLocation.put(ROLE_INTERNAL_NAME, roleToBe.name());
                roleLocation.put(LOCATION_ID, locationId);
                roleAndLocations.put(roleLocation);
            });
        } else if (roleToBe.equals(OWNER) || roleToBe.equals(ADMIN)) {
            final JSONObject roleLocation = new JSONObject();
            roleLocation.put(ROLE_INTERNAL_NAME, roleToBe.name());
            updateAccount.put(WORKING_LOCATION_IDS, allLocations);
            roleAndLocations.put(roleLocation);
        }
        updateAccount.put(ROLE_LOCATION_PAYLOADS, roleAndLocations);
        return new JSONObject(UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_OK)
                .extract().body().asString());
    }

    public JSONObject changeRoleAndLocationOfUser(String organizationId, String userId, Role roleToBe, List<String> locationIds) {
        final JSONObject updateAccount = new JSONObject(UserHelper.getUserFullDetailsById(SUPPORT_TOKEN, organizationId, userId).then().extract().body().asString());

        final JSONArray roleAndLocations = new JSONArray();
        if (roleToBe.equals(LOCATION_ADMIN) || roleToBe.equals(STAFF)) {
            updateAccount.remove(WORKING_LOCATION_IDS);

            locationIds.forEach(locationId -> {
                final JSONObject roleLocation = new JSONObject();
                roleLocation.put(ROLE_INTERNAL_NAME, roleToBe.name());
                roleLocation.put(LOCATION_ID, locationId);
                roleAndLocations.put(roleLocation);
            });
        } else if (roleToBe.equals(OWNER) || roleToBe.equals(ADMIN)) {
            final JSONObject roleLocation = new JSONObject();
            roleLocation.put(ROLE_INTERNAL_NAME, roleToBe.name());
            updateAccount.put(WORKING_LOCATION_IDS, locationIds);
            roleAndLocations.put(roleLocation);
        }
        updateAccount.put(ROLE_LOCATION_PAYLOADS, roleAndLocations);
        return new JSONObject(UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_OK)
                .extract().body().asString());
    }

    @SneakyThrows
    public void restoreUser(String organizationId, JSONObject user) {
        requestRestore(organizationId, user);
        final String restoreToken = DBHelper.getRestoreToken(user.getString("id"));
        final JSONObject restoreApplyBody = new JSONObject();
        restoreApplyBody.put(RestoreApplyBody.RESTORE_TOKEN, restoreToken);
        restoreApplyBody.put(RestoreApplyBody.PASSWORD, new Random().nextInt() + "!Qw123456");
        AccountHelper.restoreApply(restoreApplyBody)
                .then()
                .statusCode(SC_OK);
        Thread.sleep(15000);
    }

    @SneakyThrows
    public String requestRestore(String organizationId, JSONObject user) {
        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, user.getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(SC_OK);
        Thread.sleep(20000);
        return DBHelper.getRestoreToken(user.getString("id"));
    }

    @SneakyThrows
    public void deleteUser(String organizationId, String userId) {
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, userId)
                .then()
                .statusCode(SC_NO_CONTENT);
//        TODO this is a novel approach for waiting, if tests with deleted users will fail revisit this
        DbWaitUtils.waitForUserStateToBe(() -> DBHelper.getUserDeletedState(userId), true);
    }

    public void uploadAvatar(String token) {
        final String filePath = "src/test/resources/files/pics/charmander.png";
        UserHelper.uploadUserAvatar(token, filePath)
                .then()
                .statusCode(SC_OK);
    }

    public String getUserId(String email, String organizationId) {
        final JSONObject body = new JSONObject();
        body.put(UserSearchBody.QUERY, email);
        ArrayList<LinkedHashMap<String, Object>> searchResult = UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, body)
                .then()
                .statusCode(SC_OK)
                .extract()
                .path("content");
        Assert.assertFalse(searchResult.isEmpty(), "user with following email: " + email + " is not found");

        return searchResult.get(0).get("id").toString();
    }

    public JSONArray inviteUsers(final String organizationId, final List<String> locationIds, final Role role, final List<String> emails) {
        final JSONObject invitationBody = new JSONObject();
        invitationBody.put(InvitationCreationBody.SEND_EMAIL, true);
        final JSONArray payloads = new JSONArray();
        IntStream.range(0, emails.size()).forEach(i -> {
            final JSONObject payload = new JSONObject();
            final JSONArray roleLocationPayloads = new JSONArray();
            final String email = emails.get(i);
            payload.put(InvitationCreationBody.EMAIL, email);
            if (role.equals(OWNER) || role.equals(ADMIN)) {
                final JSONObject roleLocationPayload = new JSONObject();
                final JSONArray workingLocationIds = new JSONArray();
                locationIds.forEach(workingLocationIds::put);
                roleLocationPayload.put(ROLE_INTERNAL_NAME, role.name());
                roleLocationPayloads.put(roleLocationPayload);
                payload.put(InvitationCreationBody.WORKING_LOCATION_IDS, workingLocationIds);
            } else {
                locationIds.forEach(locationId -> {
                    final JSONObject roleLocationPayload = new JSONObject();
                    roleLocationPayload.put(LOCATION_ID, locationIds);
                    roleLocationPayload.put(ROLE_INTERNAL_NAME, role.name());
                    roleLocationPayloads.put(roleLocationPayload);
                });
            }
            payload.put(InvitationCreationBody.ROLE_LOCATION_PAYLOADS, roleLocationPayloads);

            payloads.put(payload);
        });
        invitationBody.put(InvitationCreationBody.PAYLOADS, payloads);
        ResponseBodyExtractionOptions body = InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .body();
        return new JSONArray().put(body.asString());
    }

    public JSONArray inviteUsers(final String organizationId, final List<String> locationIds, final Role role, final int userCount) {
        final JSONObject invitationBody = new JSONObject();
        invitationBody.put(InvitationCreationBody.SEND_EMAIL, true);
        final JSONArray payloads = new JSONArray();
        IntStream.range(0, userCount).forEach(i -> {
            final JSONObject payload = new JSONObject();
            final JSONArray roleLocationPayloads = new JSONArray();
            if (role.equals(OWNER) || role.equals(ADMIN)) {
                final JSONObject roleLocationPayload = new JSONObject();
                final JSONArray workingLocationIds = new JSONArray();
                locationIds.forEach(workingLocationIds::put);
                roleLocationPayload.put(ROLE_INTERNAL_NAME, role.name());
                roleLocationPayloads.put(roleLocationPayload);
                payload.put(InvitationCreationBody.WORKING_LOCATION_IDS, workingLocationIds);
            } else {
                locationIds.forEach(locationId -> {
                    final JSONObject roleLocationPayload = new JSONObject();
                    roleLocationPayload.put(LOCATION_ID, locationId);
                    roleLocationPayload.put(ROLE_INTERNAL_NAME, role.name());
                    roleLocationPayloads.put(roleLocationPayload);
                });
            }
            payload.put(InvitationCreationBody.ROLE_LOCATION_PAYLOADS, roleLocationPayloads);
            final String email = Math.abs(getRandomInt()) + "@qless.com";
            payload.put(InvitationCreationBody.EMAIL, email);
            payload.put(InvitationCreationBody.ROLE_LOCATION_PAYLOADS, roleLocationPayloads);
            payloads.put(payload);
        });
        invitationBody.put(InvitationCreationBody.PAYLOADS, payloads);
        ResponseBodyExtractionOptions body = InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .body();
        return new JSONArray().put(body.asString());
    }

    public int getNumberOfUsersCount(String organizationId) {
        return UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .extract()
                .path("totalElements");
    }


    public JSONObject createUserAndGetFullResponse(String organizationId, Role role, final List<String> locationIds) {
        final JSONObject invitationResponseBody = inviteUser(organizationId, role, locationIds);
        final String invitationToken = DBHelper.getInvitationToken(invitationResponseBody.getString("email"));
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);
        return new JSONObject(InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }

    public JSONObject userWithActiveInactiveLocations(String organizationId, int activeCount, int inactiveCount) {

        final JSONArray activeInactiveLocations = new LocationFlows().createActiveInactiveLocations(organizationId, activeCount, inactiveCount);
        final List<String> activeInactiveLocationsIds = new ArrayList<>();
        for (int i = 0; i < activeInactiveLocations.length(); i++) {
            activeInactiveLocationsIds.add(activeInactiveLocations.getJSONObject(i).getString("id"));
        }

        final JSONObject userWithLocations = new JSONObject();
        userWithLocations.put("USER_ID", new UserFlows().createUser(organizationId, Role.LOCATION_ADMIN, activeInactiveLocationsIds).getString("id"));
        userWithLocations.put("LOCATIONS_IDS", new JSONArray(activeInactiveLocationsIds));
        userWithLocations.put("LOCATIONS_FULL_DATA", activeInactiveLocations);
        return userWithLocations;
    }

    public void linkUnlinkLocationToUser(String organizationId, String userId, String locationId, ToggleAction option) {
        UserHelper.linkToLocation(SUPPORT_TOKEN, organizationId, userId, locationId, option)
                .then()
                .statusCode(SC_OK);
    }

    public void linkUnlinkLocationsToUser(String organizationId, String userId, List<String> locationIds, ToggleAction option) {
        locationIds.forEach(locationId -> UserHelper.linkToLocation(SUPPORT_TOKEN, organizationId, userId, locationId, option)
                .then()
                .statusCode(SC_OK));
    }

    public void linkUnlinkUserToLocationService(String organizationId, String userId, String locationId, String serviceId, ToggleAction option) {
        UserHelper.linkUnlinkUserToLocationService(SUPPORT_TOKEN, organizationId, userId, locationId, serviceId, option.name())
                .then()
                .statusCode(SC_OK);
    }

    public void linkUnlinkUserToLocationServices(String organizationId, String userId, String locationId, List<String> servicesIds, ToggleAction option) {
        servicesIds.forEach(serviceId -> linkUnlinkUserToLocationService(organizationId, userId, locationId, serviceId, option));
    }

    public JSONObject updateUserName(String organizationId, JSONObject existingUser, String firstname, String lastname) {
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, existingUser.getString("id"));
        updateBody.put(UserUpdateBody.FIRST_NAME, firstname);
        updateBody.put(UserUpdateBody.LAST_NAME, lastname);

        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, existingUser.getString("id"), updateBody)
                .then()
                .statusCode(SC_OK);
        existingUser.put(FIRST_NAME, firstname);
        existingUser.put(LAST_NAME, lastname);

        return existingUser;
    }

    public void setPreferredLocation(String token, String locationId) {
        UserHelper.setPreferredLocation(token, locationId)
                .then()
                .statusCode(SC_OK);
    }

    public List<JSONObject> createInactiveUsers(String organizationId, Role role, String locationId, int usersCount) {
        final ArrayList<JSONObject> users = new ArrayList<>();
        for (int i = 0; i < usersCount; i++) {
            final JSONObject user = createUser(organizationId, role, Collections.singletonList(locationId));
            inactivateUserById(organizationId, user.getString("id"));
            users.add(createUser(organizationId, role, Collections.singletonList(locationId)));
        }
        return users;
    }

    public JSONArray getListOfUsersByLinkedLocationsWithAscendingSorted(String organizationId, List<String> locationIds) {
        final JSONObject body = new JSONObject();
        final JSONObject paginationObject = new JSONObject();
        paginationObject.put(SORT, UserSearchBody.SortingBy.FIRST_NAME.getAscending() + "," +
                UserSearchBody.SortingBy.LAST_NAME.getAscending() + ","
                + UserSearchBody.SortingBy.EMAIL.getAscending());
        body.put(PAGINATION, paginationObject);
        body.put(LOCATION_IDS, locationIds);
        return new JSONObject(UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, body)
                .then()
                .statusCode(SC_OK)
                .extract().asString()).getJSONArray("content");
    }

}
