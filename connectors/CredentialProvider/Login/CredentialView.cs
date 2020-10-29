using CredProvider.NET.Interop;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;

namespace Login
{
    public class CredentialDescriptor
    {
        public _CREDENTIAL_PROVIDER_FIELD_DESCRIPTOR Descriptor { get; set; }
        public _CREDENTIAL_PROVIDER_FIELD_STATE State { get; set; }
        public object Value { get; set; }
    }

    public class CredentialView
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private readonly List<CredentialDescriptor> fields = new List<CredentialDescriptor>();
        private readonly Dictionary<int, ICredentialProviderCredential> credentials = new Dictionary<int, ICredentialProviderCredential>();
        public bool Active { get; set; }
        public int DescriptorCount { get { return fields.Count; } }
        public virtual int CredentialCount { get { return 1; } }
        public virtual int DefaultCredential { get { return 0; } }
        public readonly static CredentialView NotActive = new CredentialView { Active = false };

        public CredentialView() { }

        public virtual void AddField(_CREDENTIAL_PROVIDER_FIELD_TYPE cpft, string pszLabel, _CREDENTIAL_PROVIDER_FIELD_STATE state, string defaultValue = null, Guid guidFieldType = default(Guid))
        {
            if (!Active)
            {
                throw new NotSupportedException();
            }

            fields.Add(new CredentialDescriptor
            {
                State = state,
                Value = defaultValue,
                Descriptor = new _CREDENTIAL_PROVIDER_FIELD_DESCRIPTOR
                {
                    dwFieldID = (uint)fields.Count,
                    cpft = cpft,
                    pszLabel = pszLabel,
                    guidFieldType = guidFieldType
                }
            });
        }

        public virtual bool GetField(int dwIndex, [Out] IntPtr ppcpfd)
        {
            log.Debug($"dwIndex: {dwIndex}; descriptors: {fields.Count}");

            if (dwIndex >= fields.Count)
            {
                return false;
            }

            var field = fields[dwIndex];

            var pcpfd = Marshal.AllocHGlobal(Marshal.SizeOf(field.Descriptor));

            Marshal.StructureToPtr(field.Descriptor, pcpfd, false);
            Marshal.StructureToPtr(pcpfd, ppcpfd, false);

            return true;
        }

        public string GetValue(int dwFieldId)
        {
            return (string)fields[dwFieldId].Value;
        }

        public void SetValue(int dwFieldId, string val)
        {
            fields[dwFieldId].Value = val;
        }

        public void GetFieldState(
            int dwFieldId,
            out _CREDENTIAL_PROVIDER_FIELD_STATE pcpfs,
            out _CREDENTIAL_PROVIDER_FIELD_INTERACTIVE_STATE pcpfis
        )
        {
            var field = fields[dwFieldId];

            pcpfs = field.State;
            pcpfis = _CREDENTIAL_PROVIDER_FIELD_INTERACTIVE_STATE.CPFIS_NONE;
        }

        public string GetUsername()
        {
            var username = fields.Where(f => f.Descriptor.cpft.Equals(_CREDENTIAL_PROVIDER_FIELD_TYPE.CPFT_EDIT_TEXT) && f.Descriptor.pszLabel.Equals("Brugernavn")).FirstOrDefault();
            return (string)username?.Value ?? "";
        }

        public string GetPassword()
        {
            var password = fields.Where(f => f.Descriptor.cpft.Equals(_CREDENTIAL_PROVIDER_FIELD_TYPE.CPFT_PASSWORD_TEXT) && f.Descriptor.pszLabel.Equals("Kodeord")).FirstOrDefault();
            return (string)password?.Value ?? "";
        }

        public IntPtr GetBitmap(uint dwFieldID)
        {
            var field = fields[(int)dwFieldID];
            string assemblyFolder = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);
            try
            {
                Bitmap tileIcon = new Bitmap(assemblyFolder + "\\Images\\logo192.png");
                return tileIcon.GetHbitmap();
            }
            catch (Exception ex)
            {
                log.Error("Error occured while loading image.", ex);
            }

            return IntPtr.Zero;
        }

        public virtual ICredentialProviderCredential CreateCredential(int dwIndex)
        {
            if (credentials.TryGetValue(dwIndex, out ICredentialProviderCredential credential))
            {
                return credential;
            }

            credential = new CredentialProviderCredential(this);

            credentials[dwIndex] = credential;

            return credential;
        }

    }
}
