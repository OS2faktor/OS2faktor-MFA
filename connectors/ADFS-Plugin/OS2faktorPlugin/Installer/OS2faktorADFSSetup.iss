; This file is a script that allows to build the OrgSyncer instalation package
; To generate the installer, define the variable MyAppSourceDir MUST point to the Directory where the dll's should be copied from
; The script may be executed from the console-mode compiler - iscc "c:\isetup\samples\my script.iss" or from the Inno Setup Compiler UI
#define AppId "{{99fc06ec-8285-4bd1-a018-58c74fca4987}"
#define AppSourceDir "Z:\projects\os2faktor\connectors\ADFS-Plugin\OS2faktorPlugin\OS2faktorPlugin\bin\Release"
#define AppName "OS2faktor"
#define AppVersion "2.6.0"
#define AppPublisher "Digital Identity"
#define AppURL "http://digital-identity.dk/"
#define AppExeName "OS2faktorADFSSetup.exe"

[Setup]
AppId={#AppId}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppURL}
AppSupportURL={#AppURL}
AppUpdatesURL={#AppURL}
DefaultDirName={pf}\{#AppPublisher}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
SetupLogging=yes
OutputBaseFilename=OS2faktorADFSSetup
Compression=lzma
SolidCompression=yes
SourceDir={#AppSourceDir}
OutputDir=..\..\..\Installer
SetupIconFile={#AppSourceDir}\..\..\..\Resources\di.ico
UninstallDisplayIcon={#AppSourceDir}\..\..\..\Resources\di.ico

[Registry]
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}";
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; Flags: uninsdeletekey
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}";
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "ConnectorVersion"; ValueData: "adfs-2.6.0"
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "ApiKey"; ValueData: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "CprField"; ValueData: "cprAttribute"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "PidField"; ValueData: ""; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "Debug"; ValueData: "false"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "PseudonymField"; ValueData: ""; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "DeviceIdField"; ValueData: ""; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "GetDeviceUrl"; ValueData: "https://www.os2faktor.dk/download.html"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "TrustAllSSLCerts"; ValueData: "false"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "AllowSelfRegistration"; ValueData: "false"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "RequirePin"; ValueData: "false"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "CprWebservice"; ValueData: ""; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "ConnectionString"; ValueData: ""; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "SQL"; ValueData: ""; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "SortByActive"; ValueData: "false"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "HmacKey"; ValueData: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "RememberDeviceAllowed"; ValueData: "false"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "RememberDeviceDays"; ValueData: "30"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: multisz; ValueName: "RememberDeviceRelyingParties"; ValueData: ""; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\{#AppName}"; ValueType: string; ValueName: "DisallowTotp"; ValueData: "false"; Flags: createvalueifdoesntexist

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "OS2faktorPlugin.dll"; DestDir: "{app}"; Flags: "ignoreversion gacinstall"; StrongAssemblyName: "OS2faktorPlugin, Version=1.0.1.0, Culture=neutral, PublicKeyToken=72670204a7176fed, processorArchitecture=MSIL";
Source: "Newtonsoft.Json.dll"; DestDir: "{app}"; Flags: "ignoreversion gacinstall"; StrongAssemblyName: "Json.NET, Version=11.0.2.21924, Culture=neutral, PublicKeyToken=30ad4fe6b2a6aeed, processorArchitecture=MSIL";
Source: "log4net.dll"; DestDir: "{app}"; Flags: "ignoreversion gacinstall"; StrongAssemblyName: "log4net, Version=2.0.8.0, Culture=neutral, PublicKeyToken=669e0ddf0bb1aa2a, processorArchitecture=MSIL";
Source: "RestSharp.dll"; DestDir: "{app}"; Flags: "ignoreversion gacinstall"; StrongAssemblyName: "RestSharp, Version=105.2.3.0, Culture=neutral, PublicKeyToken=598062e77f915f75, processorArchitecture=MSIL";
Source: "..\..\..\Resources\install.ps1"; DestDir: "{app}"; Flags: "ignoreversion";
Source: "..\..\..\Resources\uninstall.ps1"; DestDir: "{app}"; Flags: "ignoreversion";

[Tasks]
Name: "runpowershell"; Description: "Do you want to run AD FS setup script?"

[Code]
procedure CurStepChanged(CurStep: TSetupStep);
var
  ErrorCode: Integer;
  ReturnCode: Boolean;
begin
  if CurStep = ssPostInstall then begin

    if (IsTaskSelected('runpowershell')) then begin
      ExtractTemporaryFile('install.ps1');
      ReturnCode := ShellExec('open', '"PowerShell"', ExpandConstant(' -ExecutionPolicy Unrestricted -File "{tmp}\install.ps1"'), '', SW_SHOWNORMAL, ewWaitUntilTerminated, ErrorCode);

      if (ReturnCode = False) then begin
          MsgBox('Failed to setup AD FS. Error code: ' + IntToStr(ErrorCode) + ' ' + SysErrorMessage(ErrorCode), mbInformation, MB_OK);
      end;
    end;
  end;
end;
