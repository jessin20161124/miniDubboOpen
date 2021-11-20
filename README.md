1. 执行项目下init.sql，创建数据库和数据表
2. 在项目路径下打包
```
mvn -DskipTests package
```
3. 运行项目
```
### xxx替换为你自己的数据库ip地址、用户名、用户密码
java -DdbUserPassword=xxx -DdbIp=xxx -DdbUser=xxx -jar /Users/jessin/Documents/program/java/springboot-demo/target/demo-0.0.1-SNAPSHOT.jar

```
更多精彩样例，请关注公众号：
![扫一扫](https://raw.githubusercontent.com/jessin20161124/springboot-demo/main/scan.png)


