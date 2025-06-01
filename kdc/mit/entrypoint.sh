#!/bin/bash

echo -n "${LDAP_ADMIN_PASSWORD}" > /tmp/ldap_admin_password
chmod 600 /tmp/ldap_admin_password
ldap_admin_password_hash=$(slappasswd -T /tmp/ldap_admin_password)

echo "Starting LDAP service..."
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

ldapadd -x -D ${LDAP_ADMIN_DN} -y /tmp/ldap_admin_password -H ldapi:/// <<EOF
dn: ${LDAP_SERVICE_DN}
cn: $(echo ${LDAP_SERVICE_DN} | sed -E "s/.*cn=([^,]+).*/\1/g")
objectClass: simpleSecurityObject
objectClass: organizationalRole
userPassword: ${LDAP_SERVICE_PASSWORD}
description: Kerberos KDC Service Account
EOF

ldapmodify -Y EXTERNAL -H ldapi:/// <<EOF
dn: olcDatabase={1}mdb,cn=config
changetype: modify
add: olcAccess
olcAccess: {0}to dn.subtree="${LDAP_KRB_CONTAINER_DN}"
  by dn="${LDAP_SERVICE_DN}" write
  by dn="${LDAP_ADMIN_DN}" write
  by * none
EOF

kdb5_ldap_util -D ${LDAP_ADMIN_DN} -w ${ADMIN_PRINC_PASS} create -subtrees ${LDAP_KRB_CONTAINER_DN} -r ${REALM} -P "${ADMIN_PRINC_PASS}" -s -H ldapi:///

mkdir -p ${KEYTABS_PATH}

kadmin.local -q "addprinc -pw ${ADMIN_PRINC_PASS} admin/admin@${REALM}"
kadmin.local -q "addprinc -pw P@ssw0rd123 +preauth +forwardable +proxiable +ok_as_delegate user1@${REALM}"
kadmin.local -q "addprinc -randkey +ok_to_auth_as_delegate +ok_as_delegate +preauth HTTP/frontend.${DOMAIN}@${REALM}"
kadmin.local -q "ktadd -k ${KEYTABS_PATH}/frontend.keytab HTTP/frontend.${DOMAIN}@${REALM}"
kadmin.local -q "addprinc -randkey +preauth +ok_as_delegate HTTP/backend.${DOMAIN}@${REALM}"
kadmin.local -q "ktadd -k ${KEYTABS_PATH}/backend.keytab HTTP/backend.${DOMAIN}@${REALM}"


ldapmodify -x -D ${LDAP_ADMIN_DN} -y /tmp/ldap_admin_password -H ldapi:/// <<EOF
dn: krbPrincipalName=HTTP/frontend.${DOMAIN}@${REALM},cn=${REALM},${LDAP_KRB_CONTAINER_DN}
changetype: modify
add: krbAllowedToDelegateTo
krbAllowedToDelegateTo: HTTP/backend.${DOMAIN}@${REALM}
EOF

set -e

# Start Kerberos KDC
echo "Starting Kerberos KDC (krb5-kdc)..."
service krb5-kdc start

# Start Kerberos Admin Server
echo "Starting Kerberos Admin Server (kadmind)..."
service krb5-admin-server start

echo "Cleaning up history and environment variables..."
history -c
unset HISTFILE
unset LDAP_ADMIN_PASSWORD
unset LDAP_SERVICE_PASSWORD
unset KRB_MASTER_KEY
unset ADMIN_PRINC_PASS
rm -f ~/.bash_history

echo "Kerberos services started. Tailing /dev/null to keep container running."
# Keep the container running
tail -f /dev/null