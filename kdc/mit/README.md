# MIT Kerberos + OpenLDAP Container

## Getting started

### docker-compose

Run at the root of this repository

```bash
docker-compose up -d mit
```

### Test delegation with `kinit`

connect to the container:

```bash
docker-compose exec mit bash
```

Then run:

```bash
kinit user1@IAM.DEV
kvno HTTP/frontend.iam.dev@IAM.DEV
kinit -f -k -t /etc/keytabs/frontend.keytab HTTP/frontend.iam.dev@IAM.DEV
kvno -U user1 -P HTTP/backend.iam.dev@IAM.DEV
```

if at the end you see:

```
HTTP/backend.iam.dev@IAM.DEV: kvno = 2
```

then delegation is working.

