package chy.frame.multidatasourcespringstarter.core.transaction;

import chy.frame.multidatasourcespringstarter.core.ConnectWarp;
import chy.frame.multidatasourcespringstarter.enums.TransactionTypeEnum;
import org.apache.ibatis.session.SqlSessionException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 每一次事务都会有一个 TransactionCarrier
 */
public class TransactionCarrier {

    /**
     * 本次参与了事务的所有 connect
     */
    private Map<String, Connection> transactionConnects = new HashMap<>();

    private int transactionType;

    public TransactionCarrier(int transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * 新加入一个 connect 来参与事物
     *
     * @param name
     * @param connection
     * @throws SQLException
     */
    public void addTransactionConnect(String name, Connection connection) throws SQLException {
        //相同的数据源已经有connect正在参与事物了,那么就不再去添加了
        if (transactionConnects.containsKey(name)) {
            return;
        }
        //设置隔离级别
        prepareTransactionalConnection(connection);
        //去开启事物
        connectBegin(connection);
        //把这个connect缓存
        transactionConnects.put(name, connection);
    }

    public Optional<Connection> getConnect(String name) {
        return Optional.ofNullable(transactionConnects.get(name));
    }


    /**
     * 设置隔离级别
     *
     * @param con
     * @throws SQLException
     */
    protected void prepareTransactionalConnection(Connection con)
            throws SQLException {
        if (TransactionTypeEnum.isNotDefined(transactionType)) {
            throw new SqlSessionException("当前事物隔离级别未被定义");
        }
        con.setTransactionIsolation(transactionType);
    }


    /**
     * 开启事物的一些准本工作
     */
    private void connectBegin(Connection connection) throws SQLException {
        if (connection.getAutoCommit()) {
            connection.setAutoCommit(false);
        }
    }



    /**
     * 提交事物
     */
    public void commitTransaction() throws SQLException {
        for (Map.Entry<String, Connection> connectionEntry : transactionConnects.entrySet()) {
            Connection connection = connectionEntry.getValue();
            if (!(connection instanceof ConnectWarp)){
                continue;
            }
            ConnectWarp connectWarp = (ConnectWarp) connection;
            connectWarp.commit(true);
            connectWarp.close(true);
        }
    }

    public void rollbackTransaction() throws SQLException {
        for (Map.Entry<String, Connection> connectionEntry : transactionConnects.entrySet()) {
            Connection connection = connectionEntry.getValue();
            if (!(connection instanceof ConnectWarp)){
                continue;
            }
            ConnectWarp connectWarp = (ConnectWarp) connection;
            connectWarp.rollback();
            connectWarp.close(true);
        }

    }
}
