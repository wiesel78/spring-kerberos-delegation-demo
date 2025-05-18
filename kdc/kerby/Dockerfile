FROM ubuntu:25.04

ENV DEBIAN_FRONTEND=noninteractive
RUN apt update -y && apt upgrade -y && apt install -y \
    openjdk-21-jdk \
    maven \
    curl \
    wget \
    unzip

RUN mkdir -p /lib/kerby /opt/kerby/kdc-dist/keytabs /opt/kerby/kdc-dist/runtime && \
    wget https://dlcdn.apache.org//directory/kerby/dist/2.1.0/kerby-all-2.1.0-source-release.zip -O /lib/kerby/kerby.zip && \
    unzip /lib/kerby/kerby.zip -d /lib/kerby && \
    mv /lib/kerby/kerby-*/* /lib/kerby/ && \
    cd /lib/kerby && \
    mvn clean install -DskipTests && \
    cd /lib/kerby/kerby-dist && \
    mvn package -Pdist -DskipTests && \
    cp -r /lib/kerby/kerby-dist/kdc-dist /lib/kerby/kerby-dist/tool-dist /opt/kerby && \
    cd /opt/kerby/kdc-dist && \
    rm -rf /lib/kerby

COPY --chmod=550 start.sh /opt/kerby/start.sh
EXPOSE 8888

ENTRYPOINT ["sh", "/opt/kerby/start.sh"]