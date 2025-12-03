FROM spring_kerberos_delegation_base

ARG CACHEBURST=1
ARG SLAPD_SERVICES="ldap://:8389/ ldapi:///"
ARG KRB_MASTER_KEY="P@ssw0rd123"
ARG ADMIN_PRINC_PASS="P@ssw0rd123"
ARG LDAP_SERVICE_PASSWORD="P@ssw0rd123"
ARG LDAP_SERVICE_DN="cn=kdc-service,dc=iam,dc=dev"
ARG LDAP_ADMIN_PASSWORD="P@ssw0rd123"
ARG LDAP_ADMIN_DN="cn=admin,dc=iam,dc=dev"

ENV SLAPD_SERVICES=${SLAPD_SERVICES}
ENV KRB_MASTER_KEY=${KRB_MASTER_KEY}
ENV ADMIN_PRINC_PASS=${ADMIN_PRINC_PASS}
ENV LDAP_SERVICE_PASSWORD=${LDAP_SERVICE_PASSWORD}
ENV LDAP_SERVICE_DN=${LDAP_SERVICE_DN}
ENV LDAP_ADMIN_PASSWORD=${LDAP_ADMIN_PASSWORD}
ENV LDAP_ADMIN_DN=${LDAP_ADMIN_DN}

RUN echo "slapd slapd/domain string ${DOMAIN}" | debconf-set-selections && \
    echo "slapd slapd/organization string ${LDAP_ORGANIZATION}" | debconf-set-selections && \
    echo "slapd slapd/admin_password password ${LDAP_ADMIN_PASSWORD}" | debconf-set-selections && \
    echo "slapd slapd/admin_password_again password ${LDAP_ADMIN_PASSWORD}" | debconf-set-selections && \
    echo "slapd slapd/backend select MDB" | debconf-set-selections

RUN rm -rf /var/lib/ldap /etc/ldap/slapd.d && \
    apt-get install -y \
      slapd \
      schema2ldif \
      expect \
      gettext-base && \
      rm -rf /var/lib/apt/lists/*

EXPOSE 8389
EXPOSE 8636

CMD ["bash", "/entrypoint.sh"]
