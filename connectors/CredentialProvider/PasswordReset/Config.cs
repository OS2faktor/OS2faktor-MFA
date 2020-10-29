using Microsoft.Win32;

using System.Collections.Generic;
using System.Linq;

namespace PasswordReset
{
    class Config
    {
        private const string REGISTRY_LOCATION = @"SOFTWARE\Digital Identity\OS2faktorCP";

        public static List<string> GetAllowedUrls()
        {
            var key = Registry.LocalMachine.OpenSubKey(REGISTRY_LOCATION);

            var allowedUrls = (string)key.GetValue("AllowedUrls");

            return new List<string>(allowedUrls.Split(";".ToArray()));
        }

        public static string GetProxyUrl()
        {
            var key = Registry.LocalMachine.OpenSubKey(REGISTRY_LOCATION);

            string proxyUrl = (string)key.GetValue("ProxyUrl");
            if (proxyUrl.EndsWith("/"))
            {
                return proxyUrl.Substring(0, proxyUrl.Length - 1);
            }

            return proxyUrl;
        }

        public static log4net.Core.Level GetLogLevel()
        {
            var key = Registry.LocalMachine.OpenSubKey(REGISTRY_LOCATION);

            var debug = (string)key.GetValue("Debug");
            if ("true".Equals(debug))
            {
                return log4net.Core.Level.Debug;
            }

            return log4net.Core.Level.Info;
        }
    }
}
