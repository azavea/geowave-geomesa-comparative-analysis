FROM java:8

MAINTAINER Nathan Zimmerman <npzimmerman@gmail.com>

EXPOSE 7070

COPY server/target/scala-2.11/query-server-assembly-0.0.1.jar /opt/app/query-server-assembly-0.0.1.jar

CMD [ "java", "-jar", "/opt/app/query-server-assembly-0.0.1.jar", "com.azavea.ca.server.Main"]
