# Keycloak to Kerberos

## Description

The part of the test system shows the usage of a `openldap` server and a `mit`
kerberos kdc as a keycloak user federation backend. 

## Usage LDAP

### Prerequisites

After starting the containers, you maybe need to re-enter the password for the
ldap access in the user federation area in the realm `fedldap`. 

* open `http://localhost:9080` and login with `admin`/`admin` 
* click the burger menu in the top left corner
* Manage realms
* choose `fedldap`
* Burger menu again
* User federation
* `ldap`
* at Bind credentials enter: P@ssw0rd123
* press Test authentication to test the credentials

### Login as User over ldap

* Clients
* Home URL of account-console: http://localhost:9080/realms/fedldap/account/
* Login with `user1@IAM.DEV` and `P@ssw0rd123`
* Sign in

## Usage MIT kerberos

### Login as User over mit kerberos kdc

* go to the realm `fedmit`
* Clients
* Home URL of account-console: http://localhost:9080/realms/fedmit/account/
* Login with `user1@IAM.DEV` and `P@ssw0rd123`
* Sign in
