package e2e.ui.pages;

import configuration.Config;
import configuration.Role;
import helpers.BaseUIHelper;
import helpers.SauceLabsHelper;
import helpers.flows.AuthenticationFlowHelper;
import lombok.SneakyThrows;
import org.testng.annotations.*;


public class BasePageTest {

    private static Boolean tunnelStarted = false;
    private static Process process;
    private static final Runtime runTime = Runtime.getRuntime();

    protected String browserToUse;
    protected String versionToBe;
    protected String supportToken;

    @Parameters({"browser", "version"})
    @BeforeClass
    protected void browserSetup(String browserName, String version){
        browserToUse = browserName;
        versionToBe = version;

        supportToken = new AuthenticationFlowHelper().getToken(Role.SUPPORT);
    }

    @Parameters({"browser", "version"})
    @AfterMethod(alwaysRun = true)
    protected final void tearDown(String browserName, String version) {
        BaseUIHelper.quitDriver();
    }

    @SneakyThrows
    private void startTunnelProcess() {
        final String OS = System.getProperty("os.name").toLowerCase();
        final String executablesPath = System.getProperty("user.dir")+"/src/main/resources/executables/";
        String executableFile = "sc_linux";

        if(OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
            executableFile = "sc_linux";
            process = runTime.exec("chmod +x " + executablesPath + executableFile);

        } else if (OS.contains("win")){
            executableFile = "sc.exe";

        } else if (OS.contains("mac")) {
            executableFile = "sc_osx";
            process = runTime.exec("chmod +x " + executablesPath + executableFile);

        }
        process = runTime.exec(executablesPath + executableFile + " " + Config.TUNNEL_START_PARAMS);
        Thread.sleep(60000);
    }

    @SneakyThrows
    @BeforeSuite
    public void startTunnel() {

       if(Config.REMOTE.equals("true")){
            startTunnelProcess();
            Thread.sleep(10000);
            int maxAttempts = 3;
            while(!SauceLabsHelper.isTunnelRunning(Config.TUNNEL_IDENTIFIER) && maxAttempts > 0) {
                Thread.sleep(10000);
                maxAttempts--;
            }
            if(SauceLabsHelper.isTunnelRunning(Config.TUNNEL_IDENTIFIER)){
                tunnelStarted = true;
                System.out.println("!!! TUNNEL IS ACTIVE !!!");
            }
        }
    }

    @SneakyThrows
    @AfterSuite(alwaysRun = true)
    public void stopTunnel() {
        if(tunnelStarted) {
            SauceLabsHelper.deleteTunnel(Config.TUNNEL_IDENTIFIER);
            process.destroy();
            System.out.println("!!! TUNNEL IS CLOSED !!!");
        }
        tunnelStarted = false;
    }
}