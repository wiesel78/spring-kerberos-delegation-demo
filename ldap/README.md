# LDAP

A LDAP container for this testsystem. It will be deployed with the
mit part of this system.


## Testing

If you have the container running you can test it on the host with

```bash
ldapsearch -x -H ldap://localhost:8389 -D "cn=admin,dc=iam,dc=dev" -w "P@ssw0rd123" -b "cn=kdc-service,dc=iam,dc=dev" -s base -w "P@ssw0rd123"
```

or in the mit-client container with

```bash
ldapsearch -x -H ldap://ldap:389 -D "cn=admin,dc=iam,dc=dev" -w "P@ssw0rd123" -b "cn=kdc-service,dc=iam,dc=dev" -s base -w "P@ssw0rd123"
```

## Searching principal

searching for prefrontend

```bash
REALM=IAM.DEV
LDAP_KRB_CONTAINER_DN="cn=IAM.DEV,dc=iam,dc=dev"
service=HTTP/prefrontend.iam.dev@IAM.DEV
ldap_service="localhost:8389"

ldapsearch -x -H ldap://${ldap_service} -D "cn=admin,dc=iam,dc=dev" -b "krbPrincipalName=${service},cn=${REALM},${LDAP_KRB_CONTAINER_DN}" -s base -w "P@ssw0rd123"
```

searching for user1
```bash
REALM=IAM.DEV
LDAP_KRB_CONTAINER_DN="cn=IAM.DEV,dc=iam,dc=dev"
service=HTTP/prefrontend.iam.dev@IAM.DEV
ldap_service="localhost:8389"

ldapsearch -x -H ldap://${ldap_service} -D "cn=admin,dc=iam,dc=dev" -b "krbPrincipalName=${service},cn=${REALM},${LDAP_KRB_CONTAINER_DN}" -s base -w "P@ssw0rd123"
```

