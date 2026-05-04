# 本地隔离环境说明

这个源码包包含三个主要工程：

- `RuoYi-Backend`: Java 17 + Maven + Spring Boot 4 的若依后端多模块工程。
- `RuoYi-Vue3-master`: Vue 3 + Vite 管理端前端。
- `mini-program`: Taro 4 + Vue 3 微信小程序。

根目录 README 还提到 `device-integration` 和 `intervention-engine`，但当前 zip 中没有这两个目录；`docker-compose.yml` 里引用它们时会构建失败，除非后续补齐源码。

## 推荐版本

- Python: 3.9+，本仓库已使用根目录 `.venv` 隔离。
- Node.js: 20 LTS，根目录 `.nvmrc` 和 `.node-version` 已固定为 `20`。本机已安装 Homebrew `node@20`，激活脚本会优先使用它，不覆盖全局 Node。
- Java: JDK 17。激活脚本会优先使用 Android Studio 自带 JBR：`/Applications/Android Studio.app/Contents/jbr/Contents/Home`。
- Maven: 3.9+。

## 启用虚拟环境

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
source scripts/activate_dev_env.sh
python -m pip install -r requirements-dev.txt
```

`requirements-dev.txt` 当前只有注释，因为仓库内 Python 脚本只依赖标准库。保留这个文件是为了让 `.venv` 有稳定入口，后续如果增加 Python 工具依赖可以集中写在这里。

激活脚本会把 pip 缓存固定到根目录 `.pip-cache`，并把 Python 字节码缓存固定到 `.pycache`，避免使用系统用户缓存导致权限或版本残留问题。

## Node 依赖隔离

先切换到 Node 20，再安装依赖：

```bash
nvm use
cd RuoYi-Vue3-master
npm install

cd ../mini-program
npm ci
```

当前机器没有 `nvm` 命令；本机已安装 `/opt/homebrew/opt/node@20/bin/node`，激活脚本会自动把它放到当前 shell 的 PATH 前面。如果你改用 `nvm`、`fnm` 或 Volta，按根目录 `.nvmrc` / `.node-version` 切换到 Node 20 即可。

两个 Node 工程的 `.npmrc` 已把 registry 固定到 `https://registry.npmjs.org/`，缓存放到根目录 `.npm-cache`，并启用 `engine-strict`，避免误用 Node 25 之类过新的运行时安装依赖。

注意：`RuoYi-Vue3-master` 当前没有 `package-lock.json`，首次 `npm install` 会生成锁文件；`mini-program` 已有 `package-lock.json`，建议用 `npm ci`。

## Maven 依赖隔离

后端的 Maven 本地仓库已通过 `RuoYi-Backend/.mvn/maven.config` 固定到：

```text
/Users/black/Desktop/try/Xindong_Platform-main/.m2/repository
```

构建命令：

```bash
cd RuoYi-Backend
mvn clean package -DskipTests
```

当前机器默认 `java` 不在 PATH，但激活脚本会挂载 Android Studio 自带 JDK；Maven 已通过 Homebrew 安装。

## 已知冲突点

- 当前系统全局 Node 是 `v25.9.0`，不建议直接跑 Vue/Taro 依赖安装；项目激活脚本会切到 Homebrew `node@20`。
- 当前系统默认 PATH 没有 Java Runtime；项目激活脚本会设置 Android Studio JDK。
- `docker-compose.yml` 引用了 zip 中缺失的 `device-integration` 和 `demo` 目录，完整 compose 启动目前不可用。
