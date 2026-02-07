<<<<<<< HEAD
"# Dream-Studio--梦工坊" 
正在准备ing.....
=======
﻿#  ̲̅𝑬𝒍𝒆𝒗𝒆𝒏 𝒃𝒍𝒐𝒈
 **一个以简约为主的个人主题博客**

# ✨ 项目结构
```
Eleven-blog/
├── blog-backend/            
│   ├── blog-common/         # 公共模块
│   ├── blog-pojo/           # 实体类模块 
│   ├── blog-server/         # 核心业务服务 (启动入口, Controller, Service)
│   └── Dockerfile          
├── blog-cms/                # 后台管理系统前端
│   ├── src/                 
│   ├── public/            
│   ├── vite.config.js      
│   └── nginx.conf          
├── blog-view/               # 博客前台展示前端
│   ├── src/                 
│   ├── vite.config.js    
│   └── nginx.conf      
├── sql/                     # 数据库初始化脚本 (.sql 文件)
├── upload_data/             # 文件上传存储目录 (挂载卷)
└── docker-compose.yml       # Docker Compose 

```
# 🛠️ 技术栈
### 后端 (blog-backend)
- 核心框架: Spring Boot 
- 安全框架：Spring Security
- Token：jwt
- 数据库: MySQL,Redis
- ORM框架: MyBatis 
### 前端 (blog-cms & blog-view)
- 构建工具: Vite
- 语言: JavaScript 
- 框架: Vue 3 
- 样式/UI: Element Plus


# 📄 使用文档
```
1. 数据库准备
创建一个名为 eleven_blog 的数据库。
运行 sql/ 目录下的 .sql 
2. 后端启动 (blog-backend)
修改 application.yml 中的数据库连接信息
启动
3. 前端启动 (blog-cms & blog-view)
cd blog-cms / blog-view
npm install   
npm run dev    
  
// 修改你的配置
/* .env  */
blog-cms/.env
blog-view/.env
VITE_APP_API_URL=http://localhost:8081/api
VITE_APP_UPLOAD_URL=http://localhost:8081

npm run build  //打包
```

# 📷 截图
<img width="1000" height="500" alt="屏幕截图 2026-01-10 192951" src="https://github.com/user-attachments/assets/c03e7230-60f4-4275-9a8a-efff87b96592" />
<img width="1000" height="500" alt="image" src="https://github.com/user-attachments/assets/f7ca07f4-59aa-4f07-a960-59d2f2699a59" />

<img width="1000" height="500" alt="屏幕截图 2026-01-10 193038" src="https://github.com/user-attachments/assets/f789a6f1-c389-4c61-a019-26ec947dcc7a" />

<img width="1000" height="500" alt="image" src="https://github.com/user-attachments/assets/6515e309-44bd-4ec1-8722-ccc9afcbde07" />

<img width="1000" height="500" alt="image" src="https://github.com/user-attachments/assets/2904549a-f101-419c-b7a4-d3d60be38fff" />


# 🤝 贡献
欢迎提交 Issue 或 Pull Request 来改进这个项目！




>>>>>>> d9a26b3 (筹备ing...)
