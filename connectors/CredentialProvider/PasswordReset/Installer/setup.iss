; This file is a script that allows to build the OrgSyncer instalation package
; To generate the installer, define the variable MyAppSourceDir MUST point to the Directory where the dll's should be copied from
; The script may be executed from the console-mode compiler - iscc "c:\isetup\samples\my script.iss" or from the Inno Setup Compiler UI
#define AppId "{{603ce31d-fdd5-45db-a458-2b7e78a2988a}"
#define AppSourceDir "z:\projects\os2faktor\connectors\CredentialProvider\PasswordReset\bin\Debug"
#define AppName "OS2faktor Password Reset Credential Provider"
#define AppVersion "1.1.0"
#define AppPublisher "Digital Identity"
#define AppURL "http://digital-identity.dk/"
#define CredentialProviderId "{{9d4d50a6-63ac-43c0-a65e-9039cfa792b5}"
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
OutputBaseFilename=OS2faktor Password Reset Credential Provider
Compression=lzma
SolidCompression=yes
SourceDir={#AppSourceDir}
OutputDir=..\..\..
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64

[Registry]
Root: HKLM; Subkey: {#RegKey};
Root: HKLM; Subkey: {#RegKey}; ValueType: string; ValueName: ""; ValueData: "OS2Faktor Password Reset Credential Provider"
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\OS2faktorCP";
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\OS2faktorCP"; ValueType: string; ValueName: "ProxyUrl"; ValueData: "https://proxy.dk:9500"; Flags: createvalueifdoesntexist
Root: HKLM; Subkey: "SOFTWARE\{#AppPublisher}\OS2faktorCP"; ValueType: string; ValueName: "AllowedUrls"; ValueData: "applet.danid.dk;proxy.dk"; Flags: createvalueifdoesntexist

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "Images\*"; DestDir: "{app}\Images"; Flags: ignoreversion
Source: "locales\*"; DestDir: "{app}\locales"; Flags: ignoreversion
Source: "swiftshader\*"; DestDir: "{app}\swiftshader"; Flags: ignoreversion
Source: "cef.pak"; DestDir: "{app}"; Flags: ignoreversion
Source: "CefSharp.BrowserSubprocess.Core.dll"  ; DestDir: "{app}"; Flags: ignoreversion
Source: "CefSharp.BrowserSubprocess.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "CefSharp.Core.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "CefSharp.Core.xml"; DestDir: "{app}"; Flags: ignoreversion
Source: "CefSharp.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "CefSharp.WinForms.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "CefSharp.WinForms.XML"; DestDir: "{app}"; Flags: ignoreversion
Source: "CefSharp.XML"; DestDir: "{app}"; Flags: ignoreversion
Source: "cef_100_percent.pak"; DestDir: "{app}"; Flags: ignoreversion
Source: "cef_200_percent.pak"; DestDir: "{app}"; Flags: ignoreversion
Source: "cef_extensions.pak"; DestDir: "{app}"; Flags: ignoreversion
Source: "chrome_elf.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "d3dcompiler_47.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "devtools_resources.pak"; DestDir: "{app}"; Flags: ignoreversion
Source: "icudtl.dat"; DestDir: "{app}"; Flags: ignoreversion
Source: "libcef.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "libEGL.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "libGLESv2.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "log4net.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "natives_blob.bin"; DestDir: "{app}"; Flags: ignoreversion
Source: "PasswordReset.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "PasswordReset.dll.config"; DestDir: "{app}"; Flags: ignoreversion
Source: "README.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "snapshot_blob.bin"; DestDir: "{app}"; Flags: ignoreversion
Source: "v8_context_snapshot.bin"; DestDir: "{app}"; Flags: ignoreversion
; VC++ redistributable runtime. Extracted by VC2017RedistNeedsInstall(), if needed.
Source: "Redist\vc_redist.x64.exe"; DestDir: {tmp}; Flags: dontcopy

[Run]
; install VC++ redistributable runtime
Filename: "{tmp}\vc_redist.x64.exe"; StatusMsg: "{cm:InstallingVC2017redist}"; Parameters: "/quiet"; Check: VC2017RedistNeedsInstall ; Flags: waituntilterminated

; register .Net components for com+
Filename: {win}\Microsoft.NET\Framework64\v4.0.30319\RegAsm.exe; Parameters: """{app}\PasswordReset.dll"" /codebase ""{app}\PasswordReset.dll"""; Description: Component registration; WorkingDir: {app}; StatusMsg: Component registration...; Flags: waituntilterminated

[UninstallRun]
Filename: {win}\Microsoft.NET\Framework64\v4.0.30319\RegAsm.exe; Parameters: "/unregister ""{app}\PasswordReset.dll"""; WorkingDir: {app}; StatusMsg: Component un-registration...; Flags: waituntilterminated

[CustomMessages]
AlreadyInstalled ={#AppName} is already installed. Please remove it using the Add or remove programs menu. 
InstallingVC2017redist = Installing VC++ 2017 Redistributables...

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
        if not ShellExec('', ExpandConstant('{win}\Microsoft.NET\Framework64\v4.0.30319\RegAsm.exe'), ExpandConstant('/unregister "{app}\PasswordReset.dll"'), '', SW_SHOW, ewWaitUntilTerminated, ErrorCode) then
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

  function VC2017RedistNeedsInstall: Boolean;
  var 
    Version: String;
  begin
    if (RegQueryStringValue(HKEY_LOCAL_MACHINE, 'SOFTWARE\Microsoft\VisualStudio\14.0\VC\Runtimes\x64', 'Version', Version)) then
    begin
      // Is the installed version at least 14.16 ? 
      Log('VC Redist Version check : found ' + Version);
      Result := (CompareStr(Version, 'v14.16.27024.01')<0);
    end
    else 
    begin
      // Not even an old version installed
      Result := True;
    end;
    if (Result) then
    begin
      ExtractTemporaryFile('vc_redist.x64.exe');
    end;
  end;
