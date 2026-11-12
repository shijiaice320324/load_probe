# java-demo

Maven + Java 21 起步项目，包含最小可运行程序与 JUnit 5 测试。

## 环境（便携工具链，已就位）

机器上没有全局 JDK/Maven，项目自带一份，放在 `.tools/`（已加入 `.gitignore`）：

| 组件 | 路径 |
| --- | --- |
| Temurin JDK 21.0.13 | `.tools/jdk-21.0.13+9` |
| Apache Maven 3.9.16 | `.tools/apache-maven-3.9.16` |
| Maven 本地仓库 | `.tools/m2repo`（`.mvn/maven.config` 固定） |

开启环境（PowerShell，注意开头是 `.` + 空格，表示 dot-source 到当前会话）：

```powershell
. .\scripts\dev-env.ps1
```

之后 `java`、`javac`、`mvn` 都可直接用。

> 说明：本机 `C:\Users\<用户>\.m2` 写入被系统文件权限拒绝，所以本地仓库改到项目内 `.tools/m2repo`；
> 另外本机 winget 无法从 GitHub 下载安装包（网络不通），首次依赖是从清华镜像手工下载的便携包。

## 常用命令

```powershell
mvn clean package     # 编译 + 跑测试 + 打包
mvn test              # 只跑测试
java -jar target\java-demo.jar   # 运行打好的 jar
```

当前状态：`mvn clean package` 构建成功，3 个测试通过，`java -jar` 输出 `Hello, Java 21!`。

## 目录结构

```
java-demo/
├─ .mvn/maven.config              # 固定本地仓库位置
├─ scripts/dev-env.ps1            # 便携 JDK/Maven 环境变量
├─ .tools/                        # 便携工具链（不纳入版本控制）
│  ├─ jdk-21.0.13+9/
│  ├─ apache-maven-3.9.16/
│  └─ m2repo/
├─ pom.xml
├─ src/main/java/com/example/demo/App.java
└─ src/test/java/com/example/demo/AppTest.java
```

## 说明

`App.greeting(String)` 是示例逻辑：传入名字返回问候语，空白或 null 回退为 `World`。
