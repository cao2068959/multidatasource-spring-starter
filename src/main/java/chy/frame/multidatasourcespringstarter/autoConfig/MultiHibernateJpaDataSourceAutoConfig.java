package chy.frame.multidatasourcespringstarter.autoConfig;


import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaSessionFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.List;


@Configuration
@ConditionalOnClass({ LocalSessionFactoryBean.class, LocalContainerEntityManagerFactoryBean.class})
@AutoConfigureAfter(MultiDataSourceAutoConfig.class)
@EnableConfigurationProperties(JpaProperties.class)
public class MultiHibernateJpaDataSourceAutoConfig  {

    @Autowired
    JpaProperties jpaProperties;


    @Bean
    @ConditionalOnBean(name = "multiDataSource")
    public LocalContainerEntityManagerFactoryBean MybaitisDataSouce(@Qualifier("multiDataSource")DataSource dataSource) throws Exception {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setDataSource(dataSource);

        JpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        ((HibernateJpaVendorAdapter) jpaVendorAdapter).setShowSql(jpaProperties.isShowSql());
        ((HibernateJpaVendorAdapter) jpaVendorAdapter).setDatabase(jpaProperties.getDatabase());
        ((HibernateJpaVendorAdapter) jpaVendorAdapter).setGenerateDdl(jpaProperties.isGenerateDdl());



        return localContainerEntityManagerFactoryBean;
    }






}
