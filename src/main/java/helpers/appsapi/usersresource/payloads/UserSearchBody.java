package helpers.appsapi.usersresource.payloads;

import lombok.Getter;

public class UserSearchBody {
    public final static String INCLUDE_DELETED = "includeDeleted";
    public final static String INVITATION_STATUSES = "invitationStatuses";
    public final static String LOCATION_IDS = "locationIds";
    public final static String PAGINATION = "pagination";
    public final static String PAGE = "page";
    public final static String SIZE = "size";
    public final static String SORT = "sort";
    public final static String USER_STATUSES = "userStatuses";
    public final static String POINT_OF_CONTACT = "pointOfContact";
    public final static String QUERY = "query";

    @Getter
    public enum SortingBy {
        ID("ID:ASC", "ID:DESC"),
        FIRST_NAME("FIRST_NAME:ASC", "FIRST_NAME:DESC"),
        LAST_NAME("LAST_NAME:ASC", "LAST_NAME:DESC"),
        EMAIL("EMAIL:ASC", "EMAIL:DESC"),
        USER_STATUS("USER_STATUS:ASC", "USER_STATUS:DESC"),
        INVITATION_STATUS("INVITATION_STATUS:ASC", "INVITATION_STATUS:DESC");
        private final String ascending;
        private final String descending;

        SortingBy(String ascending, String descending) {
            this.ascending = ascending;
            this.descending = descending;
        }
    }

}