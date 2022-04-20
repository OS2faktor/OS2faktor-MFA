using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
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

namespace OS2faktor.UI.Registration
{
    /// <summary>
    /// Interaction logic for DeviceName.xaml
    /// </summary>
    public partial class DeviceName : Page
    {
        public DeviceName()
        {
            InitializeComponent();
        }

        private void btnSave_Click(object sender, RoutedEventArgs e)
        {
            if (tbDeviceName.Text.Length < 3)
            {
                lblError.Visibility = Visibility.Visible;
            } else
            {
                //Get the parent window
                RegistrationWindow wnd = (RegistrationWindow)Window.GetWindow(this);
                wnd.Content = new PinCode();
                wnd.DeviceName = tbDeviceName.Text;
            }
        }

        private void tbDeviceName_KeyUp(object sender, System.Windows.Input.KeyEventArgs e)
        {
            if (e.Key == Key.Enter)
            {
                btnSave_Click(sender, e);
            }
            if (tbDeviceName.Text.Length < 3 && tbDeviceName.Text.Length != 0)
            {
                lblError.Visibility = Visibility.Visible;
            }
            else
            {
                lblError.Visibility = Visibility.Collapsed;
            }
        }
    }
}
