using Microsoft.Win32;
using System;

namespace VPNLauncher
{
    class Config
    {
        public enum RUN_MODE { BEFORE_LOGIN, AFTER_LOGIN }
        private const string REGISTRY_LOCATION = @"SOFTWARE\Digital Identity\OS2faktorCP";

        public static RUN_MODE GetRunMode()
        {
            var key = Registry.LocalMachine.OpenSubKey(REGISTRY_LOCATION);
            if (key != null)
            {
                string value = (string)key.GetValue("RunMode");

                if (RUN_MODE.AFTER_LOGIN.ToString().Equals(value))
                {
                    return RUN_MODE.AFTER_LOGIN;
                }
            }

            return RUN_MODE.BEFORE_LOGIN;
        }

        public static string GetTempFolder()
        {
            var key = Registry.LocalMachine.OpenSubKey(REGISTRY_LOCATION);

            if (key != null)
            {
                return (string)key.GetValue("TempFolder");
            }

            return null;
        }

        public static log4net.Core.Level GetLogLevel()
        {
            var key = Registry.LocalMachine.OpenSubKey(REGISTRY_LOCATION);

            if (key != null)
            {
                var value = (string)key.GetValue("Debug");

                if ("true".Equals(value))
                {
                    return log4net.Core.Level.Debug;
                }
            }

            return log4net.Core.Level.Info;
        }
    }
}
