package chy.frame.multidatasourcespringstarter.autoConfig;


import chy.frame.multidatasourcespringstarter.core.DataSourceChangeAop;
import chy.frame.multidatasourcespringstarter.core.DataSourceRouting;
import chy.frame.multidatasourcespringstarter.core.transaction.MultiTransactionManagerAop;
import chy.frame.multidatasourcespringstarter.properties.DataSourceExtentProperties;
import chy.frame.multidatasourcespringstarter.properties.MultiDataSourceProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(MultiDataSourceProperties.class)
public class MultiDataSourceAutoConfig {

    @Autowired
    MultiDataSourceProperties multiDataSourceProperties;


    @Bean
    public DataSource multiDataSource() {
        //真正用来切换数据源的哥们
        DataSourceRouting dataSourceRouting = new DataSourceRouting();
        //拿到配置的所有配置信息
        Map<String, DataSourceExtentProperties> dataSourcePropertiesMap = multiDataSourceProperties.getDataSource();
        for (String dataSourceName : dataSourcePropertiesMap.keySet()) {
            DataSourceExtentProperties dataSourceProperties = dataSourcePropertiesMap.get(dataSourceName);
            //根据配置文件生成dataSource
            DataSource dataSource = dataSource(dataSourceProperties);
            //塞入
            dataSourceRouting.addDataSouce(dataSourceName, dataSource);
            //设置一些连接池的额外值
            setExProperties(dataSource, dataSourceProperties);
        }
        dataSourceRouting.setDefaultDataSource(multiDataSourceProperties.getDefaultDataSource());
        //把数据源的压入AbstractRoutingDataSource
        dataSourceRouting.buildDataSouce();


        return dataSourceRouting;
    }


    @Bean
    public DataSourceChangeAop changeAop() {
        DataSourceChangeAop dataSourceChangeAop = new DataSourceChangeAop();
        return dataSourceChangeAop;
    }

    @Bean
    public MultiTransactionManagerAop multiTransactionManagerAop() {
        MultiTransactionManagerAop multiTransactionManagerAop = new MultiTransactionManagerAop();
        return multiTransactionManagerAop;
    }


    private DataSource dataSource(DataSourceProperties properties) {
        DataSourceBuilder factory = DataSourceBuilder
                .create(properties.getClassLoader())
                .driverClassName(properties.getDriverClassName())
                .url(properties.getUrl()).username(properties.getUsername())
                .password(properties.getPassword());
        if (properties.getType() != null) {
            factory.type(properties.getType());
        }
        return factory.build();
    }

    /**
     * 使用反射把一些额外的配置设置进 DataSource 比如 druid dbcp 的一些额外配置线程池最大值等参数
     */
    private void setExProperties(DataSource dataSource, DataSourceExtentProperties dataSourceProperties) {

        Map<String, Object> pool = dataSourceProperties.getPool();
        if (pool == null) {
            return;
        }
        Class<? extends DataSource> dataSourceClass = dataSource.getClass();
        Map<String, PropertyDescriptor> descriptorMap = Arrays.stream(BeanUtils.getPropertyDescriptors(dataSourceClass)).collect(Collectors.toMap(PropertyDescriptor::getName, s -> s));

        for (String key : pool.keySet()) {
            Object value = pool.get(key);
            try {
                //把key转驼峰
                String newKey = keyConver(key);
                PropertyDescriptor propertyDescriptor = descriptorMap.get(newKey);
                if (propertyDescriptor == null) {
                    System.err.println(String.format("pool.%s = %s 没有正确映射 请检查参数是否正确", key, value));
                    continue;
                }
                Method writeMethod = propertyDescriptor.getWriteMethod();
                if (writeMethod == null) {
                    System.err.println(String.format("pool.%s = %s 不能设置对应的值", key, value));
                    continue;
                }
                writeMethod.setAccessible(true);
                writeMethod.invoke(dataSource, typeHandler(value, propertyDescriptor.getPropertyType()));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 反射的时候进行类型处理，如：value其实是 "2", field 期望的类型是 int
     *
     * @param value
     * @return
     */
    private Object typeHandler(Object value, Class<?> type) {
        if (value == null) {
            return value;
        }
        if (value.getClass() == type) {
            return value;
        }
        if (Integer.class == type || int.class == type) {
            return Integer.parseInt(value.toString());
        }

        if (Long.class == type || long.class == type) {
            return Long.parseLong(value.toString());
        }

        if (Boolean.class == type || boolean.class == type) {
            return Boolean.parseBoolean(value.toString());
        }
        return value;
    }

    /**
     * 反射扫描属性,包括父类的
     *
     * @return
     */
    private Field getField(Class<? extends DataSource> dataSourceClass, String key) {
        try {
            Field field = dataSourceClass.getDeclaredField(key);
            return field;
        } catch (NoSuchFieldException e) {

        }
        Class<?> superclass = dataSourceClass.getSuperclass();
        try {
            return superclass.getDeclaredField(key);
        } catch (NoSuchFieldException e) {

        }
        return null;
    }


    private String keyConver(String data) {
        final StringBuffer sb = new StringBuffer();
        Pattern p = Pattern.compile("-(\\w)");
        Matcher m = p.matcher(data);
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase());
        }
        m.appendTail(sb);
        return sb.toString();
    }


}
