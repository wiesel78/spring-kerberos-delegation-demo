# PowerShell as a client

if you dont already logged in as the user you want to use, you can use `runas` 
to start a new PowerShell session as that user. 

```powershell
runas /user:IAM\user1 "powershell.exe"
```

In this powershell session, you can use the `-UseDefaultCredentials` flag to 
use the credentials of the user you started the session with. 

```powershell
Invoke-WebRequest -Uri "http://frontend.iam.dev:8081/api/v1/todos" -UseDefaultCredentials -UseBasicParsing
```