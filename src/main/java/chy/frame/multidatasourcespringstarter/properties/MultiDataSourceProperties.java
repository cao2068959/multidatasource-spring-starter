package chy.frame.multidatasourcespringstarter.properties;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix="chy.multi")
public class MultiDataSourceProperties {

    Map<String,DataSourceExtentProperties> dataSource;

    public Map<String, DataSourceExtentProperties> getDataSource() {
        return dataSource;
    }

    public void setDataSource(Map<String, DataSourceExtentProperties> dataSource) {
        this.dataSource = dataSource;
    }
}
