package e2e.gatewayapps.userresource.data;

import configuration.Role;
import org.testng.annotations.DataProvider;

import java.util.UUID;

public class RoleDataProvider {

    @DataProvider(name = "adminRoles")
    public static Object[][] adminRoles() {
        return new Object[][]{
                {Role.OWNER},
                {Role.SUPPORT}
        };
    }

    @DataProvider(name = "extendedAdminRoles")
    public static Object[][] extendedAdminRoles() {
        return new Object[][]{
                {Role.ADMIN},
                {Role.OWNER},
                {Role.SUPPORT}
        };
    }

    @DataProvider(name = "allRoles")
    public static Object[][] allRoles() {
        return new Object[][]{
                {Role.STAFF},
                {Role.LOCATION_ADMIN},
                {Role.ADMIN},
                {Role.OWNER},
                {Role.SUPPORT}
        };
    }

    @DataProvider(name = "organizationAdminRoles", parallel = true)
    public static Object[][] organizationAdminRoles() {
        return new Object[][]{
                {Role.ADMIN},
                {Role.OWNER}
        };
    }

    @DataProvider(name = "otherOrganizationRoles", parallel = true)
    public static Object[][] otherOrganizationRoles() {
        return new Object[][]{
                {Role.ADMIN},
                {Role.LOCATION_ADMIN}
        };
    }

    @DataProvider(name = "allOrganizationRoles")
    public static Object[][] allOrganizationRoles() {
        return new Object[][]{
                {Role.STAFF},
                {Role.LOCATION_ADMIN},
                {Role.ADMIN},
                {Role.OWNER}
        };
    }

    @DataProvider(name = "inviters")
    public static Object[][] inviters() {
        return new Object[][]{
                {Role.SUPPORT},
                {Role.LOCATION_ADMIN},
                {Role.ADMIN},
                {Role.OWNER}
        };
    }

    @DataProvider(name = "organizationLevelInviters")
    public static Object[][] organizationLevelInviters() {
        return new Object[][]{
                {Role.LOCATION_ADMIN},
                {Role.ADMIN},
                {Role.OWNER}
        };
    }

    @DataProvider(name = "rolesWithLocation")
    public static Object[][] rolesWithLocations() {
        return new Object[][]{
                {Role.STAFF},
                {Role.LOCATION_ADMIN}
        };
    }

    @DataProvider(name = "otherOrganizationUsers")
    public static Object[][] otherOrganizationUsers() {
        return new Object[][]{
                {Role.ADMIN},
                {Role.LOCATION_ADMIN}
        };
    }

    @DataProvider(name = "invalidRoles")
    public static Object[][] invalidRoles() {
        return new Object[][]{
                {"SUPORT"},
                {"EMPLOYEE"},
                {UUID.randomUUID().toString()},
                {"MOTHER_OF_DRAGONS"}
        };
    }

    @DataProvider(name = "some valid roles combinations", parallel = true)
    public static Object[][] validRoleCombinations() {
        return new Object[][]{
                {Role.SUPPORT, Role.OWNER},
                {Role.SUPPORT, Role.LOCATION_ADMIN},

                {Role.OWNER, Role.ADMIN},
                {Role.OWNER, Role.STAFF},

                {Role.ADMIN, Role.OWNER},
                {Role.ADMIN, Role.STAFF},

                {Role.LOCATION_ADMIN, Role.LOCATION_ADMIN},
                {Role.LOCATION_ADMIN, Role.ADMIN},

                {Role.STAFF, Role.LOCATION_ADMIN},
                {Role.STAFF, Role.OWNER},
        };
    }

    @DataProvider(name = "valid role change from role to role", parallel = true)
    public static Object[][] validRoleChangeFromRoleToRole() {
        return new Object[][]{
                {Role.SUPPORT, Role.OWNER, Role.ADMIN},
                {Role.SUPPORT, Role.OWNER, Role.LOCATION_ADMIN},
                {Role.SUPPORT, Role.OWNER, Role.STAFF},
                {Role.SUPPORT, Role.ADMIN, Role.OWNER},
                {Role.SUPPORT, Role.ADMIN, Role.LOCATION_ADMIN},
                {Role.SUPPORT, Role.ADMIN, Role.STAFF},
                {Role.SUPPORT, Role.LOCATION_ADMIN, Role.OWNER},
                {Role.SUPPORT, Role.LOCATION_ADMIN, Role.ADMIN},
                {Role.SUPPORT, Role.LOCATION_ADMIN, Role.STAFF},
                {Role.SUPPORT, Role.STAFF, Role.OWNER},
                {Role.SUPPORT, Role.STAFF, Role.ADMIN},
                {Role.SUPPORT, Role.STAFF, Role.LOCATION_ADMIN},

                {Role.OWNER, Role.OWNER, Role.ADMIN},
                {Role.OWNER, Role.OWNER, Role.LOCATION_ADMIN},
                {Role.OWNER, Role.OWNER, Role.STAFF},
                {Role.OWNER, Role.ADMIN, Role.OWNER},
                {Role.OWNER, Role.ADMIN, Role.LOCATION_ADMIN},
                {Role.OWNER, Role.ADMIN, Role.STAFF},
                {Role.OWNER, Role.LOCATION_ADMIN, Role.OWNER},
                {Role.OWNER, Role.LOCATION_ADMIN, Role.ADMIN},
                {Role.OWNER, Role.LOCATION_ADMIN, Role.STAFF},
                {Role.OWNER, Role.STAFF, Role.OWNER},
                {Role.OWNER, Role.STAFF, Role.ADMIN},
                {Role.OWNER, Role.STAFF, Role.LOCATION_ADMIN},

                {Role.ADMIN, Role.LOCATION_ADMIN, Role.STAFF},
                {Role.ADMIN, Role.STAFF, Role.LOCATION_ADMIN},
        };
    }

    @DataProvider(name = "invalid role change from role to role", parallel = true)
    public static Object[][] invalidRoleChangeFromRoleToRole() {
        return new Object[][]{
                {Role.STAFF, Role.OWNER, Role.ADMIN},
                {Role.STAFF, Role.OWNER, Role.LOCATION_ADMIN},
                {Role.STAFF, Role.OWNER, Role.STAFF},
                {Role.STAFF, Role.ADMIN, Role.OWNER},
                {Role.STAFF, Role.ADMIN, Role.LOCATION_ADMIN},
                {Role.STAFF, Role.ADMIN, Role.STAFF},
                {Role.STAFF, Role.LOCATION_ADMIN, Role.OWNER},
                {Role.STAFF, Role.LOCATION_ADMIN, Role.ADMIN},
                {Role.STAFF, Role.LOCATION_ADMIN, Role.STAFF},
                {Role.STAFF, Role.STAFF, Role.OWNER},
                {Role.STAFF, Role.STAFF, Role.ADMIN},
                {Role.STAFF, Role.STAFF, Role.LOCATION_ADMIN},

                {Role.LOCATION_ADMIN, Role.OWNER, Role.ADMIN},
                {Role.LOCATION_ADMIN, Role.OWNER, Role.LOCATION_ADMIN},
                {Role.LOCATION_ADMIN, Role.OWNER, Role.STAFF},
                {Role.LOCATION_ADMIN, Role.ADMIN, Role.OWNER},
                {Role.LOCATION_ADMIN, Role.ADMIN, Role.LOCATION_ADMIN},
                {Role.LOCATION_ADMIN, Role.ADMIN, Role.STAFF},
                {Role.LOCATION_ADMIN, Role.LOCATION_ADMIN, Role.OWNER},
                {Role.LOCATION_ADMIN, Role.LOCATION_ADMIN, Role.ADMIN},
                {Role.LOCATION_ADMIN, Role.LOCATION_ADMIN, Role.STAFF},
                {Role.LOCATION_ADMIN, Role.STAFF, Role.OWNER},
                {Role.LOCATION_ADMIN, Role.STAFF, Role.ADMIN},
                {Role.LOCATION_ADMIN, Role.STAFF, Role.LOCATION_ADMIN},

                {Role.ADMIN, Role.OWNER, Role.ADMIN},
                {Role.ADMIN, Role.OWNER, Role.LOCATION_ADMIN},
                {Role.ADMIN, Role.OWNER, Role.STAFF},
                {Role.ADMIN, Role.ADMIN, Role.OWNER},
                {Role.ADMIN, Role.ADMIN, Role.LOCATION_ADMIN},
                {Role.ADMIN, Role.ADMIN, Role.STAFF},
                {Role.ADMIN, Role.LOCATION_ADMIN, Role.OWNER},
                {Role.ADMIN, Role.LOCATION_ADMIN, Role.ADMIN},
                {Role.ADMIN, Role.STAFF, Role.OWNER},
                {Role.ADMIN, Role.STAFF, Role.ADMIN},
        };
    }

    @DataProvider(name = "invalid role change (for org level user)", parallel = true)
    public static Object[][] invalidOrgLevelUserRoleChange() {
        return new Object[][]{
                {Role.OWNER, Role.OWNER, Role.ADMIN},
                {Role.OWNER, Role.ADMIN, Role.OWNER}
        };
    }


}