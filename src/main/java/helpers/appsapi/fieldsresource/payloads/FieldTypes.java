package helpers.appsapi.fieldsresource.payloads;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

import static utils.TestUtils.getRandomElementFromList;

public enum FieldTypes {
  TEXT("Text"),
  NUMBER("Number"),
  CHECKBOX("Checkbox"),
  MULTI_SELECT_DROPDOWN("Multi select dropdown"),
  SINGLE_SELECT_DROPDOWN("Single select dropdown"),
  RADIOBUTTON("Radio button");

  private static final List<FieldTypes> ALL_FIELD_TYPES =
      Arrays.asList(
          TEXT, NUMBER, CHECKBOX, MULTI_SELECT_DROPDOWN, SINGLE_SELECT_DROPDOWN, RADIOBUTTON);
  private static final List<FieldTypes> FIELDS_WITHOUT_REGEX =
      Arrays.asList(CHECKBOX, MULTI_SELECT_DROPDOWN, SINGLE_SELECT_DROPDOWN, RADIOBUTTON, NUMBER);
  private static final List<FieldTypes> FIELDS_WITH_OPTIONS =
      Arrays.asList(CHECKBOX, MULTI_SELECT_DROPDOWN, SINGLE_SELECT_DROPDOWN, RADIOBUTTON);
  private static final List<FieldTypes> FIELDS_WITHOUT_OPTIONS = Arrays.asList(TEXT, NUMBER);

  @Getter private final String displayName;

  FieldTypes(String displayName) {
    this.displayName = displayName;
  }

  public static FieldTypes getRandomType() {
    return getRandomElementFromList(ALL_FIELD_TYPES);
  }

  public static FieldTypes getRandomTypeWithOption() {
    return getRandomElementFromList(FIELDS_WITH_OPTIONS);
  }

  public static FieldTypes getRandomTypeWithoutOption() {
    return getRandomElementFromList(FIELDS_WITHOUT_OPTIONS);
  }

  public static FieldTypes getRandomTypeWithoutRegex() {
    return getRandomElementFromList(FIELDS_WITHOUT_REGEX);
  }

}
