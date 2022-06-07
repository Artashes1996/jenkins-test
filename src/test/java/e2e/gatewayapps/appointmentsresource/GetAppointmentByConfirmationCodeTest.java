package e2e.gatewayapps.appointmentsresource;

import e2e.gatewayapps.BaseTest;
import helpers.appsapi.appointmentsresource.AppointmentsHelper;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Collections;
import java.util.UUID;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;

public class GetAppointmentByConfirmationCodeTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private AppointmentsFlow appointmentsFlow;
    private LocationFlows locationFlows;
    private ServiceFlows serviceFlows;
    private UserFlows userFlows;
    private ResourceFlows resourceFlows;
    private String organizationId;
    private String locationId;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        appointmentsFlow = new AppointmentsFlow();
        locationFlows = new LocationFlows();
        serviceFlows = new ServiceFlows();
        userFlows = new UserFlows();
        resourceFlows = new ResourceFlows();

        final JSONObject organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationWithUsers.getJSONObject("LOCATION").getString("id");
    }

    @Xray(requirement = "PEG-6021", test = "PEG-6547")
    @Test
    public void getAppointment() {

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);

        AppointmentsHelper.getByConfirmationCode(organizationId, locationId, appointment.getString("confirmationCode"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentByConfirmationCode.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6021", test = "PEG-6549")
    @Test
    public void getAppointmentWithoutConfirmationCode() {
        AppointmentsHelper.getByConfirmationCode(organizationId, locationId, "")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("types[0]", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(requirement = "PEG-6021", test = "PEG-6553")
    @Test
    public void getAppointmentWithInvalidLocationId() {

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);

        final String invalidLocationId = UUID.randomUUID().toString();

        AppointmentsHelper.getByConfirmationCode(organizationId, invalidLocationId, appointment.getString("confirmationCode"))
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("types[0]", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-6021", test = "PEG-6555")
    @Test
    public void getAppointmentWithInvalidOrganizationId() {

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);

        final String invalidOrgId = UUID.randomUUID().toString();

        AppointmentsHelper.getByConfirmationCode(invalidOrgId, locationId, appointment.getString("confirmationCode"))
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("types[0]", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-6021", test = "PEG-6603")
    @Test
    public void getAppointmentWithBlockOrganizationId() {
        final String organizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        organizationFlows.blockOrganization(organizationId);

        AppointmentsHelper.getByConfirmationCode(organizationId, locationId, appointment.getString("confirmationCode"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentByConfirmationCode.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6021", test = "PEG-6637")
    @Test
    public void getAppointmentWithPausedOrganizationId() {
        final String organizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        organizationFlows.pauseOrganization(organizationId);

        AppointmentsHelper.getByConfirmationCode(organizationId, locationId, appointment.getString("confirmationCode"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentByConfirmationCode.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6021", test = "PEG-6638")
    @Test
    public void getAppointmentWithDeleteOrganizationId() {
        final String organizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        organizationFlows.deleteOrganization(organizationId);

        AppointmentsHelper.getByConfirmationCode(organizationId, locationId, appointment.getString("confirmationCode"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentByConfirmationCode.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6021", test = "PEG-6639")
    @Test
    public void getAppointmentInactiveResource() {
        final String organizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        resourceFlows.inactivateResourceById(organizationId, resourceId);

        AppointmentsHelper.getByConfirmationCode(organizationId, locationId, appointment.getString("confirmationCode"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentByConfirmationCode.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6021", test = "6642")
    @Test
    public void getAppointmentDeletedResource() {
        final String organizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final JSONObject service = serviceFlows.createService(organizationId);
        final JSONObject appointment = appointmentsFlow.createAppointmentWithUser(organizationId, locationId, newUser.getString("id"), service);

        userFlows.deleteUser(organizationId, newUser.getString("id"));

        AppointmentsHelper.getByConfirmationCode(organizationId, locationId, appointment.getString("confirmationCode"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentByConfirmationCode.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6021", test = "PEG-6640")
    @Test
    public void getAppointmentInactiveService() {
        final String organizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        serviceFlows.inactivateService(organizationId, service);

        AppointmentsHelper.getByConfirmationCode(organizationId, locationId, appointment.getString("confirmationCode"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentByConfirmationCode.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6021", test = "PEG-6641")
    @Test
    public void getAppointmentWithInactiveLocation() {
        final String organizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final JSONObject location = locationFlows.createLocation(organizationId);
        final String locationId = location.getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        locationFlows.inactivateLocation(organizationId, locationId);

        AppointmentsHelper.getByConfirmationCode(organizationId, locationId, appointment.getString("confirmationCode"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentByConfirmationCode.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }
}
