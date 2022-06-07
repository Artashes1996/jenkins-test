package helpers;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.SneakyThrows;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.util.Date;

import static configuration.Config.*;

public class BaseUIHelper {

    private static final ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();

    @SneakyThrows
    public static WebDriver getDriver(String browser, String version) {

        if (driverThread.get() == null) {
            if (REMOTE.equals("true")) {
                final DesiredCapabilities caps = new DesiredCapabilities();
                caps.setBrowserName(browser);
                caps.setPlatform(Platform.ANY);
                caps.setVersion(version);
                caps.setCapability("sauce:options", getSauceLabsConfigs());
                driverThread.set(new RemoteWebDriver(new URL(HUB_URL), caps));

            } else {
                WebDriverManager.chromedriver().setup();
                driverThread.set(new ChromeDriver());
                driverThread.get().manage().window().maximize();
            }
        }

        return driverThread.get();
    }

    public static void quitDriver() {
        driverThread.get().quit();
        driverThread.remove();
    }
    
    public static MutableCapabilities getSauceLabsConfigs() {

        final MutableCapabilities sauceOptions = new MutableCapabilities();
        sauceOptions.setCapability("screenResolution", "1280x800");
        sauceOptions.setCapability("name", "Pegasus Regression Test: " + new Date());
        sauceOptions.setCapability("tunnelIdentifier", TUNNEL_IDENTIFIER);
        sauceOptions.setCapability("screenResolution", "1280x800");

        return sauceOptions;
    }

}