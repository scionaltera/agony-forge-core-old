FROM openjdk:8-jdk-alpine as build
MAINTAINER Peter Keeler <scion@agonyengine.com>
WORKDIR /opt/build
COPY . /opt/build/
RUN cd /opt/build \
&& apk update \
&& apk add --no-cache bash \
&& ./gradlew --console=plain clean build -x buildDocker -x dependencyCheckAnalyze

FROM openjdk:8-jre-alpine
MAINTAINER Peter Keeler <scion@agonyengine.com>
EXPOSE 8080
COPY --from=build /opt/build/build/libs/agony-forge-core-*.jar /opt/mud/app.jar
CMD ["/usr/bin/java", "-jar", "/opt/mud/app.jar"]
