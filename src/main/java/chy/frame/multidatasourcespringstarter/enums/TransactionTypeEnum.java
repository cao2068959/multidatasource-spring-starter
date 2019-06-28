package chy.frame.multidatasourcespringstarter.enums;

public enum TransactionTypeEnum {

    TRANSACTION_NONE(0,"无事务"),
    TRANSACTION_READ_UNCOMMITTED(1,"允许读脏，不可重读，幻读"),
    TRANSACTION_READ_COMMITTED(2,"仅允许读取已提交的数据，即不能读脏，但是可能发生不可重读和幻读"),
    TRANSACTION_REPEATABLE_READ(4,"不可读脏，保证同一事务重复读取相同数据，但是可能发生幻读"),
    TRANSACTION_SERIALIZABLE(8,"串行事务，保证不读脏，可重复读，不可幻读");

    private int value;

    private String details;

    TransactionTypeEnum() {
    }

    TransactionTypeEnum(int value, String details) {
        this.value = value;
        this.details = details;
    }

    public static boolean isNotDefined(Integer value) {
        TransactionTypeEnum[] transactionTypeEnums = values();
        for (TransactionTypeEnum transactionTypeEnum : transactionTypeEnums) {
            if (transactionTypeEnum.getValue()==value) {
                return false;
            }
        }
        return true;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
