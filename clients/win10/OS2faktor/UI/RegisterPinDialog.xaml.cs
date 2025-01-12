using OS2faktor.Utils;
using System;
using System.ComponentModel;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Input;

namespace OS2faktor
{
    /// <summary>
    /// Interaction logic for RegisterPinDialog.xaml
    /// </summary>
    public partial class RegisterPinDialog : Window
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        public RegisterPinDialog()
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
                + "/ui/pin/register?"
                + "&apiKey=" + Uri.EscapeDataString(EncryptionUtil.GetDecryptedApiKey(Properties.Settings.Default.apiKey))
                + "&deviceId=" + Uri.EscapeDataString(Properties.Settings.Default.deviceId);

            webview.Source = new Uri(urlString);
        }

        private void webview_Navigating(object sender, System.Windows.Navigation.NavigatingCancelEventArgs e)
        {
            var queryDictionary = System.Web.HttpUtility.ParseQueryString(e.Uri.Query);

            bool status = false;
            if (bool.TryParse(queryDictionary["status"], out status))
            {
                Properties.Settings.Default.IsPinRegistered = status;
                Properties.Settings.Default.Save();
                Properties.Settings.Default.Reload();
            }

            string closeWindow = queryDictionary["closeWindow"];
            if (closeWindow != null)
            {
                this.Close();
            }

            ((App)App.Current).UpdateContextMenuVisibility();
        }
    }
}
