package chy.frame.multidatasourcespringstarter.core.transaction;

public class SqlTransactionException extends RuntimeException {

    public SqlTransactionException(String message) {
        super(message);
    }
}
