using System.Reflection;
using log4net;
using log4net.Appender;
using log4net.Layout;

namespace OS2faktorPlugin
{
    class Initializer
    {
        private static bool initialized = false;

        public static void init(bool debug)
        {
            if (!initialized)
            {
                PatternLayout patternLayout = new PatternLayout();
                patternLayout.ConversionPattern = "%date - %-5level %logger - %message%newline";
                patternLayout.ActivateOptions();

                RollingFileAppender appender = new RollingFileAppender();
                appender.Layout = patternLayout;
                appender.AppendToFile = true;
                appender.File = @"c:\logs\os2faktor\adfs.log";
                appender.MaxFileSize = (10 * 1024 * 1024);
                appender.MaxSizeRollBackups = 5;
                appender.RollingStyle = RollingFileAppender.RollingMode.Composite;
                appender.ActivateOptions();

                var logRepository = (log4net.Repository.Hierarchy.Hierarchy)LogManager.GetRepository(Assembly.GetEntryAssembly());
                logRepository.Root.AddAppender(appender);

                if (debug)
                {
                    logRepository.Root.Level = log4net.Core.Level.Debug;
                }
                else
                {
                    logRepository.Root.Level = log4net.Core.Level.Info;
                }

                logRepository.Configured = true;

                initialized = true;
            }
        }
    }
}
