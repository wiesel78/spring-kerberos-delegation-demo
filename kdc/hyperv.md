# HyperV : Windows Server 2025

A few notes on how to set up a Windows Server 2025 Hyper-V server.

## Download Windows Server 2022

You can download the Windows Server 2025 VHDX file from the Microsoft website :
https://www.microsoft.com/de-de/evalcenter/download-windows-server-2025

## Create a new VM

### Create a external switch using PowerShell

To bring the vm as standalone device into the network, you need to create an
external switch. To figure out the name of your network adapter, you can use
`ipconfig` or `Get-Adapter`. Then create the external switch and add it to the VM.

```powershell (as Admin)
$VMSwitch = "ExternalSwitch"
$NetAdapterName = "Ethernet"
New-VMSwitch -Name $VMSwitch -NetAdapterName $NetAdapterName -AllowManagementOS $true
```

### Create a new VM using PowerShell

```powershell (as Admin)
$VhdPath = "./windows-server-2025.vhdx"
$VMName = "WindowsServer2025"
$VMMemory = 8GB
$VMSwitch = "ExternalSwitch"

New-VM `
    -Name $VMName `
    -MemoryStartupBytes $VMMemory `
    -VHDPath $VhdPath `
    -Generation 2 `
    -SwitchName $VMSwitch `
    -ErrorAction Stop `
    -Path (Get-Location)

Set-VM -Name $VMName -CheckpointType Disabled
```


### Start the VM using PowerShell

```powershell (as Admin)
$VMName = "WindowsServer2025"

Start-VM -Name $VMName -ErrorAction Stop
```

### Connect to the VM using PowerShell

```powershell (as Admin)
$VMName = "WindowsServer2025"

VMConnect.exe localhost $VMName
```
