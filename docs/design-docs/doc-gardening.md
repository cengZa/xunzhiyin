# Doc Gardening

当前尚未自动化，但先定义维护职责，防止索引层迅速腐烂。

## 维护动作
- 检查索引是否指向真实文件
- 检查旧文档是否已被新入口挂载
- 检查 prompt shell 是否开始重新膨胀
- 检查计划是否有状态漂移
- 检查代码骨架是否仍与设计文档对齐
- 检查 service 接口是否仍满足文档约定的核心签名
- 检查 controller 路由是否仍覆盖主接口设计
- 检查关键 API 字段是否仍与文档约定一致
- 检查是否出现 docs 外新增模块
- 检查本机 Java toolchain 是否满足项目基线

## 触发时机
- 完成大型重构后
- 新增一批文档后
- 发现入口失真后

## 后续自动化方向
- 增加简单脚本扫描链接与缺失文件
- 周期性生成文档新鲜度报告
- 增加签名级结构检查
- 增加 controller 参数和响应字段级检查
- 统一结构检查与编译检查入口
- 增加模块依赖方向检查
- 增加 Maven toolchain 和编译验证集成

## 当前可用工具
- `scripts/check-doc-links.ps1`: 检查 markdown 文档中显式出现的 `.md` 路径是否存在
- `scripts/check-backend-skeleton.ps1`: 检查后端骨架文件是否与设计结构对齐
- `scripts/check-module-boundaries.ps1`: 检查顶层模块是否越界
- `scripts/check-service-signatures.ps1`: 检查 service 接口是否包含文档要求的核心方法
- `scripts/check-controller-routes.ps1`: 检查 controller 是否覆盖文档要求的主路由
- `scripts/check-api-contracts.ps1`: 检查关键 API 字段是否与文档约定对齐
- `scripts/check-java-toolchain.ps1`: 检查本机 Java 主版本是否达到 21
- `scripts/run-governance-checks.ps1`: 统一运行全部治理检查
