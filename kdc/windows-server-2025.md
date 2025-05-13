# Active Directory with Windows Server 2025

Setup Active Directory on Windows Server 2025

## Setup Active Directory using PowerShell

Activate Active Directory Domain Services (AD DS) on Windows Server 2025 using 
PowerShell. This will install the necessary features and create a new 
Active Directory forest. Maybe a restart is required.

```powershell
Install-WindowsFeature -Name AD-Domain-Services -IncludeManagementTools
Install-ADDSForest -DomainName iam.dev -SafeModeAdministratorPassword (Get-Credential).Password
```

## Add users using PowerShell

I define `$SecurePassword` once and use it for all users, because it is a 
test system. That is obviously insecure, so adapt it to your needs.

```powershell
$SecurePassword = Read-Host -AsSecureString "Enter Password"
```

### Add three principals

```powershell
New-ADUser -Name "user1" -SamAccountName "user1" -UserPrincipalName "user1@IAM.DEV" -Path "CN=Users,DC=IAM,DC=DEV" -AccountPassword $SecurePassword -Enabled $true -PasswordNeverExpires $true -ChangePasswordAtLogon $false
New-ADUser -Name "user2" -SamAccountName "user2" -UserPrincipalName "user2@IAM.DEV" -Path "CN=Users,DC=IAM,DC=DEV" -AccountPassword $SecurePassword -Enabled $true -PasswordNeverExpires $true -ChangePasswordAtLogon $false
New-ADUser -Name "user3" -SamAccountName "user3" -UserPrincipalName "user3@IAM.DEV" -Path "CN=Users,DC=IAM,DC=DEV" -AccountPassword $SecurePassword -Enabled $true -PasswordNeverExpires $true -ChangePasswordAtLogon $false
```

### Add frontend and backend service principals

```powershell
$SecurePassword = Read-Host -AsSecureString "Enter Password"

New-ADUser `
    -Name "Frontend Service Account" `
    -SamAccountName "frontend_svc" `
    -UserPrincipalName "HTTP/frontend.iam.dev@IAM.DEV" `
    -Path "CN=Users,DC=IAM,DC=DEV" `
    -AccountPassword $SecurePassword `
    -Enabled $true `
    -ServicePrincipalNames "HTTP/frontend.iam.dev"

New-ADUser `
    -Name "Backend Service Account" `
    -SamAccountName "backend_svc" `
    -UserPrincipalName "HTTP/backend.iam.dev@IAM.DEV" `
    -Path "CN=Users,DC=IAM,DC=DEV" `
    -AccountPassword $SecurePassword `
    -Enabled $true `
    -ServicePrincipalNames "HTTP/backend.iam.dev"
```

### Add permission to delegate

The frontend service account needs permission to delegate to the backend service account.

```powershell
Set-ADAccountControl -Identity frontend_svc -TrustedToAuthForDelegation $true
Set-ADUser -Identity frontend_svc -Add @{
    'msDS-AllowedToDelegateTo' = 'HTTP/backend.iam.dev'
}
Set-ADUser -Identity "frontend_svc" -Replace @{
    "msDS-SupportedEncryptionTypes" = 0x18
}

Set-ADUser -Identity "backend_svc" -Replace @{
    "msDS-SupportedEncryptionTypes" = 0x18
}
```

### Create Keytabs

```powershell
$SecurePassword = "P@ssw0rd123"

mkdir ~/keytabs
ktpass -out C:\Users\Administrator\keytabs\frontend.keytab -princ HTTP/frontend.iam.dev@IAM.DEV -mapuser frontend_svc@IAM.DEV -pass $SecurePassword -ptype KRB5_NT_PRINCIPAL -crypto AES256-SHA1
ktpass -out C:\Users\Administrator\keytabs\backend.keytab -princ HTTP/backend.iam.dev@IAM.DEV -mapuser backend_svc@IAM.DEV -pass $SecurePassword -ptype KRB5_NT_PRINCIPAL -crypto AES256-SHA1
```

### Add Domains to DNS

Figure out the ip address of your host and the vm and put it in for the 
services and kdc domains (host is maybe with ipconfig → gateway)

```powershell
$hostIp = "192.168.1.2"
Add-DnsServerResourceRecordA -Name "web" -ZoneName "IAM.DEV" -IPv4Address $hostIp
Add-DnsServerResourceRecordA -Name "frontend" -ZoneName "IAM.DEV" -IPv4Address $hostIp
Add-DnsServerResourceRecordA -Name "backend" -ZoneName "IAM.DEV" -IPv4Address $hostIp

$vmIp = "192.168.1.3"
Add-DnsServerResourceRecordA -Name "kdc" -ZoneName "IAM.DEV" -IPv4Address $vmIp
```

you can check the dns records with

```powershell
Get-DnsServerResourceRecord -ZoneName "IAM.DEV" | Where-Object HostName -in "web", "frontend", "backend", "kdc"
```

### Add domains to trusted domains

to allow browsers authenticating on this domains, we have to add them to the trusted domains

* `Win+R` -> `inetcpl.cpl`
* `Security` -> `Local Intranet` -> `Sites`
* add the domains `*.iam.dev` and `*.kdc.iam.dev`