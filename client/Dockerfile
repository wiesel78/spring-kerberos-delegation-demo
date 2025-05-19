FROM ubuntu:25.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update -y && \
    apt-get upgrade -y && \
    apt-get install -y krb5-user curl jq inetutils-tools inetutils-ping vim && \
    rm -rf /var/lib/apt/lists/*

COPY krb5.conf /etc/krb5.conf

CMD tail -f /dev/null