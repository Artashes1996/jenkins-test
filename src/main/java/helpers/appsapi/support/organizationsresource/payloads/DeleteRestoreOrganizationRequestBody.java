package helpers.appsapi.support.organizationsresource.payloads;

import org.json.JSONObject;

import java.util.*;

public class DeleteRestoreOrganizationRequestBody {
    public final static String COMMENT = "comment";
    public final static String ID = "id";
    public final static String REASONS = "reasons";


    public enum DeleteReasons {
        LACK_OF_PAYMENT,
        CONTRACT_CANCELLATION,
        OTHER
    }

    public enum RestoreReasons {
        OTHER,
        CONTRACT_SIGNED,
        PAYMENT_RECEIVED
    }

    public enum DeleteRestoreCombination {
        DELETE,
        RESTORE
    }

    public static JSONObject bodyBuilder(DeleteRestoreCombination combination) {
        final JSONObject deleteRestoreOrganization = new JSONObject();
        final String comment = UUID.randomUUID().toString();

        switch (combination) {
            case DELETE:
                final DeleteReasons randomDeleteReason = Arrays.asList(DeleteReasons.values()).get(new Random().nextInt(DeleteReasons.values().length));
                deleteRestoreOrganization.put(DeleteRestoreOrganizationRequestBody.REASONS, Collections.singletonList(randomDeleteReason));
                if (randomDeleteReason.equals(DeleteReasons.OTHER))
                    deleteRestoreOrganization.put(DeleteRestoreOrganizationRequestBody.COMMENT, comment + " Delete");
                break;
            case RESTORE:
                final RestoreReasons randomRestoreReason = Arrays.asList(RestoreReasons.values()).get(new Random().nextInt(RestoreReasons.values().length));
                deleteRestoreOrganization.put(DeleteRestoreOrganizationRequestBody.REASONS, Collections.singletonList(randomRestoreReason));
                deleteRestoreOrganization.put(DeleteRestoreOrganizationRequestBody.COMMENT, comment + " Restore");
                break;
        }
        return deleteRestoreOrganization;
    }
}
