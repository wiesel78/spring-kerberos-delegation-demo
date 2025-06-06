# Constrained Delegation

This repository contains a setup to demonstrate constrained delegation. Either 
MIT Kerberos or Active Directory can be used as the KDC (Key Distribution Center).

## Setup your environment

on the host machine you need to have the following dns addresses pointing to
the ip address of the host machine.

```
<HOST-IP> backend.iam.dev
<HOST-IP> frontend.iam.dev
```

your host ip you can get with `ipconfig` for windows and `ifconfig` or `ip addr` for linux.

the reason we need the dns entries is, that a client like curl or a browser resolve
usually the SPN (Service Principal Name) from the dns address of the service you
send a request to. for example the frontend service `frontend.iam.dev:8081` has
the SPN `HTTP/frontend.iam.dev`.

## Active Directory kdc and client

setup or adapt your active directory as described in 
* [Setup a Windows Server VM in Hyper-V](kdc/active-directory/hyperv.md).
* [Setup Active Directory in Windows Server](kdc/active-directory/windows-server-2025.md).

after that you can deploy a client container to login as a kerberos user and trigger
the frontend service.

```bash
docker-compose up -d ad-client
```

go into the client container

```bash
docker-compose exec ad-client bash
```

in the client container login with the kinit command

```bash
kinit user1@IAM.DEV
```

type in the password `P@ssw0rd123`.

now you can start the frontend and backend project with the run configuration with the mit suffix.

after that you can send a request to the frontend service from the client container with the curl command

```bash
curl --negotiate -u : http://frontend.iam.dev:8081/api/v1/todos
```


## MIT Kerberos kdc and client

to deploy the mit kerberos kdc and the client with the settings for the mit
kerberos kdc run :

```bash
docker-compose up -d mit mit-client
```

go into the client container

```bash
docker-compose exec mit-client bash
```

in the client container login with the kinit command

```bash
kinit user1@IAM.DEV
```

type in the password `P@ssw0rd123`

now you can start the frontend and backend project with the run configuration with the mit suffix.

after that you can send a request to the frontend service from the client container with the curl command

```bash
curl --negotiate -u : http://frontend.iam.dev:8081/api/v1/todos
```

you can also test the constrained delegation with the MIT Kerberos tools

```bash
kinit user1@IAM.DEV
kvno HTTP/frontend.iam.dev@IAM.DEV
kinit -f -k -t /etc/keytabs/frontend.keytab HTTP/frontend.iam.dev@IAM.DEV
kvno -U user1 -P HTTP/backend.iam.dev@IAM.DEV
```
