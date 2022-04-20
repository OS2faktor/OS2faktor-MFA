using System;
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
