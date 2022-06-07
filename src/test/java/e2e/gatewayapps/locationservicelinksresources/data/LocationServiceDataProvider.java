package e2e.gatewayapps.locationservicelinksresources.data;

import org.testng.annotations.DataProvider;

import static helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody.*;

public class LocationServiceDataProvider {

    @DataProvider(name = "validVisibilityFields")
    public static Object[][] validVisibilityFields(){
        return new Object[][]{
                {MONITOR},
                {WEB_KIOSK},
                {PHYSICAL_KIOSK}
        };
    }
}
