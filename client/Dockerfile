FROM ubuntu:25.04

RUN apt-get update && \
    apt-get install -y krb5-user curl jq && \
    rm -rf /var/lib/apt/lists/*

COPY krb5.conf /etc/krb5.conf

CMD tail -f /dev/null