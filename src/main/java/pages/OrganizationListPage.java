package pages;

import helpers.BaseUIHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static configuration.Config.UI_URI_BACK_OFFICE;
import static org.testng.Assert.*;
import static utils.TestUtils.capitalize;

public class OrganizationListPage extends BasePage<OrganizationListPage> {

    public enum Actions {
        BLOCK("Block"),
        UNBLOCK("Unblock"),
        PUBLISH("Publish"),
        PAUSE("Pause"),
        UNPAUSE("Unpause");

        private final String action;

        Actions(String action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return action;
        }
    }

    private final String PAGE_URL = UI_URI_BACK_OFFICE + "/organizations/list";

    //    header part
    @FindBy(css = "[data-testid='profile-action-menu']")
    private WebElement avatar;
    @FindBy(css = "[data-testid='action-menu-list']")
    private WebElement accountBox;
    @FindBy(css = "[data-testid$='logout']")
    private WebElement logOut;

    @FindBy(css = "[data-testid='organization-name']")
    private List<WebElement> organizationNames;
    @FindBy(css = "[data-testid='organization-status']")
    private List<WebElement> organizationStatuses;
    @FindBy(css = "[data-testid='organization-vertical']")
    private List<WebElement> organizationVertical;
    @FindBy(css = "[data-testid='employee-count']")
    private List<WebElement> employeeCount;
    @FindBy(css = "[data-testid='popup-close-btn']")
    private WebElement closeIconElement;
    @FindBy(css = "[data-testid='locations-count']")
    private List<WebElement> locationCount;
    @FindBy(css = "[class^='_organization-image']")
    private List<WebElement> companyLogo;
    @FindBy(css = "[data-testid='organizations-actions']")
    private List<WebElement> threeDotIconElements;
    @FindBy(css = "[data-testid='popup-content-container']")
    private WebElement publishOrganizationPopupElement;
    @FindBy(css = "[data-testid='actionMenuItem_1']")
    private WebElement publishButtonElement;
    @FindBy(xpath = "//div[@data-testid='organizations-actions']/ul/li")
    private List<WebElement> organizationActionsElements;
    @FindBy(css = "[data-testid^='actionMenuItem']")
    private List<WebElement> organizationActionMenuItems;
    @FindBy(css = "[data-testid='cancel']")
    private WebElement cancelButtonPopupElement;
    @FindBy(css = "[data-testid='confirm']")
    private WebElement confirmButtonPopupElement;
    @FindBy(css = "[data-testid='confirm']")
    private WebElement blockButtonPopupElement;
    @FindBy(css = "[data-testid='popup-content']")
    private WebElement popupContentElement;
    @FindBy(css = "[data-testid='sort-INTERNAL_NAME']")
    private WebElement sortingCompanyName;
    @FindBy(css = "[data-testid='sort-NUMBER_OF_USERS']")
    private WebElement sortingUserCount;
    @FindBy(css = "[data-testid='sort-NUMBER_OF_LOCATIONS']")
    private WebElement sortingLocationCount;
    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement dangerToastElement;
    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToastElement;
    @FindBy(css = "[data-testid='paginationContent']")
    private WebElement paginationText;
    @FindBy(css = "[data-testid='organization-search']")
    private WebElement organizationSearchInput;
    @FindBy(css = "[data-testid='paginationPrevButton']")
    private WebElement previousButton;
    @FindBy(css = "[data-testid='paginationNextButton']")
    private WebElement nextButton;
    @FindBy(css = "[data-testid='block-organization-popup']")
    private WebElement blockOrganizationPopup;
    @FindBy(xpath = "//*[@id='blockUnblockForm']/div/label[*]")
    private List<WebElement> blockUnblockOptionsLabels;
    @FindBy(css = "[data-testid='block-organization-option']")
    private List<WebElement> blockOptions;
    @FindBy(css = "#blockUnblockForm")
    private WebElement blockUnblockForm;
    @FindBy(css = "[data-testid='block-organization-option-other']")
    private WebElement otherReasonTextArea;
    @FindBy(css = "[data-testid='block-organization-option-other-error']")
    private WebElement blockUnblockErrorMsg;
    @FindBy(css = "[data-testid='popup-title-container']")
    private WebElement popupTitle;

    public void checkOrganizationCount(int count) {
        isElementEnabled(paginationText);
        final String[] pieces = paginationText.getText().split(" ");
        assertEquals(pieces[pieces.length - 1], String.valueOf(count));
        isElementDisabled(previousButton);

        if (count > 50) {
            isElementEnabled(nextButton);
            click(nextButton);
            isElementEnabled(previousButton);
        } else isElementDisabled(previousButton);
    }

    public void checkOrganizationInList(int index, String organizationName, String status, String vertical) {

        waitForElements(organizationNames);
        checkText(organizationNames.get(index), organizationName);
        checkText(organizationStatuses.get(index), capitalize(status));
        checkText(organizationVertical.get(index), capitalize(vertical));
    }

    public void sortByUserCount() {
        click(sortingUserCount);
    }

    public void sortByLocationCount() {
        click(sortingLocationCount);
    }

    public OrganizationListPage(String browser, String version, String token) {
        super(browser, version, token);
    }

    public OrganizationListPage(String browser, String version) {
        super(browser, version);
    }

    public void clickOnNextButton() {
        click(nextButton);
    }

    public void checkNextButtonEnabled() {
        isElementEnabled(nextButton);
    }

    public void checkPreviousButtonDisabled() {
        isElementDisabled(previousButton);
    }

    public OrganizationListPage searchForOrganization(String organizationSearchKey) {
        type(organizationSearchInput, organizationSearchKey);
        waitForElements(threeDotIconElements);
        return new OrganizationListPage(browser, version);
    }

    public OrganizationListPage unPauseOrganization() {
        clickThreeDotItemActionsMenu();
        click(findElementByText(organizationActionsElements, Actions.UNPAUSE.toString()));
        return new OrganizationListPage(browser, version);
    }

    public void clickOnCloseIcon() {
        click(closeIconElement);
    }

    public OrganizationListPage pauseOrganization() {
        clickThreeDotItemActionsMenu();
        click(findElementByText(organizationActionsElements, Actions.PAUSE.toString()));
        return new OrganizationListPage(browser, version);
    }

    public void confirmPauseOperation(String organizationName) {
        checkText(popupContentElement, organizationName + "\n" +
                "If you proceed, employees and customers will not be able to continue using the platform. " +
                "Are you sure you want to proceed? Please note that this action will not affect on billing.");
        click(confirmButtonPopupElement);
    }

    public void checkSuccessToastForAnPauseOrganization() {
        checkText(successToastElement, "Your organization is currently paused.");
    }

    public void checkSuccessToastForUnblockOrganization() {
        checkText(successToastElement, "Organization has been unblocked successfully.");
    }

    public void checkSuccessToastForBlockOrganization() {
        checkText(successToastElement, "Organization has been blocked successfully.");
    }

    public OrganizationListPage publishOrganization() {
        clickThreeDotItemActionsMenu();
        click(publishButtonElement);
        return new OrganizationListPage(browser, version);
    }

    public void confirmPublishOperation(String organizationName) {
        checkText(popupContentElement, organizationName + "\nThis action will send a welcome email to all owner accounts." +
                " Are you sure you want to proceed?");
        click(confirmButtonPopupElement);
    }

    public void clickOnActionMenuItem(Actions action) {
        waitForElements(organizationActionMenuItems);
        click(findElementByText(organizationActionMenuItems, action.toString()));
    }

    public void checkBlockingPopupContent(String organizationName) {
        checkText(popupContentElement, organizationName + "\nIf you block " + organizationName + ", all the accounts will only have restricted access to the system.\n" +
                "\n" + "Are you sure you want to proceed?\n" +
                "\n" + "If yes, please select a reason for blocking. This will be displayed to the organization owners and admins.");
        checkText(popupTitle, "Block organization");
        waitForElements(blockUnblockOptionsLabels);
        assertNotNull(blockUnblockOptionsLabels.get(0).getText(), "Lack of Payment");
        assertNotNull(blockUnblockOptionsLabels.get(1).getText(), "Other");

    }

    public void checkUnblockingPopupContent(String organizationName) {
        checkText(popupContentElement, organizationName + "\nIf you Unblock " +
                organizationName + ", all the accounts will regain full access to the system within their permissions.\n" +
                "\n" + "Are you sure you want to proceed?\n" +
                "\n" + "If yes, please select a reason for activating.");
        checkText(popupTitle, "Unblock organization");
        waitForElements(blockUnblockOptionsLabels);
        assertNotNull(blockUnblockOptionsLabels.get(0).getText(), "Contract signed");
        assertNotNull(blockUnblockOptionsLabels.get(1).getText(), "Payment received");
        assertNotNull(blockUnblockOptionsLabels.get(2).getText(), "Other");
    }

    public void checkThatFirstRadioButtonIsSelected() {
        isElementSelected(blockOptions.get(0));
    }

    public void clickThreeDotItemActionsMenu() {
        waitForElements(threeDotIconElements);
        click(threeDotIconElements.get(0).findElement(By.id("Capa_1")));
    }

    public void clickCancelFromPopup() {
        click(cancelButtonPopupElement);
    }

    public void clickConfirmFromPopup() {
        click(blockButtonPopupElement);
    }

    public void checkRequiredOtherFieldError() {
        checkText(blockUnblockErrorMsg, "This field is required.");
    }

    public void clickOnPaymentReceived() {
        waitForElement(blockUnblockForm);
        click(findElementByText(blockUnblockOptionsLabels, "Payment received"));
    }

    public void clickOnOtherReason() {
        waitForElement(blockUnblockForm);
        click(findElementByText(blockUnblockOptionsLabels, "Other"));
    }

    public void typeReason(String reasonText) {
        waitForElement(blockUnblockForm);
        click(otherReasonTextArea);
        type(otherReasonTextArea, reasonText);
    }

    public void cancelPausedOrganization() {
        clickThreeDotItemActionsMenu();
        click(findElementByText(organizationActionsElements, Actions.PAUSE.toString()));
    }

    public void cancelPublishingOrganization() {
        clickThreeDotItemActionsMenu();
        click(publishButtonElement);
    }

    public void cancelPublishOperation(String organizationName) {
        checkText(popupContentElement, organizationName + "\nThis action will send a welcome email to all owner accounts." +
                " Are you sure you want to proceed?");
        click(cancelButtonPopupElement);
    }

    public void checkNoPopupIsDisplayed() {
        isElementInvisible(publishOrganizationPopupElement);
    }

    public void checkOrganizationStatus(String status) {
        waitForElements(organizationStatuses);
        checkText(organizationStatuses.get(0), capitalize(status));
    }

    public void checkOrganizationStatus(String organizationName, String status) {
        searchForOrganization(organizationName);
        waitForElements(organizationStatuses);
        checkText(organizationStatuses.get(0), capitalize(status));
    }

    public void checkErrorToastForAnActiveOwnerAccount() {
        checkText(dangerToastElement, "SORRY :(\n" +
                "The organization does not have an active owner account.");
    }

    public void checkErrorToastForAnActivePOC() {
        checkText(dangerToastElement, "SORRY :(\n" +
                "The organization does not have an active point of contact.");
    }

    public void checkSuccessToastForAnUnPauseOrganization() {
        checkText(successToastElement, "CONGRATULATIONS!\n" +
                "The organization is live.");
    }

    public void logOut() {
        click(avatar);
        waitForElement(accountBox);
        click(logOut);
    }

    public void clickBrowserBack() {
        driver.navigate().back();
    }

    public void checkLoggedOut() {
        wait.until(ExpectedConditions.urlContains("signin"));
        assertNotNull(driver.manage().getCookieNamed("token"));
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL;
    }

    @Override
    protected void load() {
        BaseUIHelper.getDriver(browser, version).get(getPageUrl());
    }

    @Override
    protected void isLoaded() throws Error {
        assertTrue(BaseUIHelper.getDriver(browser, version).getCurrentUrl().contains(getPageUrl()));
        waitForElements(organizationNames);
    }

}