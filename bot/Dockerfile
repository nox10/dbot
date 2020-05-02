FROM openjdk:11.0.4-jdk-stretch
#VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
