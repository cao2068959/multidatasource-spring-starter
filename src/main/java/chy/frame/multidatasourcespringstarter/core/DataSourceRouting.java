package chy.frame.multidatasourcespringstarter.core;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DataSourceRouting extends AbstractRoutingDataSource {

    ThreadLocal<String> threadLocal = new ThreadLocal<>();

    //把当前事物下的连接塞入,用于事物处理
    ThreadLocal<Map<String, ConnectWarp>> connectionThreadLocal = new ThreadLocal<>();

    //这里只是留一个备份,切换数据源的时候,如果没有对应ke就直接异常,真正调用会传给AbstractRoutingDataSource处理
    //这里只读,没有线程安全问题
    Map<String, DataSource> dataSourceMap = new HashMap<>();

    @Override
    protected Object determineCurrentLookupKey() {
        String currentName = threadLocal.get();
        //没有时,拿第一个
        if (currentName == null) {
            currentName = dataSourceMap.keySet().iterator().next();
        }

        return currentName;
    }


    public DataSource getDataSource(String key) {
        return dataSourceMap.get(key);
    }


    public void changeDataSource(String name) {
        DataSource dataSource = dataSourceMap.get(name);
        if (dataSource == null) {
            throw new RuntimeException("无效的数据源");
        }
        threadLocal.set(name);
    }

    public void buildDataSouce() {
        setTargetDataSources((Map) dataSourceMap);
    }

    /**
     * 这里只是添加到 dataSourceMap,要真正可以使用,添加后调用buildDataSouce
     */
    public void addDataSouce(String name, DataSource dataSource) {
        dataSourceMap.put(name, dataSource);
    }


    /**
     * 开启事物的时候,把连接放入 线程中,后续crud 都会拿对应的连接操作
     *
     * @param key
     * @param connection
     */
    public void bindConnection(String key, Connection connection) {
        Map<String, ConnectWarp> connectionMap = connectionThreadLocal.get();
        if (connectionMap == null) {
            connectionMap = new HashMap<>();
            connectionThreadLocal.set(connectionMap);
        }

        //包装一下 不然给 spring把我关闭了
        ConnectWarp connectWarp = new ConnectWarp(connection);

        connectionMap.put(key, connectWarp);
    }


    /**
     * 提交事物
     *
     * @throws SQLException
     */
    protected void doCommit() throws SQLException {
        Map<String, ConnectWarp> stringConnectionMap = connectionThreadLocal.get();
        if (stringConnectionMap == null) {
            return;
        }
        for (String dataSourceName : stringConnectionMap.keySet()) {
            ConnectWarp connection = stringConnectionMap.get(dataSourceName);
            connection.commit(true);
            connection.close(true);
        }
        removeConnectionThreadLocal();
    }

    /**
     * 撤销事物
     *
     * @throws SQLException
     */
    protected void rollback() throws SQLException {
        Map<String, ConnectWarp> stringConnectionMap = connectionThreadLocal.get();
        if (stringConnectionMap == null) {
            return;
        }
        for (String dataSourceName : stringConnectionMap.keySet()) {
            ConnectWarp connection = stringConnectionMap.get(dataSourceName);
            connection.rollback();
            connection.close(true);
        }
        removeConnectionThreadLocal();
    }

    protected void removeConnectionThreadLocal() {
        connectionThreadLocal.remove();
    }


    /**
     * 如果 在connectionThreadLocal 中有 说明开启了事物,就从这里面拿
     *
     * @return
     * @throws SQLException
     */
    @Override
    public Connection getConnection() throws SQLException {
        Map<String, ConnectWarp> stringConnectionMap = connectionThreadLocal.get();
        if (stringConnectionMap == null) {
            //没开事物 直接走
            return determineTargetDataSource().getConnection();
        } else {
            //开了事物,从当前线程中拿,而且拿到的是 包装过的connect 只有我能关闭O__O "…
            String currentName = (String) determineCurrentLookupKey();
            return stringConnectionMap.get(currentName);
        }

    }


    public void clearThreadLocal() {
        threadLocal.remove();
    }

}
