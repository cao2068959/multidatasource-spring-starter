package chy.frame.multidatasourcespringstarter.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TransactionMulti {

    String[] value() default {};
}
