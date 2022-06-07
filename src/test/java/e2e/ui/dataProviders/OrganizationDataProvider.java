package e2e.ui.dataProviders;

import org.testng.annotations.DataProvider;
import pages.CreateOrganization;

import static utils.TestUtils.RANDOM;

public class OrganizationDataProvider {

    @DataProvider(name = "Valid Organizations")
    public static Object[][] validOrganizations() {
        return new Object[][]{
                {CreateOrganization.Vertical.EDUCATION, "TESLA " + RANDOM.nextInt(), "https://www.tesla.com/", "+37455634513"},
                {CreateOrganization.Vertical.GOVERNMENT, "FORD " + RANDOM.nextInt(), "https://www.ford.com/", "+37455123454"},
                {CreateOrganization.Vertical.HEALTHCARE, "FERRARI " + RANDOM.nextInt(), "https://www.ferrari.com/en-EN", "+37455678685"},
                {CreateOrganization.Vertical.RETAIL_OTHER, "INFINITY " + RANDOM.nextInt(), "https://www.sfl.am", "+37499455445"}
        };
    }


    @DataProvider(name = "Invalid organizations with invalid values")
    public static Object[][] invalidOrganizationValues() {
        return new Object[][]{
                {CreateOrganization.RequiredFields.URL, null,"Monkey Shoulder " + RANDOM.nextInt(), "url for wiskey", "+37455634513"},
                {CreateOrganization.RequiredFields.PHONE, null, "Johnnie Walker " + RANDOM.nextInt(), "https://www.ferrari.com/en-EN", "37467883399"},
        };
    }

    @DataProvider(name = "Valid values organizations edit")
    public static Object[][] validOrganizationsEditValues() {

        return new Object[][]{
                {"https://www.tesla.com/", "+37455634513"},
                {" ", " "}
        };
    }

    @DataProvider(name = "Invalid phone number")
    public static Object[][] invalidPhoneNumber() {
        return new Object[][]{
                {"+37434634513"},
                {"37455222222"},
                {"plusOneTwo"}
        };
    }

    @DataProvider(name = "Invalid website url")
    public static Object[][] invalidWebsiteUrl() {
        return new Object[][]{
                {"http:/www.google.com"},
                {"www.google.com"}
        };
    }

}