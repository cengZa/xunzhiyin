# Backend Skeleton Check

此检查对应 `docs/03_backend/backend_structure.md` 的最小骨架一致性验证。

## 脚本
- `scripts/check-backend-skeleton.ps1`

## 当前覆盖
- 启动类
- `common / config / controller / domain / mapper / service / strategy / infra`
- `application.yml`
- 基础测试类

## 当前限制
- 只检查文件存在，不检查类内容是否完全符合设计。
- 不检查方法签名漂移。
- 不检查包内是否多出未授权模块。
