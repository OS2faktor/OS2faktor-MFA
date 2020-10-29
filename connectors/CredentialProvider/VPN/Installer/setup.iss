; This file is a script that allows to build the OrgSyncer instalation package
; To generate the installer, define the variable MyAppSourceDir MUST point to the Directory where the dll's should be copied from
; The script may be executed from the console-mode compiler - iscc "c:\isetup\samples\my script.iss" or from the Inno Setup Compiler UI
#define AppId "{{9dc5bb40-a376-4209-b063-2c24c9097835}"
#define AppSourceDir "e:\projects\os2faktor\connectors\CredentialProvider\VPN\bin\x64\Debug"
#define LauncherBinaryDir "e:\projects\os2faktor\connectors\CredentialProvider\VPNLauncher\bin\x64\Debug"
#define AppName "OS2faktor VPN Credential Provider"
#define AppVersion "1.3.0"
#define AppPublisher "Digital Identity"
#define AppURL "https://www.os2faktor.dk/"
#define CredentialProviderId "{{62b8f223-6e50-453f-af28-4bab3c7a7002}"
#define RegKey "SOFTWARE\Microsoft\Windows\CurrentVersion\Authentication\Credential Providers\" + CredentialProviderId

[Setup]
AppId={#AppId}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppURL}
AppSupportURL={#AppURL}
AppUpdatesURL={#AppURL}
DefaultDirName={pf}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
OutputBaseFilename=OS2faktor VPN Credential Provider
Compression=lzma
SolidCompression=yes
SourceDir={#AppSourceDir}
OutputDir=..\..\..\Installer
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64

[Registry]
Root: HKLM; Subkey: {#RegKey};
Root: HKLM; Subkey: {#RegKey}; ValueType: string; ValueName: ""; ValueData: "OS2Faktor VPN Credential Provider"
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\OS2faktorCP";
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\OS2faktorCP"; ValueType: string; ValueName: "Debug"; ValueData: "false"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\OS2faktorCP"; ValueType: string; ValueName: "RunMode"; ValueData: "BEFORE_LOGIN"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\OS2faktorCP"; ValueType: string; ValueName: "TempFolder"; ValueData: "c:\temp"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\OS2faktorCP"; ValueType: string; ValueName: "PowershellFile"; ValueData: "C:\Program Files\OS2faktor VPN Credential Provider\example.ps1"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "OS2faktorVPN"; ValueData: "{app}\VPNLauncher.exe"; Flags: createvalueifdoesntexist

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "Images\*"; DestDir: "{app}\Images"; Flags: ignoreversion
Source: "log4net.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "VPN.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "example.ps1"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#LauncherBinaryDir}\VPNLauncher.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#LauncherBinaryDir}\VPNLauncher.exe.config"; DestDir: "{app}"; Flags: ignoreversion

[Run]
; register .Net components for com+
Filename: {win}\Microsoft.NET\Framework64\v4.0.30319\RegAsm.exe; Parameters: """{app}\VPN.dll"" /codebase ""{app}\VPN.dll"""; Description: Component registration; WorkingDir: {app}; StatusMsg: Component registration...; Flags: waituntilterminated

[UninstallRun]
Filename: {win}\Microsoft.NET\Framework64\v4.0.30319\RegAsm.exe; Parameters: "/unregister ""{app}\VPN.dll"""; WorkingDir: {app}; StatusMsg: Component un-registration...; Flags: waituntilterminated

[CustomMessages]
AlreadyInstalled ={#AppName} is already installed. Please remove it using the Add or remove programs menu. 

[Code]

  // find current version before installation
  function InitializeSetup: Boolean;
  begin
  if RegKeyExists(HKEY_LOCAL_MACHINE,'SOFTWARE\{#AppPublisher}\{#AppName}') then
      begin
        MsgBox(ExpandConstant('{cm:AlreadyInstalled}') , mbError, MB_OK);
        Result := False;
        exit;
      end
        Result := True;
  end;

  // Uninstall registrey entries
  procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
  var
    ErrorCode: Integer;
  begin
    if CurUninstallStep = usUninstall then
    begin
        if not ShellExec('', ExpandConstant('{win}\Microsoft.NET\Framework64\v4.0.30319\RegAsm.exe'), ExpandConstant('/unregister "{app}\VPN.dll"'), '', SW_SHOW, ewWaitUntilTerminated, ErrorCode) then
        begin
            MsgBox('Error code: ' + IntToStr(ErrorCode), mbError, MB_OK);
        end;
    end;
    if CurUninstallStep = usPostUninstall then
    begin
      if RegKeyExists(HKEY_LOCAL_MACHINE, ExpandConstant('{#RegKey}')) then
      begin
        RegDeleteKeyIncludingSubkeys(HKEY_LOCAL_MACHINE, ExpandConstant('{#RegKey}'));
      end;
    end;
  end;
