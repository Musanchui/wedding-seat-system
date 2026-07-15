# ============================================
# 多阶段构建：第一阶段用Maven镜像编译打包，第二阶段只把jar包放进精简的JRE镜像
# 好处：最终镜像体积小很多（不含Maven和源码，只含运行需要的JRE+jar包）
# ============================================

# ---------- 阶段一：构建 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# 先只拷贝pom.xml并下载依赖，这样只要依赖没变，Docker会用缓存，不用每次都重新下载
COPY pom.xml .
RUN mvn dependency:go-offline -B
# 再拷贝源码进行编译打包
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 重要：Alpine镜像默认不带任何字体，Java用Graphics2D画中文文字(比如邀请函二维码上的新人姓名)
# 会渲染成方块/乱码。这里装上fontconfig和文泉驿中文字体，解决这个问题。
RUN apk add --no-cache fontconfig wqy-zenhei

# 从构建阶段把打好的jar包拷过来（通配符匹配，不用关心具体版本号文件名）
COPY --from=build /build/target/*.jar app.jar

# 上传文件存储目录，需要在docker run或docker-compose里挂载数据卷到这个路径，否则容器重建文件会丢
RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
