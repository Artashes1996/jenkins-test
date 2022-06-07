package helpers.appsapi.usersresource.payloads;

import configuration.Role;
import helpers.appsapi.invitationresource.payloads.InvitationCreationBody;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.AuthenticationFlowHelper;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static configuration.Role.*;
import static configuration.Role.ADMIN;
import static helpers.appsapi.invitationresource.payloads.InvitationCreationBody.*;

public class UserUpdateBody {

    public final static String CONTACT_NUMBER = "contactNumber";
    public final static String FIRST_NAME = "firstName";
    public final static String LAST_NAME = "lastName";
    public final static String LOCATION_LEVEL_POINTS_OF_CONTACT = "locationLevelPointsOfContact";
    public final static String ORGANIZATION_LEVEL_POINTS_OF_CONTACT = "organizationLevelPointsOfContact";
    public final static String TITLE = "title";
    public final static String USER_STATUS = "userStatus";

    public enum PointsOfContacts {
        ADMINISTRATIVE,
        TECHNICAL,
        BILLING,
        OTHER
    }

    //TODO change body builder to REQUIRED/AFF_FIELDS, remove unnecessary fields
    public static JSONObject bodyBuilder(String organizationId, String userId) {
        final InvitationCreationBody.PointsOfContacts pointsOfContact = Arrays.asList(InvitationCreationBody.PointsOfContacts.values()).get(new Random().nextInt(InvitationCreationBody.PointsOfContacts.values().length));
        final JSONObject updateAccount = new JSONObject(UserHelper.getUserFullDetailsById(new AuthenticationFlowHelper().getToken(Role.SUPPORT), organizationId, userId)
                .then()
                .extract().body().asString());

        final String role = updateAccount.getJSONArray("roleLocationPayloads").getJSONObject(0).getString("roleInternalName");
        if (role.equals(LOCATION_ADMIN.name()) || role.equals(STAFF.name())) {
            updateAccount.remove(WORKING_LOCATION_IDS);
        }
        else if(role.equals(OWNER.name()) || role.equals(ADMIN.name())){
            updateAccount.getJSONArray(ROLE_LOCATION_PAYLOADS).getJSONObject(0).remove(LOCATION_ID);
        }

        updateAccount.put(CONTACT_NUMBER, "+37488923126");
        updateAccount.put(FIRST_NAME, "Jane");
        updateAccount.put(LAST_NAME, "DOE");
        updateAccount.put(USER_STATUS, "ACTIVE");
        updateAccount.put(ORGANIZATION_LEVEL_POINTS_OF_CONTACT, Collections.singletonList(pointsOfContact));
        updateAccount.put(TITLE, new Random().nextInt() + " Random Title");

        return updateAccount;
    }
}
