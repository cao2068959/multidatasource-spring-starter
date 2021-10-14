package chy.frame.multidatasourcespringstarter.core;

import chy.frame.multidatasourcespringstarter.core.transaction.SqlTransactionException;
import chy.frame.multidatasourcespringstarter.core.transaction.TransactionCarrier;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DataSourceRouting extends AbstractRoutingDataSource {

    ThreadLocal<String> currentDataSourceKey = new ThreadLocal<>();

    //把当前事物下的连接塞入,用于事物处理
    ThreadLocal<TransactionCarrier> transactionCarrierThreadLocal = new ThreadLocal<>();

    //这里只是留一个备份,切换数据源的时候,如果没有对应key就直接异常,真正调用会传给AbstractRoutingDataSource处理
    //这里只读,没有线程安全问题
    Map<String, DataSource> dataSourceMap = new HashMap<>();

    @Override
    protected Object determineCurrentLookupKey() {
        String currentName = currentDataSourceKey.get();
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
        currentDataSourceKey.set(name);
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
     * 提交事物
     *
     * @throws SQLException
     */
    public void commitTransaction() throws SQLException {
        getCurrentTransactionCarrier().orElseThrow(() -> new SqlTransactionException("当前线程中事物没有开启"))
                .commitTransaction();
        //提交事物后清理释放资源
        clearTransaction();
    }


    /**
     * 撤销事物
     *
     * @throws SQLException
     */
    public void rollbackTransaction() throws SQLException {
        getCurrentTransactionCarrier().orElseThrow(() -> new SqlTransactionException("当前线程中事物没有开启"))
                .rollbackTransaction();
        //提交事物后清理释放资源
        clearTransaction();
    }

    protected void clearTransaction() {
        transactionCarrierThreadLocal.remove();
    }


    /**
     * 如果 在connectionThreadLocal 中有 说明开启了事物,就从这里面拿
     *
     * @return
     * @throws SQLException
     */
    @Override
    public Connection getConnection() throws SQLException {
        Optional<TransactionCarrier> currentTransactionCarrier = getCurrentTransactionCarrier();
        if (currentTransactionCarrier.isPresent()) {
            TransactionCarrier transactionCarrier = currentTransactionCarrier.get();
            //开了事物 那么从 currentTransactionCarrier中去获取对应的 connect;
            String currentName = (String) determineCurrentLookupKey();

            Optional<Connection> transactionConnect = transactionCarrier.getConnect(currentName);
            //使用了已经开启了事务的connect;
            if (transactionConnect.isPresent()) {
                return transactionConnect.get();
            }
            //开启事物后第一次获取connect, 那么先获取一个新的 connect
            Connection connection = new ConnectWarp(determineTargetDataSource().getConnection());
            //把新获取到的 connection 放入 transactionCarrier中,后续再次获取就能直接拿到
            transactionCarrier.addTransactionConnect(currentName, connection);
            return connection;
        } else {
            //没开事物 直接走
            return determineTargetDataSource().getConnection();
        }
    }

    private Optional<TransactionCarrier> getCurrentTransactionCarrier() {
        return Optional.ofNullable(transactionCarrierThreadLocal.get());
    }

    public void clearThreadLocal() {
        currentDataSourceKey.remove();
    }

    /**
     * 当前线程开启了事务
     *
     * @param transactionType
     */
    public void beginTransaction(int transactionType) {
        Optional<TransactionCarrier> currentTransactionCarrier = getCurrentTransactionCarrier();
        //已经开启过了,不在重复开启了
        if (currentTransactionCarrier.isPresent()) {
            return;
        }
        TransactionCarrier transactionCarrier = new TransactionCarrier(transactionType);
        transactionCarrierThreadLocal.set(transactionCarrier);
    }


}
