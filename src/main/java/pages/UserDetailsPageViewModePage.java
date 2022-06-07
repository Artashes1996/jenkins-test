package pages;

import org.json.JSONObject;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;


import java.util.List;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.*;
import static utils.TestUtils.*;

public class UserDetailsPageViewModePage extends BasePage<UserDetailsPageViewModePage> {

    private static final String PAGE_URL = UI_URI_CONSOLE + "/company/user/";

    @FindBy(css = "[data-testid='user-full-name']")
    private WebElement userFullNameElement;
    @FindBy(css = "[data-testid='user-status']")
    private WebElement userStatusElement;
    @FindBy(css = "[data-testid='edit-button']")
    private WebElement editButtonElement;
    @FindBy(css = "[data-testid='first-name']")
    private WebElement firstNameElement;
    @FindBy(css = "[data-testid='last-name']")
    private WebElement lastNameElement;
    @FindBy(css = "[data-testid='user-email']")
    private WebElement emailElement;
    @FindBy(css = "[data-testid='user-role']")
    private WebElement userRoleElement;
    @FindBy(css = "[data-testid='user-contact-number']")
    private WebElement contactNumberElement;
    @FindBy(css = "[data-testid='user-contact-person']")
    private WebElement contactPersonIndicationElement;
    @FindBy(css = "[data-testid='resend-invitation-button']")
    private WebElement resendInvitationElement;
    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToastElement;
    @FindBy(css = "[data-testid^='breadcrumb-item']")
    private List<WebElement> breadCrumbItems;


    public UserDetailsPageViewModePage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public UserDetailsPageViewModePage(String browser, String version) {
        super(browser, version);
    }

    public UserDetailsPageViewModePage(String browser, String version, String token) {
        super(browser, version, token);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL + userToEnter.get().getString("id") + "/details";
    }

    @Override
    protected void load() {
        driver.get(getPageUrl());
    }

    @Override
    protected void isLoaded() throws Error {
        assertEquals(driver.getCurrentUrl(), getPageUrl());
        waitForElement(userStatusElement);
    }

    public void checkUserDetails(final JSONObject user) {
        final String firstName = user.getString("firstName");
        final String lastName = user.getString("lastName");
        final String fullName = firstName + " " + lastName;
        final String email = user.getString("email");
        final String contactNumber = user.getString("contactNumber");

        String role = capitalize(user.getString("role"));
        role = role.replace("/", " ");

        waitForElement(firstNameElement);

        checkText(firstNameElement, firstName);
        checkText(lastNameElement, lastName);
        checkText(userFullNameElement, fullName);
        checkText(emailElement, email);
        scrollToElement(contactNumberElement);
        checkText(contactNumberElement, contactNumber);
        checkText(userRoleElement, role);
    }

    public void checkRole(String role) {
        waitForElement(userRoleElement);
        checkText(userRoleElement, role);
    }

    public void checkContactPerson() {
        isElementEnabled(contactPersonIndicationElement);
    }

    public void checkUserStatusIs(String status) {
        waitForElement(userStatusElement);
        checkText(userStatusElement, status);
    }

    public void resendInvitation() {
        waitForElement(resendInvitationElement);
        final String email = emailElement.getText();
        resendInvitationElement.click();
        waitForElement(successToastElement);
        final String resendInvitationToastMessage = "The invitation has been resent to " + email;
        checkText(successToastElement, resendInvitationToastMessage);
    }

    public void checkBreadcrumb(JSONObject user) {
        assertEquals(breadCrumbItems.size(), 2);
        checkText(breadCrumbItems.get(0), "Users");
        final String userFullName = user.getString("firstName")+" "+user.getString("lastName");
        checkText(breadCrumbItems.get(1), userFullName);
        click(breadCrumbItems.get(0));
        assertTrue(driver.getCurrentUrl().contains("users/list"));
    }

    public void checkCannotEdit() {
        waitForElement(userStatusElement);
        boolean canEdit = true;
        try {
            editButtonElement.isEnabled();
        } catch (NoSuchElementException ignore) {
            canEdit = false;
        }
        assertFalse(canEdit);
    }

}
