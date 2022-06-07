package helpers.appsapi.invitationresource.payloads;

import configuration.Role;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

import static utils.TestUtils.*;

public class InvitationCreationBody {

    public final static String PAYLOADS = "payloads";
    public final static String CONTACT_NUMBER = "contactNumber";
    public final static String EMAIL = "email";
    public final static String FIRST_NAME = "firstName";
    public final static String LAST_NAME = "lastName";
    public final static String LOCATION_LEVEL_POINT_OF_CONTACT = "locationLevelPointsOfContact";
    public final static String ORGANIZATION_LEVEL_POINT_OF_CONTACT = "organizationLevelPointsOfContact";
    public final static String ROLE_LOCATION_PAYLOADS = "roleLocationPayloads";
    public final static String LOCATION_ID = "locationId";
    public final static String ROLE_INTERNAL_NAME = "roleInternalName";
    public final static String SEND_EMAIL = "sendEmail";
    public final static String WORKING_LOCATION_IDS = "workingLocationIds";

    public final static String ORGANIZATION_ID = "organizationId";

    private static final String randomCountry = new Locale("", Locale.getISOCountries()[getRandomInt(Locale.getISOCountries().length)]).getDisplayCountry();

    public enum InvitationCreateCombination {
        REQUIRED,
        ALL_FIELDS,
    }

    public enum PointsOfContacts {
        ADMINISTRATIVE,
        TECHNICAL,
        BILLING,
        OTHER
    }

    public static JSONObject bodyBuilder(final InvitationCreateCombination combination, final Role role, final List<String> locationIds) {
        final JSONObject invitationBody = new JSONObject();
        final JSONArray payloads = new JSONArray();
        final JSONObject invitationPayload = new JSONObject();
        final String email = getRandomInt() + "@qless.com";
        final JSONArray roleAndLocations = new JSONArray();

        if (role.equals(Role.ADMIN) || role.equals(Role.OWNER)) {
            final JSONObject roleLocation = new JSONObject();
            roleLocation.put(ROLE_INTERNAL_NAME, role.name());
            roleAndLocations.put(roleLocation);

            if (locationIds != null) {
                final JSONArray workingLocations = new JSONArray();
                locationIds.forEach(workingLocations::put);
                invitationPayload.put(WORKING_LOCATION_IDS, workingLocations);
            }

        } else {
            locationIds.forEach(locationId -> {
                final JSONObject roleLocation = new JSONObject();
                roleLocation.put(ROLE_INTERNAL_NAME, role.name());
                roleLocation.put(LOCATION_ID, locationId);
                roleAndLocations.put(roleLocation);
            });
        }

        invitationPayload.put(ROLE_LOCATION_PAYLOADS, roleAndLocations);
        invitationPayload.put(EMAIL, email);

        switch (combination) {
            case REQUIRED:
                break;
            case ALL_FIELDS: {
                final PointsOfContacts pointsOfContacts = Arrays.asList(PointsOfContacts.values()).get(getRandomInt(PointsOfContacts.values().length));
                invitationPayload.put(CONTACT_NUMBER, "+37477889900");
                invitationPayload.put(FIRST_NAME, FAKER.name().firstName());
                invitationPayload.put(LAST_NAME, FAKER.name().lastName());
                invitationPayload.put(ORGANIZATION_LEVEL_POINT_OF_CONTACT, new JSONArray().put(pointsOfContacts));
                break;
            }
        }

        payloads.put(invitationPayload);
        invitationBody.put(PAYLOADS, payloads);
        invitationBody.put(SEND_EMAIL, true);
        return invitationBody;
    }

}
