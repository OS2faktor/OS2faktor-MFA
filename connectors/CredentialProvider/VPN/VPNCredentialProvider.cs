﻿using CredProvider.NET.Interop;
using System;
using System.Runtime.InteropServices;

namespace VPN
{
    [ComVisible(true)]
    [Guid("62b8f223-6e50-453f-af28-4bab3c7a7002")]
    [ClassInterface(ClassInterfaceType.None)]
    [ProgId("OS2faktorPasswordResetCredentialProvider")]
    public class VPNCredentialProvider : CredentialProviderBase
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private readonly CredentialView view = new CredentialView();

        protected override CredentialView Initialize(_CREDENTIAL_PROVIDER_USAGE_SCENARIO cpus, uint dwFlags)
        {
            var flags = (CredentialFlag)dwFlags;

            log.Debug($"cpus: {cpus}; dwFlags: {flags}");

            var isSupported = IsSupportedScenario(cpus);

            if (!isSupported)
            {
                return CredentialView.NotActive;
            }

            var view = new CredentialView { Active = true };

            view.AddField(
                cpft: _CREDENTIAL_PROVIDER_FIELD_TYPE.CPFT_TILE_IMAGE,
                pszLabel: "Icon",
                state: _CREDENTIAL_PROVIDER_FIELD_STATE.CPFS_DISPLAY_IN_BOTH
            );

            view.AddField(
                cpft: _CREDENTIAL_PROVIDER_FIELD_TYPE.CPFT_EDIT_TEXT,
                pszLabel: "Brugernavn",
                state: _CREDENTIAL_PROVIDER_FIELD_STATE.CPFS_DISPLAY_IN_SELECTED_TILE
            );

            view.AddField(
                cpft: _CREDENTIAL_PROVIDER_FIELD_TYPE.CPFT_PASSWORD_TEXT,
                pszLabel: "Kodeord",
                state: _CREDENTIAL_PROVIDER_FIELD_STATE.CPFS_DISPLAY_IN_SELECTED_TILE
            );

            view.AddField(
                cpft: _CREDENTIAL_PROVIDER_FIELD_TYPE.CPFT_SUBMIT_BUTTON,
                pszLabel: "Login med VPN",
                defaultValue: "Login med VPN",
                state: _CREDENTIAL_PROVIDER_FIELD_STATE.CPFS_DISPLAY_IN_DESELECTED_TILE
            );

            view.AddField(
                cpft: _CREDENTIAL_PROVIDER_FIELD_TYPE.CPFT_LARGE_TEXT,
                pszLabel: "Login med VPN",
                defaultValue: "Login med VPN",
                state: _CREDENTIAL_PROVIDER_FIELD_STATE.CPFS_DISPLAY_IN_DESELECTED_TILE
            );

            return view;
        }

        private static bool IsSupportedScenario(_CREDENTIAL_PROVIDER_USAGE_SCENARIO cpus)
        {
            log.Debug("IsSupportedScenario: " + cpus);

            switch (cpus)
            {
                case _CREDENTIAL_PROVIDER_USAGE_SCENARIO.CPUS_UNLOCK_WORKSTATION:
                case _CREDENTIAL_PROVIDER_USAGE_SCENARIO.CPUS_LOGON:
                    return true;
                case _CREDENTIAL_PROVIDER_USAGE_SCENARIO.CPUS_CREDUI:
                case _CREDENTIAL_PROVIDER_USAGE_SCENARIO.CPUS_CHANGE_PASSWORD:
                case _CREDENTIAL_PROVIDER_USAGE_SCENARIO.CPUS_PLAP:
                case _CREDENTIAL_PROVIDER_USAGE_SCENARIO.CPUS_INVALID:
                default:
                    return false;
            }
        }
    }
}
