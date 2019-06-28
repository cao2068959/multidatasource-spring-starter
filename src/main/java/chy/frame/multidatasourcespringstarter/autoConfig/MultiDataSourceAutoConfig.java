package chy.frame.multidatasourcespringstarter.autoConfig;


import chy.frame.multidatasourcespringstarter.core.DataSourceChangeAop;
import chy.frame.multidatasourcespringstarter.core.DataSourceRouting;
import chy.frame.multidatasourcespringstarter.core.MultiTransactionManagerAop;
import chy.frame.multidatasourcespringstarter.properties.DataSourceExtentProperties;
import chy.frame.multidatasourcespringstarter.properties.MultiDataSourceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableConfigurationProperties(MultiDataSourceProperties.class)
public class MultiDataSourceAutoConfig {

    @Autowired
    MultiDataSourceProperties multiDataSourceProperties;


    @Bean
    public DataSource multiDataSource() {
        boolean setDefaultFlag = true;
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

            System.out.println("-");
        }
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
        for (String key : pool.keySet()) {
            Object value = pool.get(key);
            Class<? extends DataSource> dataSourceClass = dataSource.getClass();
            try {
                //把key转驼峰
                String newKey = keyConver(key);
                Field field = getField(dataSourceClass, newKey);
                if (field == null) {
                    System.err.println(String.format("pool.%s = %s 没有正确映射 请检查参数是否正确", key, value));
                    continue;
                }
                field.setAccessible(true);
                field.set(dataSource, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
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
