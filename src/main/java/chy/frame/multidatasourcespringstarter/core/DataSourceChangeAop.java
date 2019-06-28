package chy.frame.multidatasourcespringstarter.core;

import chy.frame.multidatasourcespringstarter.annotation.DataSource;
import chy.frame.multidatasourcespringstarter.utils.AopTargetUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
public class DataSourceChangeAop {

    @Autowired
    private DataSourceRouting dataSourceRouting;

    @Pointcut("@annotation(chy.frame.multidatasourcespringstarter.annotation.DataSource)")
    public void annotationPointcut(){}

    @Pointcut("this(chy.frame.multidatasourcespringstarter.annotation.JpaMulti)")
    public void interfacePoint(){}

    @Before("annotationPointcut()")
    public void beforMethod(JoinPoint point){
        Signature signature = point.getSignature();
        if (signature instanceof MethodSignature){
            //获取当前方法的签名
            MethodSignature methodSignature = (MethodSignature)signature ;
            //获取当前方法
            Method method = methodSignature.getMethod();
            //获取指定注解的引用
            DataSource annotation = method.getAnnotation(DataSource.class);
            //获取参数，使用的数据源
            String value = annotation.value();
            //切换了数据源
            dataSourceRouting.changeDataSource(value);
        }else {
            Class<?> target = point.getTarget().getClass();
            DataSource annotation = target.getAnnotation(DataSource.class);
            String value = annotation.value();
            dataSourceRouting.changeDataSource(value);
        }
    }

    @Before("interfacePoint()")
    public void interfacePointBefore(JoinPoint point) throws Exception {
        //获取被代理对象上所有的接口
        Class<?>[] interfaces = point.getTarget().getClass().getInterfaces();

        //扫描上面的DataSource 注解
        for (Class<?> anInterface : interfaces) {
            DataSource annotation = anInterface.getAnnotation(DataSource.class);
            if(annotation == null){
                continue;
            }

            String value = annotation.value();
            //切换了数据源
            dataSourceRouting.changeDataSource(value);
            return;

        }
        //dataSourceRouting.changeDataSource(value);
    }


    @After("annotationPointcut() ||interfacePoint() ")
    public void after(JoinPoint point) throws Exception {
        dataSourceRouting.clearThreadLocal();
    }




}
