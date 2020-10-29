using OS2faktor.Utils;
using System;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows;
using System.Windows.Input;

namespace OS2faktor
{
    public partial class NotificationWindow : Window
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private string subscriptionKey;
        private string token;
        private string serverName;

        public NotificationWindow()
            : base()
        {
            this.InitializeComponent();
            this.Closed += this.NotificationClosed;
            pinBox.Visibility = Visibility.Hidden;
        }

        public new void Show()
        {
            if (OS2faktor.Properties.Settings.Default.IsPinRegistered)
            {
                grid.RowDefinitions[0].Height = new GridLength(1, GridUnitType.Star);
                grid.RowDefinitions[1].Height = new GridLength(1.9, GridUnitType.Star);
                grid.RowDefinitions[2].Height = new GridLength(1, GridUnitType.Star);
                pinBox.Visibility = Visibility.Visible;
                tbPassword.Focus();
            }

            if (string.IsNullOrEmpty(serverName))
            {
                throw new Exception("ServerName must be set before showing NotificationWindow");
            }
            if (string.IsNullOrEmpty(token))
            {
                throw new Exception("Token must be set before showing NotificationWindow");
            }

            this.Topmost = true;
            base.Show();

            //this.Owner = System.Windows.Application.Current.MainWindow;
            this.Closed += this.NotificationClosed;
            var workingArea = System.Windows.SystemParameters.WorkArea;

            this.Left = workingArea.Right - this.ActualWidth;
            double top = workingArea.Bottom - this.ActualHeight;

            if(workingArea.Bottom <= 1024)
            {
                this.Left = 0;
            }

            foreach (Window window in System.Windows.Application.Current.Windows)
            {
                string windowName = window.GetType().Name;

                //Prevent showing same notification twice
                if (windowName.Equals("NotificationWindow") && window != this ) {
                    var otherNotification = window as NotificationWindow;
                    if (otherNotification.serverName.Equals(this.serverName) && otherNotification.token.Equals(this.token))
                    {
                        this.Close();
                        return;
                    }
                }

                //Adjust position
                if (windowName.Equals("NotificationWindow") && window != this)
                {
                    window.Topmost = true;
                    top = window.Top - window.ActualHeight;
                }
            }

            this.Top = top;
        }
        
        private void NotificationClosed(object sender, EventArgs e)
        {
            foreach (Window window in System.Windows.Application.Current.Windows)
            {
                string windowName = window.GetType().Name;

                if (windowName.Equals("NotificationWindow") && window != this)
                {
                    // Adjust any windows that were above this one to drop down
                    if (window.Top < this.Top)
                    {
                        window.Top = window.Top + this.ActualHeight;
                    }
                }
            }
        }

        public void IncorrectPincode(WebSockets.PinResult pinResult)
        {
            if (pinResult != null)
            {
                switch (pinResult.status)
                {
                    case WebSockets.PinResultStatus.WRONG_PIN:
                        lblError.Content = "Forkert Pin!";
                        break;
                    case WebSockets.PinResultStatus.LOCKED:
                        lblError.Content = string.Format("Klient låst indtil: {0}", pinResult.lockedUntil);
                        break;
                }
                tbPassword.Password = "";
                tbPassword.Focus();
            }
        }

        private void btnApprove_Click(object sender, RoutedEventArgs e)
        {
            WSCommunication.Accept(subscriptionKey, tbPassword.Password);
            // simple prevention against brute-force
            Thread.Sleep(1000);
        }
        private void btnDecline_Click(object sender, RoutedEventArgs e)
        {
            WSCommunication.Reject(subscriptionKey);
            this.Close();
        }

        internal string getSubscriptionKey()
        {
            return this.subscriptionKey;
        }

        internal void setSubscriptionKey(string subscriptionKey)
        {
            this.subscriptionKey = subscriptionKey;
        }

        internal void setToken(string token)
        {
            this.token = token;
            tbToken.Text = token;
        }

        internal void setServerName(string serverName)
        {
            this.serverName = serverName;
            lblServer.Content = serverName;

            if (serverName.Length < 15)
            {
                lblServer.FontSize = 22.0;
            }
            else if (serverName.Length < 20)
            {
                lblServer.FontSize = 18.0;
            }
            else if (serverName.Length < 26)
            {
                lblServer.FontSize = 16.0;
            }
            else
            {
                lblServer.FontSize = 14.0;
            }
        }

        private void tbPassword_PreviewTextInput(object sender, TextCompositionEventArgs e)
        {
            Regex regex = new Regex("[0-9]+");
            if (!regex.IsMatch(e.Text))
            {
                e.Handled = true;
            }
        }

        private void tbPassword_PreviewKeyDown(object sender, KeyEventArgs e)
        {
            //There is some weird bug and PasswordBox allows spacebar so this is a fix for it.
            e.Handled = e.Key == Key.Space;
        }
    }
}
