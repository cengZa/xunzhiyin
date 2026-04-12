请先阅读以下文档：

- docs/00_meta/constraints.md
- docs/02_design/system_architecture.md
- docs/03_backend/backend_structure.md
- docs/02_design/api_design.md
- 以及其他需要的文档

目标：
初始化一个适合本项目的 Spring Boot 4 + MyBatis-Plus + MySQL + Redis 后端工程骨架。

要求：
1. 创建标准 Maven 项目结构。
2. 生成 pom.xml，并包含本项目需要的最小依赖：
    - spring-boot-starter-web
    - spring-boot-starter-validation
    - spring-boot-starter-data-redis
    - mybatis-plus-spring-boot4-starter
    - mysql-connector-j
    - lombok
    - spring-boot-starter-test
3. 创建启动类。
4. 创建 common、config、controller、domain、mapper、service、service/impl、strategy、infra 等包结构。
5. 根据 docs/03_backend/backend_structure.md 生成空骨架类或接口，但先不要实现复杂业务逻辑。
6. 创建 application.yml，使用占位式本地开发配置即可。
7. 创建通用返回体、基础异常类、全局异常处理类。
8. 保持类名、包名、模块边界与 docs 文档一致。
9. 不要实现不在 docs 中的额外模块。

输出要求：
- 直接创建/修改文件
- 最后列出新建文件和修改文件
- 简述每个文件的作用
- 说明当前工程是否可编译
