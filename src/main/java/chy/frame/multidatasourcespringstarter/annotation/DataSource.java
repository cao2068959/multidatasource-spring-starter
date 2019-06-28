package chy.frame.multidatasourcespringstarter.annotation;

import java.lang.annotation.*;

/**
 * 指定方法、接口使用的数据源
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface DataSource {
    String value() default "";
}
