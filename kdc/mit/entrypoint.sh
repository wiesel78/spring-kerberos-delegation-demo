#!/bin/bash

echo "VERSION: 1"
echo "LDAP_KRB_CONTAINER_DN: ${LDAP_KRB_CONTAINER_DN}"
echo "LDAP_SERVER_URL: ${LDAP_SERVER_URL}"

# write password file for access to the ldap server
echo -n "${LDAP_ADMIN_PASSWORD}" > /tmp/ldap_admin_password
chmod 600 /tmp/ldap_admin_password

# create the kerberos subtree in ldap
kdb5_ldap_util -D ${LDAP_ADMIN_DN} -w ${ADMIN_PRINC_PASS} create -subtrees ${LDAP_KRB_CONTAINER_DN} -r ${REALM} -P "${ADMIN_PRINC_PASS}" -s -H "${LDAP_SERVER_URL}"

mkdir -p ${KEYTABS_PATH}

# add admin user
kadmin.local -q "addprinc -pw ${ADMIN_PRINC_PASS} admin/admin@${REALM}"

# collect all properties in use to unset them in the loop
PROPERTY_KEYS=$(cat /etc/provision/accounts.yml | \
  yq -r '[.users[] | to_entries | map(.key)[]] | unique | map("unset \(.)") | join("\n")')

# read users from the config file and add them to kerberos and ldap
cat /etc/provision/accounts.yml | \
  yq -r '.users[] | to_entries | map("export \(.key)=\(.value)") | join(" ")' | \
  while read -r LINE; do
    eval "$property_keys"
    eval "$LINE"

    kadmin.local -q "addprinc -pw ${password} +preauth +forwardable +proxiable +ok_as_delegate ${name}@${REALM}"
  done

echo "add services..."
PROPERTY_KEYS=$(cat /etc/provision/accounts.yml | \
  yq -r '[.services[] | to_entries | map(.key)[]] | unique | map("unset \(.)") | join("\n")')
echo "possible keys to cleanup $PROPERTY_KEYS"

cat /etc/provision/accounts.yml | \
  yq -r '.services[] | to_entries | map("export \(.key)=\(if (.value | type) == "array" then "'"'"'\(.value)'"'"'" else .value end)") | join(" ")' | \
  while read -r LINE; do
    echo "property keys : $PROPERTY_KEYS"
    eval "$PROPERTY_KEYS"
    echo "LINE: $LINE"
    eval "$LINE"

    echo "addprinc"
    kadmin.local -q "addprinc -randkey +ok_to_auth_as_delegate +ok_as_delegate +preauth ${name}@${REALM}"
    echo "ktadd"
    rm -f "${KEYTABS_PATH}/${keytab}"
    kadmin.local -q "ktadd -k ${KEYTABS_PATH}/${keytab} ${name}@${REALM}"

    echo "add delegation: $delegates"
    if [[ $(echo "${delegates:-[]}" | jq 'length') > 0 ]]; then
      echo "delegates will be set"
      delegation_targets=$(echo "$delegates" | jq -r 'map("krbAllowedToDelegateTo: \(.)@'${REALM}'") | join("\n")')

      echo "delegate targets: $delegation_targets"
      ldapmodify -x -D ${LDAP_ADMIN_DN} -y /tmp/ldap_admin_password -H "${LDAP_SERVER_URL}" <<-EOF
dn: krbPrincipalName=${name}@${REALM},cn=${REALM},${LDAP_KRB_CONTAINER_DN}
changetype: modify
add: krbAllowedToDelegateTo
${delegation_targets}
EOF
    fi
  done

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
