#!/bin/bash


echo -n "${LDAP_ADMIN_PASSWORD}" > /tmp/ldap_admin_password
chmod 600 /tmp/ldap_admin_password
ldap_admin_password_hash=$(slappasswd -T /tmp/ldap_admin_password)

echo "Starting LDAP service listen to ${SLAPD_SERVICES}"
service slapd start

# set admin password for ldap
ldapmodify -Q -Y EXTERNAL -H ldapi:/// <<EOF
dn: olcDatabase={1}mdb,cn=config
changetype: modify
replace: olcRootPW
olcRootPW: ${ldap_admin_password_hash}
EOF

# add kerberos schema to ldap
curl https://raw.githubusercontent.com/krb5/krb5/refs/heads/master/src/plugins/kdb/ldap/libkdb_ldap/kerberos.schema -o /etc/ldap/schema/kerberos.schema
ldap-schema-manager -i /etc/ldap/schema/kerberos.schema
ldapmodify -Q -Y EXTERNAL -H ldapi:/// <<EOF
dn: olcDatabase={1}mdb,cn=config
add: olcDbIndex
olcDbIndex: krbPrincipalName eq,pres,sub
EOF

# add the service user to the ldap
ldapadd -x -D ${LDAP_ADMIN_DN} -y /tmp/ldap_admin_password -H ldapi:/// <<EOF
dn: ${LDAP_SERVICE_DN}
cn: $(echo ${LDAP_SERVICE_DN} | sed -E "s/.*cn=([^,]+).*/\1/g")
objectClass: simpleSecurityObject
objectClass: organizationalRole
userPassword: ${LDAP_SERVICE_PASSWORD}
description: Kerberos KDC Service Account
EOF

# set full access of kerberos tree to admin and service user
ldapmodify -Y EXTERNAL -H ldapi:/// <<EOF
dn: olcDatabase={1}mdb,cn=config
changetype: modify
add: olcAccess
olcAccess: {0}to dn.subtree="${LDAP_KRB_CONTAINER_DN}"
  by dn="${LDAP_SERVICE_DN}" write
  by dn="${LDAP_ADMIN_DN}" write
  by * none
EOF

tail -f /dev/null
