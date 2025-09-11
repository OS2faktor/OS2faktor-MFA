using Microsoft.Web.WebView2.Core;
using Microsoft.Win32;
using OS2faktor.Utils;
using System;
using System.IO;
using System.Windows;

namespace OS2faktor
{
    /// <summary>
    /// Interaction logic for SelfServiceWindow.xaml
    /// </summary>
    public partial class SelfServiceWindow : Window
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        public SelfServiceWindow()
        {
            InitializeComponent();
        }

        private async void webview_Loaded(object sender, RoutedEventArgs e)
        {
            string urlString = Properties.Settings.Default.frontendUrl
                + "/ui/selfservice?"
                + "&apiKey=" + Uri.EscapeDataString(EncryptionUtil.GetDecryptedApiKey(Properties.Settings.Default.apiKey))
                + "&deviceId=" + Uri.EscapeDataString(Properties.Settings.Default.deviceId);
            
            try
            {
                var regKey = Registry.LocalMachine.OpenSubKey("SOFTWARE\\OS2\\OS2faktor");
                var regKeyValue = (string)regKey.GetValue("Path");
                if (regKeyValue == null)
                {
                    regKeyValue = "c:\\program files (x86)\\OS2\\OS2faktor";
                }

                // TODO: kan et issue være skriverettigheder til tempfolderen?
                var udf = Path.Combine(Path.GetTempPath(), "OS2faktor");

                log.Info("Using temporary folder for profile: " + udf);

                var env = await CoreWebView2Environment.CreateAsync(null, udf);
                await this.webview.EnsureCoreWebView2Async(env);
                /*
                this.webview.CreationProperties = new Microsoft.Web.WebView2.Wpf.CoreWebView2CreationProperties
                {
                    BrowserExecutableFolder = bef,
                    UserDataFolder = udf
                };
                */
            }
            catch (Exception ex)
            {
                // sometimes windows has already initialized the webview. We don't control the version in that
                // case, but it is better to allow the window to open, than fail outright
                log.Warn("Failed to initialize webview (" + ex.GetType().ToString() + "): " + ex.Message, ex);
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
        }
    }
}
