using OS2faktor.Service;
using OS2faktor.Utils;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using System.Windows.Threading;

namespace OS2faktor.UI.Registration
{
    /// <summary>
    /// Interaction logic for PinCode.xaml
    /// </summary>
    public partial class PinCode : Page
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        public PinCode()
        {
            InitializeComponent();
        }

        private void btnSave_Click(object sender, RoutedEventArgs e)
        {
            Application.Current.Dispatcher.Invoke(new Action(() => { lblInvalidPin.Visibility = Visibility.Collapsed; lblError.Visibility = Visibility.Collapsed; }));
            //Get the parent window
            RegistrationWindow wnd = (RegistrationWindow)Window.GetWindow(this);
            var deviceName = wnd.DeviceName;

            var backendService = new BackendService();
            backendService.Register(deviceName, tbPinCode.Password).ContinueWith((finishedTask) =>
            {
                var result = finishedTask.Result;
                if (result != null)
                {
                    if (result.Success)
                    {
                        Properties.Settings.Default.deviceId = result.DeviceId;
                        Properties.Settings.Default.apiKey = EncryptionUtil.GetEncryptedAndEncodedApiKey(result.ApiKey);
                        Properties.Settings.Default.IsPinRegistered = true;
                        Properties.Settings.Default.Save();
                        Properties.Settings.Default.Reload();

                        ((App)App.Current).UpdateContextMenuVisibility();
                        ((App)App.Current).Dispatcher.Invoke(DispatcherPriority.Normal, new Action(() =>
                        {
                            var registrationNemID = new RegistrationNemIDWindow();
                            registrationNemID.Show();
                        }));

                        Application.Current.Dispatcher.Invoke(new Action(() => { wnd.Close(); }));
                    }
                    else if (result.InvalidPin)
                    {
                        Application.Current.Dispatcher.Invoke(new Action(() => { lblInvalidPin.Visibility = Visibility.Visible; }));
                    }
                    else
                    {
                        log.Warn($"Error occured while trying to connect to Backend");
                        Application.Current.Dispatcher.Invoke(new Action(() => { lblError.Visibility = Visibility.Visible; }));
                    }
                }
            });
        }

        private void tbPinCode_PreviewTextInput(object sender, TextCompositionEventArgs e)
        {
            Regex regex = new Regex("[^0-9]+");
            e.Handled = regex.IsMatch(e.Text);
        }

        private void tbPinCode_PreviewKeyDown(object sender, KeyEventArgs e)
        {
            //There is some weird bug and PasswordBox allows spacebar so this is a fix for it.
            e.Handled = e.Key == Key.Space;
        }
    }
}
