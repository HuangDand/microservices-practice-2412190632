**环境检查
java --version：
openjdk 17.0.20 2026-07-21
OpenJDK Runtime Environment (build 17.0.20+8-1-26.04-Ubuntu)
OpenJDK 64-Bit Server VM (build 17.0.20+8-1-26.04-Ubuntu, mixed mode, sharing)

mvn --version：
Apache Maven 3.9.12
Maven home: /usr/share/maven
Java version: 17.0.20, vendor: Ubuntu, runtime: /usr/lib/jvm/java-17-openjdk-amd64
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "6.18.33.2-microsoft-standard-wsl2", arch: "amd64", family: "unix"

git --version：
git version 2.53.0

docker version：
Client:
 Version:           29.7.2
 API version:       1.55
 Go version:        go1.26.5
 Git commit:        a7dcaa6
 Built:             Wed Aug  5 18:27:38 2026
 OS/Arch:           linux/amd64
 Context:           default

Server: Docker Desktop 4.90.0 (238679)
 Engine:
  Version:          29.7.2
  API version:      1.55 (minimum version 1.40)
  Go version:       go1.26.5
  Git commit:       6a43e3d
  Built:            Wed Aug  5 18:28:36 2026
  OS/Arch:          linux/amd64
  Experimental:     false
 containerd:
  Version:          v2.3.3
  GitCommit:        aad11006b869517fcd3009450b6f82da282e1a9b
 runc:
  Version:          1.4.3
  GitCommit:        v1.4.3-0-gbb14dabe
 docker-init:
  Version:          0.19.0
  GitCommit:        de40ad0

docker compose version：Docker Compose version v5.5.1

## 概念回答
### 什么是微服务架构？
微服务架构是将一个大型软件系统拆分成若干个小型、独立的服务。每个服务只负责一部分业务，能够独立开发、独立部署、独立扩容，服务之间通过网络接口互相调用配合完成整体业务。

### 微服务和单体架构的主要区别是什么？
单体架构所有业务代码放在同一个项目中，统一打包、统一部署，模块之间是本地方法调用；微服务拆分为多个独立服务，服务之间通过网络远程调用。单体架构开发上手简单，但项目规模变大后维护、发布困难；微服务便于团队分工、可以针对业务单独扩容，但是引入了网络通信、分布式事务、服务治理等额外的复杂度。

### 为什么本课程先实现单体系统，再逐步拆分为微服务？
如果直接开发微服务，需要同时处理业务逻辑和大量分布式的复杂问题，学习门槛很高。先完成单体系统，可以梳理清楚完整业务需求，理清业务模块边界；之后基于已完成的单体项目做拆分改造，可以直观体会拆分原则，更好对比两种架构的优缺点。

### 为什么作业需要提供可重复运行的测试或验证脚本？
消除不同机器之间的环境差异，其他人拿到代码和脚本就可以复现运行结果。方便老师检查作业、进行代码评审，同时也为后续持续集成自动化测试打下基础。
