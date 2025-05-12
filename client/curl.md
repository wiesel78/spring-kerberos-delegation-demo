# CURL as a client

In unix-like systems, you can use `curl` to make requests and automatically handle the negotiate
process to get a TGS and use it for authentication.

```bash
curl --negotiate -u : http://frontend.iam.dev:8081/api/v1/todos
```