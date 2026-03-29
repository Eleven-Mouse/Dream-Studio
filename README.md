# Dream Studio

Dream Studio 是一个面向个人内容创作与轻社区运营的全栈博客系统，集成了博客发布、动态分享、论坛交流、资源展示、通知中心与后台管理等能力，适合作为个人站点、作品展示站或小型内容社区的基础项目。

## 项目亮点

- 博客前台：支持首页文章流、精华文章轮播、文章详情、分类筛选、标签页、归档页和关于页。
- 轻社区能力：提供动态（Moment）、论坛帖子、评论互动、热门侧栏与举报处理机制。
- 创作者工作台：普通用户登录后可进入个人中心，管理自己的文章、动态、论坛帖子、通知与账号资料。
- 管理后台：管理员可查看统计面板，管理文章、评论、标签、分类、用户、公告、站点信息和论坛举报。
- 统一权限模型：前端按 capability 控制工作台页面，后端按角色与接口职责划分普通用户端和管理端。
- 支持容器化部署：仓库内已提供前端、后端、MySQL、Redis 的 `docker-compose.yml` 和 Dockerfile。

## 技术栈

### 前端

- Vue 3
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios
- `md-editor-v3` / `highlight.js` / `prismjs`

### 后端

- Java 21
- Spring Boot 3
- Spring Security
- MyBatis-Plus
- PageHelper
- Redis
- JWT
- Spring Boot Actuator
- SpringDoc OpenAPI

### 数据与存储

- MySQL 8
- Redis
- 本地上传目录
- S3 兼容对象存储接口（可对接 MinIO / AWS S3）

### 部署

- Docker
- Docker Compose
- Nginx

## 仓库结构

```text
Dream-Studio/
|- blog-view/      # Vue 3 前端，包含前台页面、用户中心、管理后台
|- blog-backend/   # Spring Boot 多模块后端
|  |- blog-common/
|  |- blog-pojo/
|  `- blog-server/ # 主服务模块
|- sql/            # 数据库初始化脚本
|- upload_data/    # 本地上传文件目录
`- docker-compose.yml
```

## 快速开始

### 方式一：使用 Docker Compose

这是最快的启动方式，适合本地联调与体验。

```bash
docker compose up --build -d
```

默认会启动以下服务：

- 前端：`http://localhost:3000`
- 后端：`http://localhost:8080`
- MySQL：`localhost:3306`
- Redis：`localhost:6379`

停止服务：

```bash
docker compose down
```

如果希望连同数据卷一起清理：

```bash
docker compose down -v
```

## 本地开发

### 环境要求

- Node.js 20+
- npm 10+
- Java 21
- Maven 3.9+
- MySQL 8
- Redis 6+

### 1. 初始化数据库

创建数据库并导入初始化脚本：

```bash
mysql -u root -p < sql/init.sql
```

默认脚本会创建并使用数据库：`eleven_blog`。

### 2. 启动后端

在项目根目录执行：

```bash
mvn -f blog-backend/pom.xml -pl blog-server -am spring-boot:run
```

默认后端地址：`http://localhost:8080`

可选接口文档地址：

```text
http://localhost:8080/swagger-ui/index.html
```

### 3. 启动前端

进入前端目录：

```bash
cd blog-view
npm install
npm run dev
```

默认前端地址：`http://localhost:3000`

前端默认环境变量见 `blog-view/.env`：

```env
VITE_APP_API_URL=http://localhost:8080/api
VITE_APP_UPLOAD_URL=http://localhost:8080
```

## 关键配置

后端配置文件位于 `blog-backend/blog-server/src/main/resources/application.yml`，支持通过环境变量覆盖。

### 数据库与 Redis

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_DATA_REDIS_PASSWORD`
- `SPRING_DATA_REDIS_DATABASE`

### 文件上传

- `FILE_UPLOAD_DIR`：本地上传目录

### 对象存储

- `OBJECT_STORAGE_ENDPOINT`
- `OBJECT_STORAGE_REGION`
- `OBJECT_STORAGE_ACCESS_KEY`
- `OBJECT_STORAGE_SECRET_KEY`
- `OBJECT_STORAGE_BUCKET`
- `OBJECT_STORAGE_PUBLIC_URL`
- `OBJECT_STORAGE_KEY_PREFIX`
- `OBJECT_STORAGE_PATH_STYLE_ACCESS`

### 站点信息

- `BLOG_OWNER_EMAIL`
- `BLOG_OWNER_AVATAR`
- `BLOG_OWNER_NICKNAME`

### GitHub OAuth

- `GITHUB_OAUTH_CLIENT_ID`
- `GITHUB_OAUTH_CLIENT_SECRET`
- `GITHUB_OAUTH_REDIRECT_URI`

## 默认账号与安全提示

- 后端启动时会自动确保默认管理员账号存在：`admin / 123456`。
- 首次启动后请立即修改默认管理员密码，避免在公开环境中直接使用默认凭据。
- 当前仓库中的 `docker-compose.yml` 与 `application.yml` 含有本地开发用途的默认密码、示例 OAuth 配置和对象存储配置，请在部署前全部替换。
- 建议将敏感信息迁移到环境变量或外部密钥管理系统，不要把生产密钥直接提交到仓库。

## 数据说明

`sql/init.sql` 提供了基础表结构与部分演示数据，覆盖的核心数据对象包括：

- 文章 `article`
- 分类 `category`
- 标签 `tags`
- 评论 `comment`
- 动态 `moment`
- 系统配置 `system_config`

另外，应用启动时还会自动确保以下业务表存在：

- 用户账号 `user_account`
- 论坛帖子 `forum_post`
- 论坛举报 `forum_report`
- 用户通知 `user_notification`
- 站点公告 `announcement`
- 资源中心 `site_resource`

## 部署建议

- 开发环境可以直接使用仓库内的 Docker 配置快速启动。
- 生产环境建议将 MySQL、Redis、对象存储改为独立服务，并更换全部默认账号、密码和端口暴露策略。
- 若启用 GitHub OAuth，请确保 `GITHUB_OAUTH_REDIRECT_URI` 与前端登录页地址保持一致。

## 后续可完善方向

- 增加 CI/CD 与自动化测试说明
- 补充 API 文档、接口鉴权说明和数据库 ER 图
- 补充页面截图、部署示意图和生产环境配置模板

## License

当前仓库未声明开源许可证；如果你计划公开发布，建议补充 `LICENSE` 文件。

