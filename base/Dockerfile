FROM ubuntu:25.04

ARG REALM="IAM.DEV"
ARG DOMAIN="iam.dev"
ARG KDC_HOSTNAME="mit"
ARG KEYTABS_PATH="/etc/keytabs"
ARG APT_MIRROR="archive.ubuntu.com"
ARG LDAP_ORGANIZATION="IAM Dev"
ARG LDAP_KRB_CONTAINER_DN="cn=IAM.DEV,dc=iam,dc=dev"

ENV REALM=${REALM}
ENV DOMAIN=${DOMAIN}
ENV KDC_HOSTNAME=${KDC_HOSTNAME}
ENV KEYTABS_PATH=${KEYTABS_PATH}
ENV APT_MIRROR=${APT_MIRROR}
ENV LDAP_ORGANIZATION=${LDAP_ORGANIZATION}
ENV LDAP_KRB_CONTAINER_DN=${LDAP_KRB_CONTAINER_DN}
ENV DEBIAN_FRONTEND=noninteractive

RUN sed -i "s/archive.ubuntu.com/${APT_MIRROR}/g" /etc/apt/sources.list

RUN apt-get update -y && \
    apt-get upgrade -y && \
    apt-get install -y \
      krb5-user \
      curl \
      vim \
      jq \
      yq \
      ldap-utils \
      schema2ldif \
      inetutils-tools \
      inetutils-ping
