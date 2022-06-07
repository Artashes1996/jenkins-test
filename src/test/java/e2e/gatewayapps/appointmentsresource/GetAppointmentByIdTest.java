package e2e.gatewayapps.appointmentsresource;


import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
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

public class GetAppointmentByIdTest {

    private OrganizationFlows organizationFlows;
    private AppointmentsFlow appointmentsFlow;
    private LocationFlows locationFlows;
    private ServiceFlows serviceFlows;
    private String supportToken;
    private String locationId;
    private String organizationId;
    private String resourceId;
    private JSONObject appointment;
    private JSONObject organizationAndUsers;
    private ResourceFlows resourceFlows;


    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        appointmentsFlow = new AppointmentsFlow();
        locationFlows = new LocationFlows();
        serviceFlows = new ServiceFlows();
        resourceFlows = new ResourceFlows();

        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);
        appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        supportToken = new AuthenticationFlowHelper().getToken(SUPPORT);
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6694")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void getAppointment(Role role) {

        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");

        AppointmentsHelper.getByAppointmentId(userToken, organizationId, locationId, appointment.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentById.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6695")
    @Test
    public void getAppointmentWithoutId() {

        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");

        AppointmentsHelper.getByAppointmentId(userToken, organizationId, locationId, "")
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("error", is("Not Found"));
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6760")
    @Test
    public void getAppointmentWithInvalidId() {

        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final String invalidId = UUID.randomUUID().toString();

        AppointmentsHelper.getByAppointmentId(userToken, organizationId, locationId, invalidId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("types[0]", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6696")
    @Test
    public void getAppointmentByIdWithInvalidLocationId() {

        final Role role = getRandomRolesWithLocation();
        final String userToken = organizationAndUsers.getJSONObject(role.name()).getString("token");

        final String invalidLocationId = UUID.randomUUID().toString();

        AppointmentsHelper.getByAppointmentId(userToken, organizationId, invalidLocationId, appointment.getString("id"))
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("types[0]", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6697")
    @Test
    public void getAppointmentByIdWithInvalidLocationIdWithAdminRoles() {

        final Role role = getRandomAdminRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final String invalidLocationId = UUID.randomUUID().toString();

        AppointmentsHelper.getByAppointmentId(userToken, organizationId, invalidLocationId, appointment.getString("id"))
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("types[0]", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6698")
    @Test
    public void getAppointmentByIdWithInvalidOrganizationId() {

        final Role role = getRandomOrganizationRole();
        final String userToken = organizationAndUsers.getJSONObject(role.name()).getString("token");

        final String invalidOrgId = UUID.randomUUID().toString();

        AppointmentsHelper.getByAppointmentId(userToken, invalidOrgId, locationId, appointment.getString("id"))
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("types[0]", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6699")
    @Test
    public void getAppointmentByIdWithBlockOrganizationId() {

        final JSONObject organizationAndUsers= organizationFlows.createAndPublishOrganizationWithAllUsers();
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        organizationFlows.blockOrganization(organizationId);

        AppointmentsHelper.getByAppointmentId(userToken, organizationId, locationId, appointment.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentById.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6700")
    @Test
    public void getAppointmentByIdWithPausedOrganization() {

        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final Role role = getRandomOrganizationRole();
        final String userToken = organizationAndUsers.getJSONObject(role.name()).getString("token");

        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        organizationFlows.pauseOrganization(organizationId);

        AppointmentsHelper.getByAppointmentId(userToken, organizationId, locationId, appointment.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentById.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6701")
    @Test
    public void getAppointmentByIdWithDeleteOrganization() {

        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();

        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        organizationFlows.deleteOrganization(organizationId);

        AppointmentsHelper.getByAppointmentId(supportToken, organizationId, locationId, appointment.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentById.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6702")
    @Test()
    public void getAppointmentByIdInactiveResource() {

        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");

        resourceFlows.inactivateResourceById(organizationId, resourceId);

        AppointmentsHelper.getByAppointmentId(userToken, organizationId, locationId, appointment.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentById.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6703")
    @Test
    public void getAppointmentInactiveService() {

        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        serviceFlows.inactivateService(organizationId, service);

        AppointmentsHelper.getByAppointmentId(userToken, organizationId, locationId, appointment.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentById.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

    @Xray(requirement = "PEG-6198", test = "PEG-6704")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void getAppointmentWithInactiveLocation(Role role) {

        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createService(organizationId);

        final JSONObject appointment = appointmentsFlow.createAppointmentWithResource(organizationId, locationId, resourceId, service);
        locationFlows.inactivateLocation(organizationId, locationId);

        AppointmentsHelper.getByAppointmentId(userToken, organizationId, locationId, appointment.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getAppointmentById.json"))
                .body("organizationId", is(appointment.getString("organizationId")))
                .body("serviceId", is(appointment.getString("serviceId")))
                .body("confirmationCode", is(appointment.getString("confirmationCode")))
                .body("locationId", is(appointment.getString("locationId")))
                .body("resourceId", is(appointment.getString("resourceId")))
                .body("id", is(appointment.getString("id")));
    }

}
