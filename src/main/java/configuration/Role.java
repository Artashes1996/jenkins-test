package configuration;

import java.util.*;

import static utils.TestUtils.*;

public enum Role {
    SUPPORT,
    OWNER,
    ADMIN,
    LOCATION_ADMIN,
    STAFF;

    private static final List<Role> ORGANIZATION_ROLES = Arrays.asList(OWNER, ADMIN, LOCATION_ADMIN, STAFF);
    private static final List<Role> INVITER_ROLES = Arrays.asList(SUPPORT, OWNER, ADMIN, LOCATION_ADMIN);
    private static final List<Role> ORGANIZATION_INVITER_ROLES = Arrays.asList(OWNER, ADMIN, LOCATION_ADMIN);
    private static final List<Role> ADMIN_ROLES = Arrays.asList(SUPPORT, OWNER, ADMIN);
    private static final List<Role> ORGANIZATION_ADMIN_ROLES = Arrays.asList(OWNER, ADMIN);
    private static final List<Role> ROLES_WITH_LOCATION = Arrays.asList(LOCATION_ADMIN, STAFF);
    private static final List<Role> HIGHER_ADMIN_ROLES = Arrays.asList(SUPPORT, OWNER);

    public static Role getRandomRole() {
        return Arrays.asList(Role.values()).get(getRandomInt(Role.values().length));
    }

    public static Role getRandomOrganizationRole() {
        return getRandomElementFromList(ORGANIZATION_ROLES);
    }

    public static Role getRandomInviterRole() {
        return getRandomElementFromList(INVITER_ROLES);
    }

    public static Role getRandomOrganizationInviterRole() { return getRandomElementFromList(ORGANIZATION_INVITER_ROLES);}

    public static Role getRandomAdminRole() {
        return getRandomElementFromList(ADMIN_ROLES);
    }

    public static Role getRandomOrganizationAdminRole() {
        return getRandomElementFromList(ORGANIZATION_ADMIN_ROLES);
    }

    public static Role getRandomRolesWithLocation() {
        return getRandomElementFromList(ROLES_WITH_LOCATION);
    }

    public static Role getRandomHigherAdminRole() {
        return getRandomElementFromList(HIGHER_ADMIN_ROLES);
    }



}
