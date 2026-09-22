# ============================================================
# ContentHub 后端镜像（多阶段构建）
#
# 构建阶段用 Maven + JDK 17 打包，运行阶段只带 JRE，
# 这样最终镜像里没有源码和 Maven 仓库。
# ============================================================

# ---------------- 构建阶段 ----------------
# 用 alpine 变体：体积更小，且与运行阶段的基础镜像保持同一发行版
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build

# 先只拷贝各个 pom：只要依赖没变，这一层就能命中缓存，
# 改 Java 代码时不会重新下载整个 Maven 仓库。
COPY pom.xml ./
COPY contenthub-common/pom.xml contenthub-common/
COPY contenthub-jwt/pom.xml contenthub-jwt/
COPY contenthub-admin/pom.xml contenthub-admin/
COPY contenthub-web/pom.xml contenthub-web/

# 预热依赖。加 || true 是有意的：dependency:go-offline 在多模块下偶尔会漏解析
# 某些插件依赖，这里只当作「尽量缓存」，真正的成败由下面的 package 决定。
RUN mvn -B -DskipTests dependency:go-offline || true

COPY . .
RUN mvn -B -DskipTests package

# ---------------- 运行阶段 ----------------
# alpine 变体自带 busybox wget，compose 里的健康检查要用到
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 时区：容器默认 UTC，日志与数据库时间会差 8 小时
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 以非 root 用户运行
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/contenthub-web/target/contenthub-web-*.jar app.jar

# 上传目录必须先在镜像里建好并改属主：
# 空命名卷挂上来时会继承镜像里这个目录的属主，否则卷是 root 所有，
# app 用户写不进去，上传会直接失败。
RUN mkdir -p /app/uploads && chown -R app:app /app
USER app

EXPOSE 8084

# 容器内存有限，显式给 JVM 一个上限，避免它按宿主机内存估算而被 OOM 杀掉
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
