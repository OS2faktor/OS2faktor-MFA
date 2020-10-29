﻿using CredProvider.NET.Interop;
using System;
using System.Runtime.InteropServices;
using static VPN.Constants;

namespace VPN
{
    public abstract class CredentialProviderBase : ICredentialProvider, ICredentialProviderSetUserArray
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private ICredentialProviderEvents events;
        protected abstract CredentialView Initialize(_CREDENTIAL_PROVIDER_USAGE_SCENARIO cpus, uint dwFlags);
        private CredentialView view;

        public virtual int SetUsageScenario(_CREDENTIAL_PROVIDER_USAGE_SCENARIO cpus, uint dwFlags)
        {
            view = Initialize(cpus, dwFlags);

            if (view.Active) 
            {
                return HRESULT.S_OK;
            }

            return HRESULT.E_NOTIMPL;
        }

        public virtual int SetSerialization(ref _CREDENTIAL_PROVIDER_CREDENTIAL_SERIALIZATION pcpcs)
        {
            log.Debug($"ulAuthenticationPackage: {pcpcs.ulAuthenticationPackage}");

            return HRESULT.S_OK;
        }

        public virtual int Advise(ICredentialProviderEvents pcpe, ulong upAdviseContext)
        {
            log.Debug($"upAdviseContext: {upAdviseContext}");

            if (pcpe != null)
            {
                events = pcpe;

                Marshal.AddRef(Marshal.GetIUnknownForObject(pcpe));
            }

            return HRESULT.S_OK;
        }

        public virtual int UnAdvise()
        {
            log.Debug("UnAdvise");

            if (events != null)
            {
                //Marshal.Release(Marshal.GetIUnknownForObject(events));
                events = null;
            }

            return HRESULT.S_OK;
        }

        public virtual int GetFieldDescriptorCount(out uint pdwCount)
        {
            pdwCount = (uint)view.DescriptorCount;

            return HRESULT.S_OK;
        }

        public virtual int GetFieldDescriptorAt(uint dwIndex, [Out] IntPtr ppcpfd)
        {
            if (view.GetField((int)dwIndex, ppcpfd))
            {
                return HRESULT.S_OK;
            }

            return HRESULT.E_INVALIDARG;
        }

        public virtual int GetCredentialCount(out uint pdwCount, out uint pdwDefault, out int pbAutoLogonWithDefault)
        {
            log.Debug("GetCredentialCount");

            pdwCount = (uint)view.CredentialCount;

            pdwDefault = (uint)view.DefaultCredential;

            pbAutoLogonWithDefault = 0;

            return HRESULT.S_OK;
        }

        public virtual int GetCredentialAt(uint dwIndex, out ICredentialProviderCredential ppcpc)
        {
            log.Debug($"dwIndex: {dwIndex}");

            ppcpc = view.CreateCredential((int)dwIndex);
            
            return HRESULT.S_OK;
        }

        public virtual int SetUserArray(ICredentialProviderUserArray users)
        {
            users.GetCount(out uint count);
            users.GetAccountOptions(out CREDENTIAL_PROVIDER_ACCOUNT_OPTIONS options);

            log.Debug($"count: {count}; options: {options}");

            for (uint i = 0; i < count; i++)
            {
                users.GetAt(i, out ICredentialProviderUser user);

                user.GetProviderID(out Guid providerId);
                user.GetSid(out string sid);

                log.Debug($"providerId: {providerId}; sid: {sid}");
            }

            return HRESULT.S_OK;
        }
    }
}
