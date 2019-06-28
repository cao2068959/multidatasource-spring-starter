package chy.frame.multidatasourcespringstarter.annotation;

import java.lang.annotation.*;
import java.sql.Connection;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TransactionMulti {
    String[] value() default {};
    int transactionType() default Connection.TRANSACTION_READ_UNCOMMITTED;
}
