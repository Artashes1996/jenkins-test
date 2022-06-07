package helpers.flows;

import configuration.Role;
import helpers.appsapi.appointmentsresource.AppointmentsHelper;
import helpers.appsapi.appointmentsresource.payloads.AppointmentCreationRequest;
import org.json.JSONObject;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.LINK;

public class AppointmentsFlow {

    private final String supportToken = new AuthenticationFlowHelper().getToken(Role.SUPPORT);
    private final ResourceFlows resourceFlows = new ResourceFlows();
    private final LocationFlows locationFlows = new LocationFlows();
    private final UserFlows userFlows = new UserFlows();
    private final FixedAvailabilitiesFlows fixedAvailabilitiesFlows = new FixedAvailabilitiesFlows();

    /***
     * Links Service to location,
     * links service to resource,
     * create fixed availability for the resource
     * Creates appointment
     */
    public JSONObject createAppointmentWithResource(String organizationId, String locationId, String resourceId, JSONObject service) {

        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now().plusYears(1));
        final String startTime = LocalTime.MIN.toString();
        final LocalDateTime endDateTime = LocalDateTime.of(LocalDate.now().plusYears(1), LocalTime.MIN).plusSeconds(service.getInt("duration") + 60);
        final String endTime = endDateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, resourceId, date, startTime, endTime);

        locationFlows.linkUnlinkServicesToLocation(organizationId, locationId, Collections.singletonList(service.getString("id")), LINK);
        resourceFlows.linkUnlinkServiceToResource(organizationId, locationId, resourceId, service.getString("id"), LINK);

        final AppointmentCreationRequest appointmentCreationRequest = new AppointmentCreationRequest();
        final String appointmentCreationStartDateTime = LocalDateTime.of(LocalDate.parse(date), LocalTime.MIN).toString();
        final JSONObject appointmentCreationRequestBody = appointmentCreationRequest.bodyBuilder(appointmentCreationStartDateTime, resourceId);

        return new JSONObject(AppointmentsHelper
                .createAppointment(SUPPORT_TOKEN, organizationId, locationId, service.getString("id"), appointmentCreationRequestBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }

    public JSONObject createAppointmentWithUser(String organizationId, String locationId, String userId, JSONObject service) {
        FixedAvailabilitiesFlows fixedAvailabilitiesFlows = new FixedAvailabilitiesFlows();
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now().plusYears(1));
        final String startTime = LocalTime.MIN.toString();
        final LocalDateTime endDateTime = LocalDateTime.of(LocalDate.now().plusYears(1), LocalTime.MIN).plusSeconds(service.getInt("duration") + 60);
        final String endTime = endDateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        final String resourceId = resourceFlows.getResourceIdFromUserId(organizationId, userId);
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, resourceId, date, startTime, endTime);

        locationFlows.linkUnlinkServicesToLocation(organizationId, locationId, Collections.singletonList(service.getString("id")), LINK);
        userFlows.linkUnlinkUserToLocationService(organizationId, userId, locationId, service.getString("id"), LINK);

        final AppointmentCreationRequest appointmentCreationRequest = new AppointmentCreationRequest();
        final String appointmentCreationStartDateTime = LocalDateTime.of(LocalDate.parse(date), LocalTime.MIN).toString();
        final JSONObject appointmentCreationRequestBody = appointmentCreationRequest.bodyBuilder(appointmentCreationStartDateTime, resourceId);

        return new JSONObject(AppointmentsHelper
                .createAppointment(SUPPORT_TOKEN, organizationId, locationId, service.getString("id"), appointmentCreationRequestBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }

    /***
     * Creates appointment by given already setup data
     */
    public JSONObject createAppointmentByGivenTime(String organizationId, String locationId, String resourceId, String serviceId, LocalDateTime dateTime) {
        final AppointmentCreationRequest appointmentCreationRequest = new AppointmentCreationRequest();
        final JSONObject appointmentCreationRequestBody = appointmentCreationRequest.bodyBuilder(dateTime.toString(), resourceId);

        return new JSONObject(AppointmentsHelper
                .createAppointment(SUPPORT_TOKEN, organizationId, locationId, serviceId, appointmentCreationRequestBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }
}
