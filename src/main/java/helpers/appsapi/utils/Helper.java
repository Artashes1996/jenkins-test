package helpers.appsapi.utils;

import java.util.Date;

public class Helper {

    boolean isWithinRange(Date testDate, Date startDate, Date endDate) {
        return !(testDate.before(startDate) || testDate.after(endDate));
    }
}
