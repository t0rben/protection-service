FROM mcr.microsoft.com/java/jre-headless:11u2-zulu-ubuntu

MAINTAINER Kai Zimmermann <kai.zimmermann@microsoft.com>

ARG JAR_FILE=protection-service-0.0.1-SNAPSHOT.jar

ENV PROTECTION_HOME=/opt/protection
ENV COM_MICROSOFT_PROTECTION_FILE-API-CLI /opt/protection/file_sample

EXPOSE 8080

VOLUME "$PROTECTION_HOME/data"

WORKDIR $PROTECTION_HOME
ENTRYPOINT ["java","-jar","protection-service.jar","-Xms768m -Xmx768m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+HeapDumpOnOutOfMemoryError"]

ADD target/$JAR_FILE protection-service.jar
ADD mip_sdk_file_ubuntu1604_1.0.49/bins/release/x86_64/file_sample file_sample
