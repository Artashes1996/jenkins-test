package helpers.flows;

import configuration.Role;
import helpers.DBHelper;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.appsapi.accountresource.payloads.ForceResetPasswordRequestBody;
import helpers.appsapi.accountresource.payloads.ResetPasswordRequestBody;
import helpers.appsapi.usersresource.UserHelper;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

import java.util.Objects;

import static helpers.appsapi.accountresource.payloads.RestoreRequestBody.*;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class AccountFlows {

    public void restoreRequest(Object email, Object organizationId) {
        final JSONObject restoreRequestBody = new JSONObject();
        restoreRequestBody.put(EMAIL, email);

        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreRequestBody)
            .then()
            .statusCode(SC_OK);

    }

    public void resetPasswordRequest(String email){
        final JSONObject resetPasswordRequestBody = new JSONObject();
        resetPasswordRequestBody.put(ResetPasswordRequestBody.EMAIL, email);
        AccountHelper.resetPasswordRequest(resetPasswordRequestBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public String getResetPasswordRequestToken(String email){

        resetPasswordRequest(email);
        return (String) DBHelper.getResetPasswordToken(DBHelper.getUserIdByEmail(email));
    }

    public JSONObject getUserAndUsersResetPasswordToken(String organizationId){

        final JSONObject user = new UserFlows().createUser(organizationId, Role.OWNER, null);
        final String userId = user.getString("id");
        resetPasswordRequest(user.getString("email"));
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("TOKEN", Objects.requireNonNull(DBHelper.getResetPasswordToken(userId)).toString());
        jsonObject.put("USER", user);
        return jsonObject;
    }

    public String getExpiredTokenOfResetPass(String organizationId){
        final JSONObject resetPassTokenAndUser = getUserAndUsersResetPasswordToken(organizationId);
        final String token = resetPassTokenAndUser.getString("TOKEN");
        DBHelper.expireResetPasswordToken(resetPassTokenAndUser.getJSONObject("USER").getString("id"));
        return token;
    }

    @SneakyThrows
    public String getDeletedTokenOfResetPass(String organizationId){
        final JSONObject user = new UserFlows().createUser(organizationId, Role.OWNER, null);
        resetPasswordRequest(user.getString("email"));
        new UserFlows().deleteUser( organizationId, user.getString("id"));
        Thread.sleep(20000);
        return (String) DBHelper.getResetPasswordToken(user.getString("id"));
    }

    public void forceResetUserById(String organizationId, String userId) {

        final JSONObject forceResetBody = new JSONObject();
        final String accountId = DBHelper.getAccountIdByUserId(userId);

        forceResetBody.put(ForceResetPasswordRequestBody.ID, accountId);
        AccountHelper.forceResetPasswordRequest(SUPPORT_TOKEN, organizationId, forceResetBody)
                .then()
                .statusCode(SC_OK);
    }
}
