package chy.frame.multidatasourcespringstarter.properties;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import java.util.Map;

public class DataSourceExtentProperties extends DataSourceProperties {

    private Map<String,Object> pool;

    public Map<String, Object> getPool() {
        return pool;
    }

    public void setPool(Map<String, Object> pool) {
        this.pool = pool;
    }
}
