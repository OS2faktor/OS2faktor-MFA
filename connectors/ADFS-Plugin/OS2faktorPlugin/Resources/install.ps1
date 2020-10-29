# disable all MFA options in AD FS Console
Set-AdfsGlobalAuthenticationPolicy -AdditionalAuthenticationProvider {}

# uninstall any previous versions
Unregister-AdfsAuthenticationProvider -Name OS2faktorPlugin

# install the latest installed version
Register-AdfsAuthenticationProvider -TypeName "OS2faktorPlugin.OS2faktorAdapter, OS2faktorPlugin, Version=1.4.1.0, Culture=neutral, PublicKeyToken=72670204a7176fed" -Name "OS2faktorPlugin"

# for Windows Server 2019, ensure access to resources
if ((Get-WmiObject -class Win32_OperatingSystem).Caption -like '*2019*') {
  Set-AdfsResponseHeaders -SetHeaderName "Content-Security-Policy" -SetHeaderValue "default-src 'self' 'unsafe-inline' 'unsafe-eval' https://*.os2faktor.dk https://*.cloudflare.com;"
}

# finally enable in AD FS Console
Set-AdfsGlobalAuthenticationPolicy -AdditionalAuthenticationProvider {OS2faktorPlugin}
