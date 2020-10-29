using log4net;
using log4net.Appender;
using log4net.Layout;
using System;
using System.Diagnostics;
using System.IO;
using System.Management.Automation;
using System.Threading;

namespace VPNLauncher
{
    class Program
    {
        private static readonly ILog log = LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        static void Main(string[] args)
        {
            if (Config.GetRunMode().Equals(Config.RUN_MODE.AFTER_LOGIN))
            {
                InitLog();

                LogDebug("Attempting to launch VPN connection");

                try
                {
                    string tempFolder = Config.GetTempFolder();
                    if (!tempFolder.EndsWith("\\"))
                    {
                        tempFolder += "\\";
                    }

                    string uid = Environment.UserName;
                    string file = tempFolder + uid + ".ps1";

                    LogDebug("Looking for: " + file);

                    if (File.Exists(file))
                    {
                        LogDebug("Found launch file - attempting to run");

                        string content = File.ReadAllText(file);

                        using (PowerShell script = PowerShell.Create())
                        {
                            script.AddScript(content);

                            // begin invoke execution on the pipeline
                            IAsyncResult result = script.BeginInvoke();

                            // do something else until execution has completed.
                            // this could be sleep/wait, or perhaps some other work
                            int tries = 0;
                            while (result.IsCompleted == false)
                            {
                                Thread.Sleep(1000);
                                tries++;

                                // two minutes only
                                if (tries > 120)
                                {
                                    break;
                                }
                            }
                        }

                        // cleanup
                        File.Delete(file);

                        LogDebug("File executed successfully");
                    }
                    else
                    {
                        LogDebug("No launch file found");
                    }

                }
                catch (Exception ex)
                {
                    LogError("Failed to launch VPN client: " + ex.Message, ex);
                }
            }
            else
            {
                LogDebug("Will not attempt to launch VPN connection: " + Config.GetRunMode().ToString());
            }
        }

        private static void InitLog()
        {
            try
            {
                PatternLayout patternLayout = new PatternLayout();
                patternLayout.ConversionPattern = "%date - %-5level %logger - %message%newline";
                patternLayout.ActivateOptions();

                RollingFileAppender appender = new RollingFileAppender();
                appender.Layout = patternLayout;
                appender.File = "c:/logs/os2faktor/vpn.log";
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
                Console.WriteLine("Failed to initialize log4net: " + ex.Message);
            }
        }

        private static void LogDebug(string message)
        {
            Console.WriteLine("DEBUG: " + message);
            log.Debug(message);
        }

        private static void LogError(string message, Exception ex)
        {
            Console.WriteLine("ERROR: " + message);
            log.Error(message, ex);
        }
    }
}
