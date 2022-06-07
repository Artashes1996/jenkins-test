package pages;

import configuration.Role;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;

import java.util.List;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.assertEquals;
import static utils.TestUtils.capitalize;

public class InvitationCreationPage extends BasePage<InvitationCreationPage>{

    private final String PAGE_URL = "/company/management/users/invitation";

    @FindBy(css = "[data-testid='multi-emails-input']")
    private WebElement invitationInput;
    @FindBy(xpath = "*//button[text()='ADD USER']")
    private WebElement addUserButton;
    @FindBy(css = "[class*='_user-email-container']")
    private List<WebElement> emails;
    @FindBy(css = "[name='firstName']")
    private List<WebElement> firstNames;
    @FindBy(css = "[name='lastName']")
    private List<WebElement> lastNames;
    @FindBy(css = "[name='phoneNumber']")
    private List<WebElement> phoneNumbers;
    @FindBy(css = "[name='role']")
    private List<WebElement> rolesDropdown;
    @FindBy(css = "[class^='_multi-select-container']")
    private List<WebElement> locationDropdown;
    @FindBy(css = "[class*='_list-section-container']")
    private WebElement locationDropdownContainer;
    @FindBy(css = "[class*='_list-container']")
    private WebElement locationOptionsContainer;
    @FindBy(css = "[class*='_multi-select-list-item']")
    private List<WebElement> locationDropdownOptions;
    @FindBy(css = "[placeholder='Search Location']")
    private WebElement searchLocationInput;
    @FindBy(xpath = "*//button[text()='APPLY']")
    private WebElement applyButton;
    @FindBy(css = "[type='submit']")
    private WebElement inviteUsersButton;
    @FindBy(xpath = "*//button[text()='CANCEL']")
    private WebElement cancel;
    @FindBy(css = "[class*='_textarea-container'] span")
    private WebElement errorMessage;
    @FindBy(css = "[data-testid='popup-content']")
    private WebElement popup;
    @FindBy(css = "[data-testid='popup-close-btn']")
    private WebElement popupCloseButton;
    @FindBy(css = "[class^='_user-info-header'] button")
    private List<WebElement> removeUsersButton;

    //
    //  Invitation input part
    //
    public void addUsers(List<String> usersList){
        String result = String.join(", ", usersList);
        type(invitationInput, result);
        checkAddButtonToBeEnabled();
        click(addUserButton);
    }

    public void checkAddButtonToBeDisabled(){
        isElementDisabled(addUserButton);
    }
    public void checkAddButtonToBeEnabled(){
        isElementEnabled(addUserButton);
    }

    public void checkInviteButtonToBeDisabled(){
        isElementDisabled(inviteUsersButton);
    }
    public void checkInviteButtonToBeEnabled(){
        isElementEnabled(inviteUsersButton);
    }

    public void checkMoreThanTwentyUsersErrorMessage(){
        checkText(errorMessage, "Only 20 email addresses can be added at once");
    }

    public void checkDuplicateEmailsErrorMessage(){
        checkText(errorMessage, "Duplicate email addresses");
    }

    public void checkWrongEmailFormatErrorMessage(){
        checkText(errorMessage, "Wrong email address format");
    }

    //
    // User info fill part
    //

    public void fillAllFields(int index, String firstName, String lastName, String contactNumber, Role role, int... locationCount){
        type(firstNames.get(index), firstName);
        type(lastNames.get(index), lastName);
        type(phoneNumbers.get(index), contactNumber);
        final Select roleDropdown = new Select(rolesDropdown.get(index));
        roleDropdown.selectByValue(role.name());
        if(locationCount.length!=0){
            click(locationDropdown.get(index));
            waitForElement(locationOptionsContainer);
            for (int i = 0; i < locationCount[0]; i++) {
                click(locationDropdownOptions.get(i+1));
            }
            click(applyButton);
        }
    }


    public void fillAllFieldsWithAllLocations(int index, String firstName, String lastName, String contactNumber, Role role){
        type(firstNames.get(index), firstName);
        type(lastNames.get(index), lastName);
        type(phoneNumbers.get(index), contactNumber);
        final Select roleDropdown = new Select(rolesDropdown.get(index));
        roleDropdown.selectByValue(role.name());
        click(locationDropdown.get(index));
        waitForElement(locationOptionsContainer);
        click(locationDropdownOptions.get(0));
        click(applyButton);
    }

    public void checkInvitationCount(int invitationExpectedCount){
        checkText(inviteUsersButton, Integer.toString(invitationExpectedCount));
    }

    public void checkRoleDropdownValues(List<String> roles){
        final Select roleDropdown = new Select(rolesDropdown.get(0));
        List<WebElement> allOptions = roleDropdown.getOptions();
        for (int i = 0; i < allOptions.size(); i++) {
            checkText(allOptions.get(i), capitalize(roles.get(i)));
        }
    }

    public UsersListPage inviteUsers(){
        click(inviteUsersButton);
        return new UsersListPage(browser,version);
    }

    public UsersListPage cancelInvitations(){
        click(cancel);
        return new UsersListPage(browser,version);
    }



    private void checkHeaderMessageOfPopup(){
        final String headerMessage = "One or more email addresses can not be invited\nSee the reason below";
        checkText(popup, headerMessage);
    }

    public void checkErrorPopupOtherOrganizationUsersSupport(List<String> emails){
        checkHeaderMessageOfPopup();
        checkText(popup, "One or more emails are associated with another organization. Please change the email address and invite again.");
        for (String email: emails) {
            checkText(popup, email);
        }
        click(popupCloseButton);
    }

    public void checkErrorPopupAlreadyInvitedUsers(List<String> emails){
        checkHeaderMessageOfPopup();
        checkText(popup, "One or more emails are already invited. Please go to the employee details page and resend invitation.");
        for (String email: emails) {
            checkText(popup, email);
        }
        click(popupCloseButton);
    }

    public void checkErrorPopupDeletedUsers(List<String> emails){
        checkHeaderMessageOfPopup();
        checkText(popup, "One or more employees have been deleted.");
        for (String email: emails) {
            checkText(popup, email);
        }
        click(popupCloseButton);
    }

    public void checkErrorPopupExistingUsers(List<String> emails){
        checkHeaderMessageOfPopup();
        checkText(popup, "One or more employees have been already invited and accepted the invitation.");
        for (String email: emails) {
            checkText(popup, email);
        }
        click(popupCloseButton);
    }

    public void removeUserFromInvitationList(int index){
        click(removeUsersButton.get(index));
    }

    public void checkEmailOrderAfterAdding(List<String> expectedEmails){
        Assert.assertEquals(emails.size(), expectedEmails.size());
        for (int i =0; i<emails.size();i++) {
            checkText(emails.get(i), expectedEmails.get(i));
        }
    }

    public InvitationCreationPage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
        this.get();
    }

    public InvitationCreationPage(String browser, String version, String token) {
        super(browser, version, token);
        this.get();
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL;
    }

    @Override
    protected void load() {
        driver.get(UI_URI_CONSOLE + PAGE_URL);
    }

    @Override
    protected void isLoaded() throws Error {
        assertEquals(driver.getCurrentUrl(), UI_URI_CONSOLE + PAGE_URL);
    }
}
