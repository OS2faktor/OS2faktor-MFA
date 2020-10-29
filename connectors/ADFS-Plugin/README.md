This is how to install on Windows Server (AD FS)

1) Make sure the AD FS is not currently using the plugin (Authentication Policies -> MFA section)

2) Make sure the uninstaller has been executed in admin-powershell, so this command has been executed
> Unregister-AdfsAuthenticationProvider -Name OS2faktorPlugin

3) Run the installer MSI

4) restart AD FS

5) Enable MFA in AD FS console (see step 1)