using Microsoft.IdentityServer.Web.Authentication.External;
using System.Collections.Generic;
using System.Globalization;

namespace OS2faktorPlugin
{
    class OS2faktorMetadata : IAuthenticationAdapterMetadata
    {
        private const string PRODUCT_NAME = "OS2faktor";
        public const string AUTH_METHOD = "http://os2faktor.dk/mfa";

        // languages are always mapped to danish. There is no i18n support in this code, but in AD FS 4.0 login will
        // fail if a user requests a language/culture that is not supported, so we fake support for the most common ones

        public string AdminName
        {
            get { return PRODUCT_NAME; }
        }

        public virtual string[] AuthenticationMethods
        {
            get { return new[] { AUTH_METHOD }; }
        }

        public int[] AvailableLcids
        {
            get
            {
                return new[]
                {
                    new CultureInfo("da-DK").LCID,
                    new CultureInfo("en-US").LCID,
                    new CultureInfo("en-GB").LCID,
                    new CultureInfo("sv-SE").LCID,
                    new CultureInfo("nb-NO").LCID,
                };
            }
        }

        public Dictionary<int, string> FriendlyNames
        {
            get
            {
                Dictionary<int, string> _friendlyNames = new Dictionary<int, string>();
                _friendlyNames.Add(new CultureInfo("da-DK").LCID, "OS2faktor");
                _friendlyNames.Add(new CultureInfo("en-US").LCID, "OS2faktor");
                _friendlyNames.Add(new CultureInfo("en-GB").LCID, "OS2faktor");
                _friendlyNames.Add(new CultureInfo("sv-SE").LCID, "OS2faktor");
                _friendlyNames.Add(new CultureInfo("nb-NO").LCID, "OS2faktor");

                return _friendlyNames;
            }
        }

        public Dictionary<int, string> Descriptions
        {
            get
            {
                Dictionary<int, string> _descriptions = new Dictionary<int, string>();
                _descriptions.Add(new CultureInfo("da-DK").LCID, "Den kommunejede 2-faktor login løsning");
                _descriptions.Add(new CultureInfo("en-US").LCID, "Den kommunejede 2-faktor login løsning");
                _descriptions.Add(new CultureInfo("en-GB").LCID, "Den kommunejede 2-faktor login løsning");
                _descriptions.Add(new CultureInfo("sv-SE").LCID, "Den kommunejede 2-faktor login løsning");
                _descriptions.Add(new CultureInfo("nb-NO").LCID, "Den kommunejede 2-faktor login løsning");

                return _descriptions;
            }
        }

        /// Returns an array indicating the type of claim that that the adapter uses to identify the user being authenticated.
        /// Note that although the property is an array, only the first element is currently used.
        /// MUST BE ONE OF THE FOLLOWING
        /// "http://schemas.microsoft.com/ws/2008/06/identity/claims/windowsaccountname"
        /// "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/upn"
        /// "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"
        /// "http://schemas.microsoft.com/ws/2008/06/identity/claims/primarysid"
        public string[] IdentityClaims
        {
//            get { return new[] { "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/upn" }; }
            get { return new[] { "http://schemas.microsoft.com/ws/2008/06/identity/claims/windowsaccountname" }; }
        }

        public bool RequiresIdentity
        {
            get { return true; }
        }
    }
}
