﻿using CredProvider.NET.Interop;
using log4net;
using log4net.Appender;
using log4net.Layout;
using PasswordReset.Service;
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Net;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Windows.Forms;
using static PasswordReset.Constants;

namespace PasswordReset
{
    public class CredentialProviderCredential : ICredentialProviderCredential
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private readonly CredentialView view;
        private ICredentialProviderCredentialEvents credentialProviderCredentialEvents;
        private static bool initialized = false;

        public CredentialProviderCredential(CredentialView view)
        {
            if (!initialized)
            {
                try
                {
                    PatternLayout patternLayout = new PatternLayout();
                    patternLayout.ConversionPattern = "%date - %-5level %logger - %message%newline";
                    patternLayout.ActivateOptions();

                    RollingFileAppender appender = new RollingFileAppender();
                    appender.Layout = patternLayout;
                    appender.File = "c:/logs/os2faktor/passwordreset.log";
                    appender.AppendToFile = true;
                    appender.RollingStyle = RollingFileAppender.RollingMode.Size;
                    appender.MaxSizeRollBackups = 5;
                    appender.MaximumFileSize = "1MB";
                    appender.StaticLogFileName = true;
                    appender.ActivateOptions();

                    var logRepository = (log4net.Repository.Hierarchy.Hierarchy)LogManager.GetRepository();
                    logRepository.Root.AddAppender(appender);

                    logRepository.Root.Level = Config.GetLogLevel(); ;
                    logRepository.Configured = true;
                }
                catch (Exception ex)
                {
                    try
                    {
                        System.IO.File.WriteAllText(@"c:\logs\os2faktor\passwordreset-fail.log", "failure: " + ex.Message + "\n" + ex.StackTrace);
                    }
                    catch (Exception)
                    {
                        ; // ignore
                    }
                }

                initialized = true;
            }

            log.Debug("CredentialType: "+ view.GetType());

            this.view = view;
        }

        public virtual int Advise(ICredentialProviderCredentialEvents pcpce)
        {
            log.Debug("Advise");

            if (pcpce != null)
            {
                credentialProviderCredentialEvents = pcpce;
                var intPtr = Marshal.GetIUnknownForObject(pcpce);
                Marshal.AddRef(intPtr);
            }

            return HRESULT.S_OK;
        }

        public virtual int UnAdvise()
        {
            log.Debug("UnAdvise");

            if (credentialProviderCredentialEvents != null)
            {
                var intPtr = Marshal.GetIUnknownForObject(credentialProviderCredentialEvents);
                Marshal.Release(intPtr);
                credentialProviderCredentialEvents = null;
            }

            return HRESULT.S_OK;
        }

        public virtual int SetSelected(out int pbAutoLogon)
        {
            log.Debug("SetSelected");

            pbAutoLogon = 0;

            return HRESULT.S_OK;
        }

        public virtual int SetDeselected()
        {
            log.Debug("SetDeselected");

            return HRESULT.E_NOTIMPL;
        }

        public virtual int GetFieldState(uint dwFieldID, out _CREDENTIAL_PROVIDER_FIELD_STATE pcpfs, out _CREDENTIAL_PROVIDER_FIELD_INTERACTIVE_STATE pcpfis)
        {
            log.Debug($"dwFieldID: {dwFieldID}");

            view.GetFieldState((int)dwFieldID, out pcpfs, out pcpfis);

            return HRESULT.S_OK;
        }

        public virtual int GetStringValue(uint dwFieldID, out string ppsz)
        {
            log.Debug($"dwFieldID: {dwFieldID}");

            ppsz = view.GetValue((int)dwFieldID);

            return HRESULT.S_OK;
        }

        public virtual int GetBitmapValue(uint dwFieldID, out IntPtr phbmp)
        {
            log.Debug($"dwFieldID: {dwFieldID}");

            phbmp = view.GetBitmap(dwFieldID);

            return HRESULT.S_OK;
        }

        public virtual int GetCheckboxValue(uint dwFieldID, out int pbChecked, out string ppszLabel)
        {
            log.Debug($"dwFieldID: {dwFieldID}");

            pbChecked = 0;
            ppszLabel = "";

            return HRESULT.E_NOTIMPL;
        }

        public virtual int GetSubmitButtonValue(uint dwFieldID, out uint pdwAdjacentTo)
        {
            log.Debug($"dwFieldID: {dwFieldID}");

            pdwAdjacentTo = 1;

            return HRESULT.S_OK;
        }

        public virtual int GetComboBoxValueCount(uint dwFieldID, out uint pcItems, out uint pdwSelectedItem)
        {
            log.Debug($"dwFieldID: {dwFieldID}");

            pcItems = 0;
            pdwSelectedItem = 0;

            return HRESULT.E_NOTIMPL;
        }

        public virtual int GetComboBoxValueAt(uint dwFieldID, uint dwItem, out string ppszItem)
        {
            log.Debug($"dwFieldID: {dwFieldID}; dwItem: {dwItem}");

            ppszItem = "";

            return HRESULT.E_NOTIMPL;
        }

        public virtual int SetStringValue(uint dwFieldID, string psz)
        {
            log.Debug($"dwFieldID: {dwFieldID}; psz: {psz}");

            view.SetValue((int)dwFieldID, psz);

            return HRESULT.S_OK;
        }

        public virtual int SetCheckboxValue(uint dwFieldID, int bChecked)
        {
            log.Debug($"dwFieldID: {dwFieldID}; bChecked: {bChecked}");

            return HRESULT.E_NOTIMPL;
        }

        public virtual int SetComboBoxSelectedValue(uint dwFieldID, uint dwSelectedItem)
        {
            log.Debug($"dwFieldID: {dwFieldID}; dwSelectedItem: {dwSelectedItem}");

            return HRESULT.E_NOTIMPL;
        }

        public virtual int CommandLinkClicked(uint dwFieldID)
        {
            log.Debug($"dwFieldID: {dwFieldID}");

            return HRESULT.E_NOTIMPL;
        }

        public virtual int GetSerialization(
            out _CREDENTIAL_PROVIDER_GET_SERIALIZATION_RESPONSE pcpgsr,
            out _CREDENTIAL_PROVIDER_CREDENTIAL_SERIALIZATION pcpcs,
            out string ppszOptionalStatusText,
            out _CREDENTIAL_PROVIDER_STATUS_ICON pcpsiOptionalStatusIcon
        )
        {
            log.Debug("GetSerialization");

            pcpgsr = _CREDENTIAL_PROVIDER_GET_SERIALIZATION_RESPONSE.CPGSR_RETURN_CREDENTIAL_FINISHED;
            pcpcs = new _CREDENTIAL_PROVIDER_CREDENTIAL_SERIALIZATION();
            ppszOptionalStatusText = string.Empty;
            pcpsiOptionalStatusIcon = _CREDENTIAL_PROVIDER_STATUS_ICON.CPSI_NONE;

            dynamic result = OS2Faktor.InitializeResetPasswordFlow();
            if (result != null) {
                DialogResult dialogResult = result.DialogResult;
                if (dialogResult.Equals(DialogResult.OK))
                {
                    return PackCredentials(result.Username, result.Password, ref pcpgsr, ref pcpcs, ref ppszOptionalStatusText, ref pcpsiOptionalStatusIcon);
                }
                else if(dialogResult.Equals(DialogResult.No))
                {
                    //Password reset not allowed
                    pcpgsr = _CREDENTIAL_PROVIDER_GET_SERIALIZATION_RESPONSE.CPGSR_NO_CREDENTIAL_NOT_FINISHED;
                    pcpcs = new _CREDENTIAL_PROVIDER_CREDENTIAL_SERIALIZATION();
                    ppszOptionalStatusText = "Dit kodeord kunne ikke ændres.";
                    pcpsiOptionalStatusIcon = _CREDENTIAL_PROVIDER_STATUS_ICON.CPSI_WARNING;
                    return HRESULT.S_OK;
                }
                else if (dialogResult.Equals(DialogResult.Cancel))
                {
                    //Password reset not allowed
                    pcpgsr = _CREDENTIAL_PROVIDER_GET_SERIALIZATION_RESPONSE.CPGSR_NO_CREDENTIAL_NOT_FINISHED;
                    pcpcs = new _CREDENTIAL_PROVIDER_CREDENTIAL_SERIALIZATION();
                    ppszOptionalStatusText = "Kodeordskifte afbrudt.";
                    pcpsiOptionalStatusIcon = _CREDENTIAL_PROVIDER_STATUS_ICON.CPSI_WARNING;
                    return HRESULT.S_OK;
                }
            }
            else
            {
                // error while reseting password
                pcpgsr = _CREDENTIAL_PROVIDER_GET_SERIALIZATION_RESPONSE.CPGSR_NO_CREDENTIAL_NOT_FINISHED;
                pcpcs = new _CREDENTIAL_PROVIDER_CREDENTIAL_SERIALIZATION();
                ppszOptionalStatusText = "Teknisk fejl.";
                pcpsiOptionalStatusIcon = _CREDENTIAL_PROVIDER_STATUS_ICON.CPSI_ERROR;
                return HRESULT.S_OK;
            }

            pcpgsr = _CREDENTIAL_PROVIDER_GET_SERIALIZATION_RESPONSE.CPGSR_NO_CREDENTIAL_NOT_FINISHED;
            pcpcs = new _CREDENTIAL_PROVIDER_CREDENTIAL_SERIALIZATION();
            ppszOptionalStatusText = string.Empty;
            pcpsiOptionalStatusIcon = _CREDENTIAL_PROVIDER_STATUS_ICON.CPSI_NONE;
            return HRESULT.E_NOTIMPL;
        }

        private int PackCredentials(string username, string password, ref _CREDENTIAL_PROVIDER_GET_SERIALIZATION_RESPONSE pcpgsr, ref _CREDENTIAL_PROVIDER_CREDENTIAL_SERIALIZATION pcpcs, ref string ppszOptionalStatusText, ref _CREDENTIAL_PROVIDER_STATUS_ICON pcpsiOptionalStatusIcon)
        {
            try
            {
                pcpgsr = _CREDENTIAL_PROVIDER_GET_SERIALIZATION_RESPONSE.CPGSR_RETURN_CREDENTIAL_FINISHED;
                pcpcs = new _CREDENTIAL_PROVIDER_CREDENTIAL_SERIALIZATION();

                var inCredSize = 0;
                var inCredBuffer = Marshal.AllocCoTaskMem(0);

                if (!PInvoke.CredPackAuthenticationBuffer(0, username, password, inCredBuffer, ref inCredSize))
                {
                    Marshal.FreeCoTaskMem(inCredBuffer);
                    inCredBuffer = Marshal.AllocCoTaskMem(inCredSize);

                    if (PInvoke.CredPackAuthenticationBuffer(0, username, password, inCredBuffer, ref inCredSize))
                    {
                        ppszOptionalStatusText = string.Empty;
                        pcpsiOptionalStatusIcon = _CREDENTIAL_PROVIDER_STATUS_ICON.CPSI_SUCCESS;

                        pcpcs.clsidCredentialProvider = Guid.Parse(Constants.PasswordResetCredentialProviderUID);
                        pcpcs.rgbSerialization = inCredBuffer;
                        pcpcs.cbSerialization = (uint)inCredSize;

                        RetrieveNegotiateAuthPackage(out var authPackage);
                        pcpcs.ulAuthenticationPackage = 0;//authPackage;
                        log.Debug("AuthPackage: " + authPackage);

                        return HRESULT.S_OK;
                    }

                    ppszOptionalStatusText = "Failed to pack credentials";
                    pcpsiOptionalStatusIcon = _CREDENTIAL_PROVIDER_STATUS_ICON.CPSI_ERROR;
                    return HRESULT.S_OK;
                }
            }
            catch (Exception ex)
            {
                // In case of any error, do not bring down winlogon
                log.Error(ex.Message, ex);
            }

            pcpgsr = _CREDENTIAL_PROVIDER_GET_SERIALIZATION_RESPONSE.CPGSR_NO_CREDENTIAL_NOT_FINISHED;
            pcpcs = new _CREDENTIAL_PROVIDER_CREDENTIAL_SERIALIZATION();
            ppszOptionalStatusText = string.Empty;
            pcpsiOptionalStatusIcon = _CREDENTIAL_PROVIDER_STATUS_ICON.CPSI_NONE;
            return HRESULT.E_NOTIMPL;
        }

        private int RetrieveNegotiateAuthPackage(out uint authPackage)
        {
            // TODO: better checking on the return codes

            var status = PInvoke.LsaConnectUntrusted(out var lsaHandle);

            using (var name = new PInvoke.LsaStringWrapper("Negotiate"))
            {
                status = PInvoke.LsaLookupAuthenticationPackage(lsaHandle, ref name._string, out authPackage);
            }

            PInvoke.LsaDeregisterLogonProcess(lsaHandle);

            return (int)status;
        }

        //This gets called after GetSerialization
        public virtual int ReportResult(int ntsStatus, int ntsSubstatus, out string ppszOptionalStatusText, out _CREDENTIAL_PROVIDER_STATUS_ICON pcpsiOptionalStatusIcon)
        {
            log.Debug($"ntsStatus: {ntsStatus}; ntsSubstatus: {ntsSubstatus}");

            ppszOptionalStatusText = "";
            pcpsiOptionalStatusIcon = _CREDENTIAL_PROVIDER_STATUS_ICON.CPSI_NONE;

            return HRESULT.S_OK;
        }
    }
}