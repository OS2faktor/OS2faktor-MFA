using OS2faktor.UI.Registration;
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

        public string DeviceName { get; set; }

        public RegistrationWindow()
        {
            InitializeComponent();
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            this.Content = new DeviceName();
        }

    }
}
