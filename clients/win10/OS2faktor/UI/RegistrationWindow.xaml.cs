using System;
using System.Windows;
using System.Windows.Threading;

namespace OS2faktor
{
    /// <summary>
    /// Interaction logic for RegistrationWindow.xaml
    /// </summary>
    public partial class RegistrationWindow : Window
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private bool navigating = false;

        public RegistrationWindow()
        {
            InitializeComponent();
        }

        private void webview_Loaded(object sender, RoutedEventArgs e)
        {
            webview.Source = new Uri(Properties.Settings.Default.backendUrl + "/ui/register2?type=" + Uri.EscapeDataString("WINDOWS"));
        }

        private void webview_Navigating(object sender, System.Windows.Navigation.NavigatingCancelEventArgs e)
        {
            var queryDictionary = System.Web.HttpUtility.ParseQueryString(e.Uri.Query);

            string closeWindow = queryDictionary["closeWindow"];
            if (closeWindow != null)
            {
                this.Close();
            }

            string apiKey = queryDictionary["apiKey"];
            string deviceId = queryDictionary["deviceId"];

            bool status = false;
            bool.TryParse(queryDictionary["status"], out status);

            if ((apiKey == null || deviceId == null) && status == false)
            {
                return;
            }

            if (status == true)
            {
                Properties.Settings.Default.IsNemIDRegistered = status;
            }

            if (apiKey != null && deviceId != null)
            {
                Properties.Settings.Default.apiKey = apiKey;
                Properties.Settings.Default.deviceId = deviceId;
            }

            Properties.Settings.Default.Save();
            Properties.Settings.Default.Reload();

            ((App)App.Current).UpdateContextMenuVisibility();

            if (!navigating)
            {
                navigating = true;
                string urlString = Properties.Settings.Default.backendUrl
                  + "/ui/register2/nemid?"
                  + "&apiKey=" + Uri.EscapeDataString(Properties.Settings.Default.apiKey)
                  + "&deviceId=" + Uri.EscapeDataString(Properties.Settings.Default.deviceId);

                webview.Navigate(new Uri(urlString), null, null, null);
            }
        }
    }
}
