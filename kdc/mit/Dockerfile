FROM ubuntu:25.04

ARG REALM="IAM.DEV"
ARG DOMAIN="iam.dev"
ARG KRB_MASTER_KEY="P@ssw0rd123"
ARG ADMIN_PRINC_PASS="P@ssw0rd123"
ARG KDC_HOSTNAME="mit"
ARG KEYTABS_PATH="/etc/keytabs"
ARG LDAP_STASH_FILE_PATH="/etc/krb5kdc/service.stash"
ARG LDAP_ORGANIZATION="IAM Dev"
ARG LDAP_SERVICE_PASSWORD="P@ssw0rd123"
ARG LDAP_SERVICE_DN="cn=kdc-service,dc=iam,dc=dev"
ARG LDAP_ADMIN_PASSWORD="P@ssw0rd123"
ARG LDAP_ADMIN_DN="cn=admin,dc=iam,dc=dev"
ARG LDAP_KRB_CONTAINER_DN="cn=IAM.DEV,dc=iam,dc=dev"
ARG APT_MIRROR="archive.ubuntu.com"
#ARG APT_MIRROR="ftp.uni-stuttgart.de"

ENV REALM=${REALM}
ENV DOMAIN=${DOMAIN}
ENV KRB_MASTER_KEY=${KRB_MASTER_KEY}
ENV ADMIN_PRINC_PASS=${ADMIN_PRINC_PASS}
ENV KDC_HOSTNAME=${KDC_HOSTNAME}
ENV KEYTABS_PATH=${KEYTABS_PATH}
ENV LDAP_STASH_FILE_PATH=${LDAP_STASH_FILE_PATH}
ENV LDAP_ORGANIZATION=${LDAP_ORGANIZATION}
ENV LDAP_SERVICE_PASSWORD=${LDAP_SERVICE_PASSWORD}
ENV LDAP_SERVICE_DN=${LDAP_SERVICE_DN}
ENV LDAP_ADMIN_PASSWORD=${LDAP_ADMIN_PASSWORD}
ENV LDAP_ADMIN_DN=${LDAP_ADMIN_DN}
ENV LDAP_KRB_CONTAINER_DN=${LDAP_KRB_CONTAINER_DN}
ENV APT_MIRROR=${APT_MIRROR}
ENV DEBIAN_FRONTEND=noninteractive

RUN echo "krb5-config krb5-config/default_realm string ${REALM}" | debconf-set-selections && \
    echo "krb5-config krb5-config/kerberos_servers string ${KDC_HOSTNAME}.${REALM}" | debconf-set-selections && \
    echo "krb5-config krb5-config/admin_server string ${KDC_HOSTNAME}.${REALM}" | debconf-set-selections && \
    echo "krb5-config krb5-config/add_servers_hierarchical boolean true" | debconf-set-selections && \
    echo "krb5-kdc krb5-kdc/debconf_master_key password ${KRB_MASTER_KEY}" | debconf-set-selections && \
    echo "krb5-kdc krb5-kdc/debconf_master_key_again password ${KRB_MASTER_KEY}" | debconf-set-selections && \
    echo "krb5-kdc krb5-kdc/purge_data_too boolean false" | debconf-set-selections

RUN echo "slapd slapd/domain string ${DOMAIN}" | debconf-set-selections && \
    echo "slapd slapd/organization string ${LDAP_ORGANIZATION}" | debconf-set-selections && \
    echo "slapd slapd/admin_password password ${LDAP_ADMIN_PASSWORD}" | debconf-set-selections && \
    echo "slapd slapd/admin_password_again password ${LDAP_ADMIN_PASSWORD}" | debconf-set-selections && \
    echo "slapd slapd/backend select MDB" | debconf-set-selections

RUN sed -i "s/archive.ubuntu.com/${APT_MIRROR}/g" /etc/apt/sources.list
#RUN sed -i 's/archive.ubuntu.com/de.archive.ubuntu.com/g' /etc/apt/sources.list

RUN apt-get update -y && \
    apt-get purge -y slapd && \
    rm -rf /var/lib/ldap /etc/ldap/slapd.d && \
    apt-get install -y \
      krb5-user \
      krb5-kdc \
      krb5-admin-server \
      krb5-kdc-ldap \
      ldap-utils \
      slapd \
      schema2ldif \
      curl \
      jq \
      inetutils-tools \
      inetutils-ping \
      vim \
      expect \
      gettext-base && \
      rm -rf /var/lib/apt/lists/*

# create service stash file
COPY --chown=root --chmod=500 ldap_stash_expect.sh /tmp/ldap_stash_expect.sh
RUN /tmp/ldap_stash_expect.sh "${LDAP_SERVICE_PASSWORD}" "${LDAP_STASH_FILE_PATH}" "${LDAP_SERVICE_DN}" && \
    /tmp/ldap_stash_expect.sh "${LDAP_ADMIN_PASSWORD}" "${LDAP_STASH_FILE_PATH}" "${LDAP_ADMIN_DN}" && \
    chmod 600 "${LDAP_STASH_FILE_PATH}" && \
    chown root:root "${LDAP_STASH_FILE_PATH}" && \
    rm /tmp/ldap_stash_expect.sh

# create krb5 realm
COPY --chown=root --chmod=500 krb5_newrealm_expect.sh /tmp/krb5_newrealm_expect.sh
RUN echo "admin/admin@${REALM} *" > /etc/krb5kdc/kadm5.acl
RUN /tmp/krb5_newrealm_expect.sh "${KRB_MASTER_KEY}" "${REALM}" && \
    rm /tmp/krb5_newrealm_expect.sh

# resolve krb5.conf and kdc.conf templates
COPY krb5.conf.template /tmp/krb5.conf.template
RUN envsubst < /tmp/krb5.conf.template > /etc/krb5.conf && \
    rm /tmp/krb5.conf.template

COPY kdc.conf.template /tmp/kdc.conf.template
RUN envsubst < /tmp/kdc.conf.template > /etc/krb5kdc/kdc.conf && \
    rm /tmp/kdc.conf.template

COPY --chown=root --chmod=500 entrypoint.sh /entrypoint.sh

EXPOSE 8088/udp 8088/tcp
EXPOSE 8750/tcp
EXPOSE 8389
EXPOSE 8636
EXPOSE 389
EXPOSE 636

CMD ["/entrypoint.sh"]
#CMD tail -f /dev/null