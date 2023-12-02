FROM node:18-alpine AS node

FROM eclipse-temurin:17.0.9_9-jdk-alpine as jre-build

# Create a custom Java runtime
RUN apk add --no-cache binutils && \
    $JAVA_HOME/bin/jlink \
         --add-modules java.base,java.desktop,java.xml,java.logging \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime

FROM eclipse-temurin:17.0.9_9-jdk-alpine AS builder

COPY --from=node /usr/lib /usr/lib
COPY --from=node /usr/local/share /usr/local/share
COPY --from=node /usr/local/lib /usr/local/lib
COPY --from=node /usr/local/include /usr/local/include
COPY --from=node /usr/local/bin /usr/local/bin

COPY . /project
WORKDIR /project
RUN npm install -g pnpm
RUN --mount=type=cache,target=/root/.gradle --mount=type=cache,target=/root/.pnpm-store \
    node -v && \
    pnpm --version && \
    ./gradlew --no-daemon build installDist

FROM alpine:3.17

COPY --from=node /usr/lib /usr/lib
COPY --from=node /usr/local/share /usr/local/share
COPY --from=node /usr/local/lib /usr/local/lib
COPY --from=node /usr/local/include /usr/local/include
COPY --from=node /usr/local/bin /usr/local/bin

ENV JAVA_HOME=/opt/java
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

COPY --from=builder /project/server/build/install/server /server
COPY --from=builder /project/frontend2 /frontend

EXPOSE 7000
CMD ["/server/bin/server", "--enable-node", "--script", "/frontend/build"]