using OS2faktor.Utils;
using System;
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

        private void webview_Loaded(object sender, RoutedEventArgs e)
        {
            string urlString = Properties.Settings.Default.frontendUrl
                + "/ui/selfservice?"
                + "&apiKey=" + Uri.EscapeDataString(EncryptionUtil.GetDecryptedApiKey(Properties.Settings.Default.apiKey))
                + "&deviceId=" + Uri.EscapeDataString(Properties.Settings.Default.deviceId);

            webview.Source = new Uri(urlString);
        }

        private void webview_Navigating(object sender, System.Windows.Navigation.NavigatingCancelEventArgs e)
        {
            var queryDictionary = System.Web.HttpUtility.ParseQueryString(e.Uri.Query);

            string closeWindow = queryDictionary["closeWindow"];
            if (closeWindow != null)
            {
                this.Close();
            }
        }
    }
}
