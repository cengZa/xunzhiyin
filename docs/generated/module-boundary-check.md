# Module Boundary Check

此检查对应 `docs/03_backend/backend_structure.md` 的顶层包边界。

## 脚本
- `scripts/check-module-boundaries.ps1`

## 当前覆盖
- `com.lcj.campusreco` 下允许的顶层目录
- 启动类 `CampusRecoApplication.java`

## 规则
- 不允许出现 docs 之外的新增顶层模块。
- 当前只检查顶层边界，不检查子包是否超纲。

## 当前限制
- 不检查子目录下的细粒度越界。
- 不检查依赖方向是否违规。
