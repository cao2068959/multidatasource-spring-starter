# 多数据源 spring-boot-starter
## 前言

这是什么?  
> 在spring-boot 项目中 本框架可以帮你快捷的使用多个不同的数据源,同时保证了多个数据源之间的事物问题
,同时可以兼容不同的 dataSource,比如 druid,也可以兼容各种 ORM 框架,比如 mybatis , JPA

为什么造这个轮子? ***~~因为我太闲了~~*** 

## 快速体验

> 下面试例 是 mysql+druid+mybatis 的形式

### 运行环境
需要 spring boot 2.0 以上的版本


### 引用坐标

使用maven中央仓库中的坐标
```xml
<dependency>
  <groupId>io.github.cao2068959</groupId>
  <artifactId>multidatasource-spring-starter</artifactId>
  <version>1.1.1</version>
</dependency>
```

或者`clone`本仓库后执行以下命令来安装

```shell
mvn clean install
```

### 配置文件
在引入项目坐标后,在 `application.yml` 文件中配置对应需要连接的源
```yaml
chy:
  multi:
    data-source:
      #定义了一个数据源叫做 userdb
      userdb:
        driver-class-name: com.mysql.cj.jdbc.Driver
        password: mysql密码
        username: mysql账号
        url: jdbc:mysql://192.168.10.10:3306/db_user?characterEncoding=utf8
        #指定了数据源类型使用 druid
        type: "com.alibaba.druid.pool.DruidDataSource"
        #下面都是 druid 的官方配置
        pool:
          initialSize: 3
          minIdle: 5
          maxActive: 20
          maxWait: 30000
          timeBetweenEvictionRunsMillis: 60000
          minEvictableIdleTimeMillis: 30000
          validationQuery: "SELECT 1 FROM DUAL"
          testWhileIdle: true
          testOnBorrow: false
          testOnReturn: false
      #定义了一个数据源叫做 storedb    
      storedb:
        driver-class-name: com.mysql.cj.jdbc.Driver
        password: mysql密码
        username: mysql账号
        url: jdbc:mysql://192.168.10.10:3306/db_store?characterEncoding=utf8
    #如果使用了 mybatis作为 orm框架可以配置以下特殊处理
    mybatis:
      #开启 mybatis 的自动配置, 这里将会自动配置 SqlSessionFactory 对象, 如果使用了 其他的mybatis自动配置插件(如:mybatis-plus)请关闭
      autoconf: true 
```
上述 配置文件中 可以看出 ,配置了2个数据源 一个是 `userdb` 使用了 `druid` 作为数据源,一个 叫做 `storedb`
使用了 默认的 `Tomcat` 数据源,同样你也换成速度更快的 `HikariCP`, 配置同 `spring-boot` 默认的 `dataSource`


### 编写DAO 接口
为了方便 使用了 `mybatis` 的注解形式, 如果使用的是 `xml` 的形式也是一样的

在对应的调用接口上打上注解 `@DataSource("storedb")` 来指定对应的dao 接口使用的是什么数据源

如果接口上没有标注 `@DataSource` 则默认使用第一个数据源

```java
    @Select("select * from employee")
    @DataSource("storedb")
    public List<Map> storedb();


    @Insert("insert into users values(#{userId},#{username},#{phoneNum},#{age},#{ddID},#{creatTime},#{updateTime}) ")
    @DataSource("userdb")
    public void setUser(@Param("userId") Integer userId, @Param("username") String username,
                             @Param("phoneNum") String phoneNum,  @Param("age") Integer age,
                             @Param("ddID") Integer ddID,  @Param("creatTime") Date creatTime,
                             @Param("updateTime") Date updateTime);
```

### 事物
这边定义了 一个新的事 注解来解决多个数据源的事物问题 

`@TransactionMulti`

```java
    @TransactionMulti
    public void setUser(){
        Random random = new Random();
        //使用了storedb数据源的写操作
        userMapper.setEmployee(random.nextInt(10000),"小绿","12345",11,new Date(),new Date());
        //使用了userdb数据源的读操作
        userMapper.setUser(random.nextInt(10000),"小红","12345",14,1,new Date(),new Date());
        int i = 2 /0;
    }
```
这个例子里,当 `2 /0` 执行之后 `userdb` 和 `storedb` 里插入的数据都会被回滚