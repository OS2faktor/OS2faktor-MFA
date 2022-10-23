using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.Wpf;
using Microsoft.Win32;
using System;
using System.ComponentModel;
using System.IO;
using System.Threading;
using System.Windows;

namespace OS2faktor
{
    /// <summary>
    /// Interaction logic for RegistrationWindow.xaml
    /// </summary>
    public partial class RegistrationNemIDWindow : Window
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        public RegistrationNemIDWindow()
        {
            InitializeComponent();
        }

        private void webview_Loaded(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrEmpty(Properties.Settings.Default.apiKey) || string.IsNullOrEmpty(Properties.Settings.Default.deviceId))
            {
                log.Error("Both ApiKey and DeviceID cannot be null or empty");
            }

            string urlString = Properties.Settings.Default.frontendUrl
                + "/ui/register2/nemid?"
                + "&apiKey=" + Uri.EscapeDataString(Properties.Settings.Default.apiKey)
                + "&deviceId=" + Uri.EscapeDataString(Properties.Settings.Default.deviceId);

            try
            {
                var regKey = Registry.LocalMachine.OpenSubKey("SOFTWARE\\OS2\\OS2faktor");
                var regKeyValue = (string)regKey.GetValue("Path");
                if (regKeyValue == null)
                {
                    regKeyValue = "c:\\program files (x86)\\OS2\\OS2faktor";
                }

                var bef = Path.Combine(regKeyValue, "Microsoft.WebView2.FixedVersionRuntime.100.0.1185.50.x64");
                var udf = Path.Combine(Path.GetTempPath(), "OS2faktor");

                log.Info("Looking for webview in folder: " + bef);
                log.Info("Using temporary folder for profile: " + udf);

                this.webview.CreationProperties = new Microsoft.Web.WebView2.Wpf.CoreWebView2CreationProperties
                {
                    BrowserExecutableFolder = bef,
                    UserDataFolder = udf
                };
            }
            catch (Exception ex)
            {
                // sometimes windows has already initialized the webview. We don't control the version in that
                // case, but it is better to allow the window to open, than fail outright
                log.Warn("Failed to initialize webview (" + ex.GetType().ToString() + "): " + ex.Message);
            }

            webview.Source = new Uri(urlString);
        }

        private void webview_NavigationStarting(object sender, Microsoft.Web.WebView2.Core.CoreWebView2NavigationStartingEventArgs e)
        {
            var queryDictionary = System.Web.HttpUtility.ParseQueryString(new Uri(e.Uri).Query);

            string closeWindow = queryDictionary["closeWindow"];
            if (closeWindow != null)
            {
                this.Close();
            }

            bool status = false;
            if (!bool.TryParse(queryDictionary["status"], out status))
            {
                return;
            }

            Properties.Settings.Default.IsNemIDRegistered = status;
            Properties.Settings.Default.Save();
            Properties.Settings.Default.Reload();

            ((App)App.Current).UpdateContextMenuVisibility();
        }
    }
}
