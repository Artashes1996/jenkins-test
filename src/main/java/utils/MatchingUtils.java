package utils;

import static org.hamcrest.Matchers.*;

import org.hamcrest.Matcher;
import org.json.JSONObject;

public class MatchingUtils {

    public static <T extends Integer> Matcher<T> expectedStatusOnRequest(String request, T expectedStatus) {
        return describedAs(request + "\nExpected -> <" + expectedStatus + ">", is(expectedStatus));
    }

    public static <T extends Integer> Matcher<T> expectedStatusOnRequest(JSONObject body, String request, T expectedStatus) {
        final String description = "\nOn REQUEST BODY -> \n" + body.toString(1)
                + "\n RESPONSE -> \n "
                + request
                + "\nExpected -> <" + expectedStatus + ">";

        return describedAs(description, is(expectedStatus));
    }

}
