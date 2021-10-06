### 开发环境

​		win10 + JDK8 

### 项目简介

​		手写了一个简易版的tomcat，保留了tomcat原生架构体系，实现了tomcat常用功能，在此基础上添加了基于JAVA AIO实现的异步IO吞吐机制。

#### tomcat架构简明

​		我们在使用tomcat的过程中，可以实现多域名多端口号映射到同一web应用这一功能，该功能便是由tomcat架构体系所支撑实现的。下图是经简化后的tomcat架构图。

![image-20211005215209443](C:\Users\dell\AppData\Roaming\Typora\typora-user-images\image-20211005215209443.png)

​		Server: 架构中的老大，Service的管理者，一个server可以映射多个service。

​		Service: 具体的服务提供者，用于管理connector与catalina之间的映射关系，即通过connector监听到请求后能通过service找到对应的Engine处理该请求。每一个connector可以对应一种传输协议，具体的处理这种请求协议的类就是Engine，有了service，tomcat就能管理好每一种协议的请求监听和请求处理，这也是tomcat能够彻底分离请求监听和请求处理的法宝。

​		Connector: 请求监听器，开启socket并监听客户端请求，返回响应数据。每一个connector可以对应一个端口号，使得访问不同的端口能够映射到同一web应用。

​		Engine: 也称为catalina，请求处理器，负责具体的请求处理。比如找到客户端请求的是哪一个web应用。

​		Host: 虚拟主机，实现一台主机提供多个域名的服务，每个域名视为一台虚拟主机，管理多个web应用。

​		Context: 容器，具体的web应用，真正的处理客户端请求的执行者。

#### 实现的功能

​		实现http协议的请求解析与响应封装

​		能够响应html、图片、PDF、servlet等静态资源和动态资源的请求

​		实现了200、304、404、500的状态码及对应的处理流程

​		实现服务端跳转传参

​		实现响应消息的压缩传输

​		自定义类加载器，实现web应用间的隔离

​		实现cookie与session会话技术

​		实现热部署

​		单例servlet

​		责任链模式实现应用过滤链

​		实现BIO/NIO(Reactor模式)/AIO三种IO机制

### 使用说明 

​		克隆下来源代码，通过IDEA打开，源代码分BIO/NIO/AIO三个模块，分别在对应模块里的/conf/sever.xml配置端口号，修改context映射的路径。运行每一个module里面的startup.bat即可。

### 压力测试

​		压测工具选用windows下的ab压测软件。以下为压测结果。

​		1000个客户端线程模拟100万条请求连接：

|   webServer   |  吞吐率   | 平均请求处理时间 |
| :-----------: | :-------: | :--------------: |
|  myTomcatBIO  | 3093.95/s |     0.323ms      |
|  myTomcatNIO  | 4085.14/s |     0.245ms      |
|  myTomcatAIO  | 3824.61/s |     0.261ms      |
| Apache Tomcat | 3889.83/s |     0.257ms      |

### 优化方向

​		由压测可见，AIO模型相对于NIO模型来说始终逊色一筹，理论上来讲，真正意义异步的AIO应该有更高的吞吐量才对。推测应该是AIO模型实现得不够理想，下一步要做的工作就是优化AIO模型代码。

​		