; This file is a script that allows to build the OrgSyncer instalation package
; To generate the installer, define the variable MyAppSourceDir MUST point to the Directory where the dll's should be copied from
; The script may be executed from the console-mode compiler - iscc "c:\isetup\samples\my script.iss" or from the Inno Setup Compiler UI
#define AppId "{{9dc5bb40-a376-4209-b063-2c24c9097835}"
#define AppSourceDir "e:\projects\os2faktor\connectors\CredentialProvider\Login\bin\Debug"
#define AppName "OS2faktor Login Credential Provider"
#define AppVersion "0.1"
#define AppPublisher "Digital Identity"
#define AppURL "http://digital-identity.dk/"
#define CredentialProviderId "{{838fa51b-9659-4c03-8409-9510d8e9b7be}"
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
OutputBaseFilename=OS2faktor Login Credential Provider
Compression=lzma
SolidCompression=yes
SourceDir={#AppSourceDir}
OutputDir=..\..\..
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64

[Registry]
Root: HKLM; Subkey: {#RegKey};
Root: HKLM; Subkey: {#RegKey}; ValueType: string; ValueName: ""; ValueData: "OS2Faktor Login Credential Provider"
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\OS2faktorCP";
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\OS2faktorCP"; ValueType: string; ValueName: "ProxyUrl"; ValueData: "https://proxy.dk:9500"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\OS2faktorCP"; ValueType: string; ValueName: "CertificateThumbprint"; ValueData: "7783d040dc4b06c038b6d621c06d4a0307ace2e7"; Flags: createvalueifdoesntexist

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "Images\*"; DestDir: "{app}\Images"; Flags: ignoreversion
Source: "log4net.config"; DestDir: "{app}"; Flags: ignoreversion
Source: "log4net.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "log4net.xml"; DestDir: "{app}"; Flags: ignoreversion
Source: "Login.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "Login.dll.config"; DestDir: "{app}"; Flags: ignoreversion

[Run]
; register .Net components for com+
Filename: {win}\Microsoft.NET\Framework64\v4.0.30319\RegAsm.exe; Parameters: """{app}\Login.dll"" /codebase ""{app}\Login.dll"""; Description: Component registration; WorkingDir: {app}; StatusMsg: Component registration...; Flags: waituntilterminated

[UninstallRun]
Filename: {win}\Microsoft.NET\Framework64\v4.0.30319\RegAsm.exe; Parameters: "/unregister ""{app}\Login.dll"""; WorkingDir: {app}; StatusMsg: Component un-registration...; Flags: waituntilterminated

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
        if not ShellExec('', ExpandConstant('{win}\Microsoft.NET\Framework64\v4.0.30319\RegAsm.exe'), ExpandConstant('/unregister "{app}\Login.dll"'), '', SW_SHOW, ewWaitUntilTerminated, ErrorCode) then
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
