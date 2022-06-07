package utils;

import lombok.SneakyThrows;

import java.util.function.Supplier;

public class DbWaitUtils {

    @SneakyThrows
    public static void waitForUserStateToBe(final Supplier<Object> supplier, Object expectedCondition) {
        int integerThreadLocal = 8;

        while (integerThreadLocal > 0) {
            if (supplier.get().equals(expectedCondition)) {
                return;
            }
            integerThreadLocal--;
            Thread.sleep(3000);
        }
    }
}
