package helpers;

import configuration.db.postgres.PostgresConnectionFactory;
import configuration.db.postgres.PostgresSchema;
import lombok.SneakyThrows;

import java.sql.*;

@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class DBHelper {

    @SneakyThrows
    public static String getInvitationToken(final Object email) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM users WHERE email = ?;")) {
                statement.setObject(1, email);

                try (final ResultSet rs = statement.executeQuery()) {
                    String token = null;
                    while (rs.next()) {
                        token = rs.getString("invitation_token");
                    }
                    return token;
                }
            }
        }
    }

    @SneakyThrows
    public static Integer getAccountId(String accountType, boolean deleted) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT global_id FROM accounts WHERE type = ? AND deleted = ?;")) {
                statement.setString(1, accountType);
                statement.setBoolean(2, deleted);
                try (final ResultSet rs = statement.executeQuery()) {
                    Integer id = null;
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                    return id;
                }
            }
        }
    }

    @SneakyThrows
    public static String getUserId(String accountType, boolean deleted) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT global_id FROM users WHERE type = ? AND deleted = ?;")) {
                statement.setString(1, accountType);
                statement.setBoolean(2, deleted);
                try (final ResultSet rs = statement.executeQuery()) {
                    String id = null;
                    while (rs.next()) {
                        id = rs.getString("global_id");
                    }
                    return id;
                }
            }
        }
    }

    @SneakyThrows
    public static String getAccountIdByEmail(String email) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT global_id FROM accounts WHERE email = ?;")) {
                statement.setString(1, email);
                try (final ResultSet rs = statement.executeQuery()) {
                    String id = null;
                    while (rs.next()) {
                        id = rs.getString("global_id");
                    }
                    return id;
                }
            }
        }
    }

    @SneakyThrows
    public static String getAccountIdByUserId(String userId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT global_id FROM accounts WHERE user_global_id = ?;")) {
                statement.setString(1, userId);
                try (final ResultSet rs = statement.executeQuery()) {
                    String id = null;
                    while (rs.next()) {
                        id = rs.getString("global_id");
                    }
                    return id;
                }
            }
        }
    }

    @SneakyThrows
    public static String getUserIdByEmail(String email) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT global_id FROM users WHERE email = ?;")) {
                statement.setString(1, email);
                try (final ResultSet rs = statement.executeQuery()) {
                    String id = null;
                    while (rs.next()) {
                        id = rs.getString("global_id");
                    }
                    return id;
                }
            }
        }
    }

    @SneakyThrows
    public static String getUserInvitationTokenByEmail(String email) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT invitation_token FROM users WHERE email = ?;")) {
                statement.setString(1, email);
                try (final ResultSet rs = statement.executeQuery()) {
                    String token = null;
                    while (rs.next()) {
                        token = rs.getString("invitation_token");
                    }
                    return token;
                }
            }
        }
    }

    @SneakyThrows
    public static Integer getAccountAttachedToOrganization() {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT global_id FROM accounts WHERE organization_id IS NOT NULL AND deleted = false;")) {

                try (final ResultSet rs = statement.executeQuery()) {
                    Integer id = null;
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                    return id;
                }
            }
        }
    }

    @SneakyThrows
    public static Integer getUserAttachedToOrganization() {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT global_id FROM users WHERE organization_id IS NOT NULL AND deleted = false;")) {

                try (final ResultSet rs = statement.executeQuery()) {
                    Integer id = null;
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                    return id;
                }
            }
        }
    }

    @SneakyThrows
    public static String getEmployeeIdOfOrganization(String organizationId, String invitationStatus, String accountStatus) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT global_id FROM accounts WHERE organization_id = ? AND type = 'EMPLOYEE' AND invitation_status = ? AND account_status = ? AND deleted = false;")) {
                statement.setString(1, organizationId);
                statement.setString(2, invitationStatus);
                statement.setString(3, accountStatus);
                try (final ResultSet rs = statement.executeQuery()) {
                    String id = null;
                    while (rs.next()) {
                        id = rs.getString("id");
                    }
                    return id;
                }
            }
        }
    }

    @SneakyThrows
    public static Integer getEmployeeIdOfOtherOrganization(String organizationId, String invitationStatus, String accountStatus) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT id FROM accounts WHERE organization_id != ? AND type = 'EMPLOYEE' AND invitation_status = ? AND account_status = ? AND deleted = false;")) {
                statement.setString(1, organizationId);
                statement.setString(2, invitationStatus);
                statement.setString(3, accountStatus);
                try (final ResultSet rs = statement.executeQuery()) {
                    Integer id = null;
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                    return id;
                }
            }
        }
    }


    @SneakyThrows
    public static String getExpiredToken(String organizationId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM users WHERE invitation_expiration_date < now() AND organization_id = ?;")) {
                statement.setString(1, organizationId);
                try (final ResultSet rs = statement.executeQuery()) {
                    String token = null;
                    while (rs.next()) {
                        token = rs.getString("invitation_token");
                    }
                    return token;
                }
            }
        }
    }

    @SneakyThrows
    public static String getExpiredInvitationEmail(String organizationId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM users WHERE invitation_expiration_date = current_timestamp - interval '1 day' AND organization_id = ? AND deleted=false;")) {
                statement.setString(1, organizationId);
                try (final ResultSet rs = statement.executeQuery()) {
                    String token = null;
                    while (rs.next()) {
                        token = rs.getString("email");
                    }
                    return token;
                }
            }
        }
    }

    @SneakyThrows
    public static Boolean checkAccountGroup(final int accountId, final int groupId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM account_group_accounts WHERE account_id = ? AND account_group_id = ?;")) {
                statement.setLong(1, accountId);
                statement.setLong(2, groupId);

                try (final ResultSet rs = statement.executeQuery()) {
                    Long id = null;
                    while (rs.next()) {
                        id = rs.getLong("account_id");
                    }
                    return (id != null);
                }

            }
        }
    }

    @SneakyThrows
    public static Boolean checkRoles(final int accountId, final int roleId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM account_permissions WHERE account_id = ? AND role_id = ?;")) {
                statement.setLong(1, accountId);
                statement.setLong(2, roleId);

                try (final ResultSet rs = statement.executeQuery()) {
                    Long id = null;
                    while (rs.next()) {
                        id = rs.getLong("id");
                    }
                    return (id != null);
                }

            }
        }
    }

    @SneakyThrows
    public static Integer getGlobalRole(boolean deleted) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM roles WHERE organization_id IS NULL AND deleted = ?;")) {
                statement.setBoolean(1, deleted);
                try (final ResultSet rs = statement.executeQuery()) {
                    Integer id = null;
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                    return id;
                }

            }
        }
    }

    @SneakyThrows
    public static void expireInvitationToken(String token) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("UPDATE users SET invitation_status_id = 4, invitation_expiration_date = current_timestamp - interval '1 day' WHERE invitation_token = ?;")) {
                statement.setString(1, token);
                statement.executeUpdate();
                connection.commit();
            }
        }
    }

    @SneakyThrows
    public static void expireInvitationByUserId(String userId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("UPDATE users SET invitation_status = 'EXPIRED', invitation_expiration_date = current_timestamp - interval '1 day' WHERE global_id = ?;")) {
                statement.setString(1, userId);
                statement.executeUpdate();
                connection.commit();
            }
        }
    }

    @SneakyThrows
    public static Integer getRoleForOrganization(String organization, boolean deleted) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM roles r inner join organizations m on m.id=r.organization_id WHERE m.global_id = ? AND r.deleted = ?")) {
                statement.setString(1, organization);
                statement.setBoolean(2, deleted);
                try (final ResultSet rs = statement.executeQuery()) {
                    Integer id = null;
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                    return id;
                }

            }
        }
    }

    @SneakyThrows
    public static Integer getGlobalGroup(boolean deleted) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM account_groups WHERE organization_id IS NULL AND deleted = ?;")) {
                statement.setBoolean(1, deleted);
                try (final ResultSet rs = statement.executeQuery()) {
                    Integer id = null;
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                    return id;
                }

            }
        }
    }

    @SneakyThrows
    public static Integer getGroupForOrganization(String organizationId, boolean deleted) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM security_service.account_groups ag left join security_service.organizations m on m.id=ag.organization_id WHERE m.global_id = ? AND ag.deleted = ?")) {
                statement.setString(1, organizationId);
                statement.setBoolean(2, deleted);
                try (final ResultSet rs = statement.executeQuery()) {
                    Integer id = null;
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                    return id;
                }

            }
        }
    }

    @SneakyThrows
    public static String getRestoreToken(String accountId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM account_restores ar left join accounts a on a.id=ar.account_id WHERE a.user_global_id = ?;")) {
                statement.setString(1, accountId);
                try (final ResultSet rs = statement.executeQuery()) {
                    String id = null;
                    while (rs.next()) {
                        id = rs.getString("token");
                    }
                    return id;
                }

            }
        }
    }


    @SneakyThrows
    public static void setAccountFieldValueById(String accountId, String columnName, Object value) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement = connection.prepareStatement("UPDATE accounts SET " + columnName + "=? WHERE global_id=?")) {
                if (value instanceof String) statement.setString(1, value.toString());
                else if (value instanceof Integer) statement.setInt(1, (Integer) value);
                statement.setString(2, accountId);
                statement.executeUpdate();
                connection.commit();
            }
        }
    }

    @SneakyThrows
    public static void setUserFieldValueById(String userId, String columnName, Object value) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement = connection.prepareStatement("UPDATE accounts SET " + columnName + "=? WHERE user_global_id=?")) {
                if (value instanceof String) statement.setString(1, value.toString());
                else if (value instanceof Integer) statement.setInt(1, (Integer) value);
                statement.setString(2, userId);
                statement.executeUpdate();
                connection.commit();
            }
        }
    }

    @SneakyThrows
    public static void setAccountFieldValueByEmail(String email, String columnName, Object value) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement = connection.prepareStatement("UPDATE accounts SET " + columnName + "=? WHERE email=?")) {
                if (value instanceof String) statement.setString(1, value.toString());
                else if (value instanceof Integer) statement.setInt(1, (Integer) value);

                statement.setString(2, email);
                statement.executeUpdate();
                connection.commit();
            }
        }
    }

    @SneakyThrows
    public static void setUserFieldValueByEmail(String email, String columnName, Object value) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement = connection.prepareStatement("UPDATE users SET " + columnName + "=? WHERE email=?")) {
                if (value instanceof String) statement.setString(1, value.toString());
                else if (value instanceof Integer) statement.setInt(1, (Integer) value);
                else if (value instanceof Timestamp) statement.setTimestamp(1, (Timestamp) value);

                statement.setString(2, email);
                statement.executeUpdate();
                connection.commit();
            }
        }
    }

    @SneakyThrows
    public static Object getAccountFieldValueById(String id, String columnName) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM accounts WHERE global_id = ?")) {
                statement.setString(1, id);
                try (final ResultSet rs = statement.executeQuery()) {
                    Object result = null;
                    while (rs.next()) {
                        result = rs.getObject(columnName);
                    }
                    return result;
                }
            }
        }
    }

    @SneakyThrows
    public static Object getUserFieldValueById(String id, String columnName) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE global_id = ?")) {
                statement.setString(1, id);
                try (final ResultSet rs = statement.executeQuery()) {
                    Object result = null;
                    while (rs.next()) {
                        result = rs.getObject(columnName);
                    }
                    return result;
                }
            }
        }
    }

    @SneakyThrows
    public static Object getOrganizationFieldValueById(String organizationId, String columnName) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM organizations WHERE global_id = ?")) {
                statement.setString(1, organizationId);
                try (final ResultSet rs = statement.executeQuery()) {
                    Object result = null;
                    while (rs.next()) {
                        result = rs.getObject(columnName);
                    }
                    return result;
                }
            }
        }
    }

    @SneakyThrows
    public static Object getOrganizationFieldValueByInternalName(String name, String columnName) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM organizations WHERE internal_name = ?")) {
                statement.setString(1, name);
                try (final ResultSet rs = statement.executeQuery()) {
                    Object result = null;
                    while (rs.next()) {
                        result = rs.getObject(columnName);
                    }
                    return result;
                }
            }
        }
    }

    @SneakyThrows
    public static Object getAccountFieldValueByEmail(String email, String columnName) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM accounts WHERE email = ?")) {
                statement.setString(1, email);
                try (final ResultSet rs = statement.executeQuery()) {
                    Object result = null;
                    while (rs.next()) {
                        result = rs.getObject(columnName);
                    }
                    return result;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    @SneakyThrows
    public static Object getUserFieldValueByEmail(String email, String columnName) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE email = ?")) {
                statement.setString(1, email);
                try (final ResultSet rs = statement.executeQuery()) {
                    Object result = null;
                    while (rs.next()) {
                        result = rs.getObject(columnName);
                    }
                    return result;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    @SneakyThrows
    public static Object getResetPasswordToken(String userId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM password_resets pr left join accounts a on a.id=pr.account_id WHERE a.user_global_id=?")) {
                statement.setString(1, userId);
                try (final ResultSet rs = statement.executeQuery()) {
                    Object result = null;
                    while (rs.next()) {
                        result = rs.getObject("token");
                    }
                    return result;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    @SneakyThrows
    public static void expireResetPasswordToken(String userId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement = connection.prepareStatement("UPDATE password_resets pr SET expiration_date=current_timestamp - interval '1 day' from accounts a WHERE pr.account_id=a.id and a.user_global_id=?")) {
                statement.setString(1, userId);
                statement.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    @SneakyThrows
    public static void expireRestoreToken(String userId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement = connection
                    .prepareStatement("update account_restores ar set expiration_date = current_timestamp - interval '1 day' from accounts a where ar.account_id = a.id and a.user_global_id = ?;")) {
                statement.setString(1, userId);
                statement.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    @SneakyThrows
    public static Object getRestoreTokenById(String userId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM account_restores ar left join accounts a on a.id=ar.account_id WHERE a.user_global_id=?")) {
                statement.setString(1, userId);
                try (final ResultSet rs = statement.executeQuery()) {
                    Object token = null;
                    while (rs.next()) {
                        token = rs.getObject("token");
                    }
                    return token;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    @SneakyThrows
    public static String getEmployeeResourceIdByUserId(String userId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM resources WHERE internal_name = ?")) {
                statement.setString(1, userId);

                try (final ResultSet rs = statement.executeQuery()) {
                    String id = null;
                    while (rs.next()) {
                        id = rs.getString("global_id");
                    }
                    return id;
                }

            }
        }
    }

    @SneakyThrows
    public static boolean getUserDeletedState(String userId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.SECURITY_SERVICE)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT deleted FROM accounts WHERE user_global_id=?")) {
                statement.setString(1, userId);
                try (final ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("deleted");
                    }
                    return false;
                }
            }
        }
    }

    @SneakyThrows
    public static String getUserStatus(String userId) {
        try (final Connection connection = PostgresConnectionFactory.getConnectionForSchema(PostgresSchema.CONFIGURATION_MANAGER)) {
            try (final PreparedStatement statement =
                         connection.prepareStatement("SELECT user_status_id FROM users WHERE global_id=?")) {
                statement.setString(1, userId);
                try (final ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("user_status_id");
                    }
                    return null;
                }
            }
        }
    }
}



