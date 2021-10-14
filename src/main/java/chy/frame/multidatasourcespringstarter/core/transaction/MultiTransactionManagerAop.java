package chy.frame.multidatasourcespringstarter.core.transaction;

import chy.frame.multidatasourcespringstarter.annotation.TransactionMulti;
import chy.frame.multidatasourcespringstarter.core.DataSourceRouting;
import chy.frame.multidatasourcespringstarter.enums.TransactionTypeEnum;
import org.apache.ibatis.session.SqlSessionException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Aspect
public class MultiTransactionManagerAop {


    @Autowired
    DataSourceRouting dataSourceRouting;


    @Pointcut("@annotation(chy.frame.multidatasourcespringstarter.annotation.TransactionMulti)")
    public void annotationPointcut() {
    }


    @Around("annotationPointcut()")
    public void roundExecute(ProceedingJoinPoint joinpoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinpoint.getSignature();
        Method method = methodSignature.getMethod();
        TransactionMulti annotation = method.getAnnotation(TransactionMulti.class);
        int transactionType = annotation.transactionType();
        //开启事务
        dataSourceRouting.beginTransaction(transactionType);
        //正真执行了 方法
        joinpoint.proceed();
        //提交事务
        dataSourceRouting.commitTransaction();
    }

    @AfterThrowing(pointcut = "annotationPointcut()", throwing = "e")
    public void handleThrowing(JoinPoint joinPoint, Exception e) {//controller类抛出的异常在这边捕获
        try {
            //回滚事物
            dataSourceRouting.rollbackTransaction();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }

}
