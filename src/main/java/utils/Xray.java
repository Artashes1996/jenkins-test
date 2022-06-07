package utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Xray {
    String requirement() default "";
    String test() default "";
    String labels() default "";
}
