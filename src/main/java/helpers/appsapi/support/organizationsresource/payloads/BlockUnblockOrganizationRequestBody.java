package helpers.appsapi.support.organizationsresource.payloads;

import org.json.JSONObject;

import java.util.*;
import java.util.function.Supplier;

import static utils.TestUtils.FAKER;
import static utils.TestUtils.getRandomInt;

public class BlockUnblockOrganizationRequestBody {

    private static final Map<BlockUnblockCombination, Supplier<JSONObject>> MAP = new HashMap<>();
    public static final String COMMENT = "comment";
    public static final String REASONS = "reasons";

    public enum BlockReasons {
        OTHER,
        LACK_OF_PAYMENT
    }

    public enum UnblockReasons {
        OTHER,
        CONTRACT_SIGNED,
        PAYMENT_RECEIVED
    }

    public enum BlockUnblockCombination {
        BLOCK,
        UNBLOCK
    }

    private static final Supplier<JSONObject> blockOrganization = () -> {

        final JSONObject blockBody = new JSONObject();
        final BlockReasons blockReasons = Arrays.asList(BlockReasons.values()).get(getRandomInt(BlockReasons.values().length));
        blockBody.put(BlockUnblockOrganizationRequestBody.REASONS, Collections.singletonList(blockReasons));

        return blockBody;
    };

    private static final Supplier<JSONObject> unblockOrganization = () -> {

        final JSONObject unblockBody = new JSONObject();
        final UnblockReasons unblockReasons = Arrays.asList(UnblockReasons.values()).get(getRandomInt(UnblockReasons.values().length));
        unblockBody.put(BlockUnblockOrganizationRequestBody.REASONS, Collections.singletonList(unblockReasons));

        return unblockBody;
    };


    public static JSONObject bodyBuilder(BlockUnblockCombination combination) {

        final JSONObject blockUnblock = MAP.get(combination).get();
        final String comment = FAKER.book().title();
        blockUnblock.put(BlockUnblockOrganizationRequestBody.COMMENT, comment + " Block");

        return blockUnblock;
    }

    static {
        MAP.put(BlockUnblockCombination.BLOCK, blockOrganization);
        MAP.put(BlockUnblockCombination.UNBLOCK, unblockOrganization);
    }
}
