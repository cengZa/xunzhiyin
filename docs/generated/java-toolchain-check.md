# Java Toolchain Check

此检查对应项目的 Java 21 基线。

## 脚本
- `scripts/check-java-toolchain.ps1`

## 当前覆盖
- 检查 `java -version` 的主版本号是否至少为 21。

## 规则
- 当前项目以 Java 21 为基线。
- 若本机 JDK 低于 21，应直接视为环境阻塞，而不是代码问题。

## 当前限制
- 只检查 `java` 命令，不检查 Maven 使用的 toolchain 配置。
- 不检查多 JDK 并存时 Maven 是否真的选择了目标 JDK。
