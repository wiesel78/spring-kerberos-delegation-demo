FROM spring_kerberos_delegation_base

ARG CACHEBUSTER=1
ARG KRB_MASTER_KEY="P@ssw0rd123"
ARG ADMIN_PRINC_PASS="P@ssw0rd123"
ARG LDAP_STASH_FILE_PATH="/etc/krb5kdc/service.stash"
ARG LDAP_SERVER_URL="ldap://ldap:389/"
ARG LDAP_SERVICE_PASSWORD="P@ssw0rd123"
ARG LDAP_SERVICE_DN="cn=kdc-service,dc=iam,dc=dev"
ARG LDAP_ADMIN_PASSWORD="P@ssw0rd123"
ARG LDAP_ADMIN_DN="cn=admin,dc=iam,dc=dev"

ENV KRB_MASTER_KEY=${KRB_MASTER_KEY}
ENV ADMIN_PRINC_PASS=${ADMIN_PRINC_PASS}
ENV LDAP_STASH_FILE_PATH=${LDAP_STASH_FILE_PATH}
ENV LDAP_SERVER_URL=${LDAP_SERVER_URL}
ENV LDAP_SERVICE_PASSWORD=${LDAP_SERVICE_PASSWORD}
ENV LDAP_SERVICE_DN=${LDAP_SERVICE_DN}
ENV LDAP_ADMIN_PASSWORD=${LDAP_ADMIN_PASSWORD}
ENV LDAP_ADMIN_DN=${LDAP_ADMIN_DN}

RUN echo "krb5-config krb5-config/default_realm string ${REALM}" | debconf-set-selections && \
    echo "krb5-config krb5-config/kerberos_servers string ${KDC_HOSTNAME}.${REALM}" | debconf-set-selections && \
    echo "krb5-config krb5-config/admin_server string ${KDC_HOSTNAME}.${REALM}" | debconf-set-selections && \
    echo "krb5-config krb5-config/add_servers_hierarchical boolean true" | debconf-set-selections && \
    echo "krb5-kdc krb5-kdc/debconf_master_key password ${KRB_MASTER_KEY}" | debconf-set-selections && \
    echo "krb5-kdc krb5-kdc/debconf_master_key_again password ${KRB_MASTER_KEY}" | debconf-set-selections && \
    echo "krb5-kdc krb5-kdc/purge_data_too boolean false" | debconf-set-selections

RUN apt-get install -y \
      krb5-kdc \
      krb5-admin-server \
      krb5-kdc-ldap \
      slapd \
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

CMD ["bash", "/entrypoint.sh"]
