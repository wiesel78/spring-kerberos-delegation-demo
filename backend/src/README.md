# Demo Backend

## Test Backend API

### Create kerberos session (TGT)

```bash
kinit user1@IAM.DEV
```

### GET all todos

```bash
curl --negotiate -u : http://backend.iam.dev:8082/api/v1/todos | jq
```

### Create a todo

```bash
curl --negotiate -u : -X POST http://backend.iam.dev:8082/api/v1/todos \
  -H "Content-Type: application/json" \
  -d '{"title": "Test todo", "description": "Test description"}' | jq
```