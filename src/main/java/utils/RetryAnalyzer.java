package utils;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryAnalyzer implements IRetryAnalyzer {

    int count = 1;
    final int maxRetryCount=2;

    @Override
    public boolean retry(ITestResult iTestResult) {
        if(count < maxRetryCount){
            count++;
            return true;
        }
        return false;
    }
}
