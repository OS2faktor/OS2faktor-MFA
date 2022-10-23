using OS2faktor.Utils;
using OS2faktor.WebSockets;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;
using System.Windows.Threading;

namespace OS2faktor.UI
{
    /// <summary>
    /// Interaction logic for NewNotification.xaml
    /// </summary>
    public partial class NewNotification : Window
    {
        private const char OPEN_CIRCLE = '\u26AA';
        private const char CLOSED_CIRCLE = '\u26AB';
        private string token;
        private string serverName;
        private string subscriptionKey;
        private DispatcherTimer dispatcherTimer;
        private bool preventSpam = false;
        private string pin = "" + OPEN_CIRCLE + OPEN_CIRCLE + OPEN_CIRCLE + OPEN_CIRCLE;
        private string enteredPin = "";
        private bool isPinRegistered = OS2faktor.Properties.Settings.Default.IsPinRegistered;
        private int reactivatedWindowCounter = 0;

        public NewNotification()
        {
            InitializeComponent();
            tbPIN.Text = pin;

            if (isPinRegistered)
            {
                tbText.Text = "Indtast PIN-kode";
                tbText.Focus();
                btnApply.Visibility = Visibility.Hidden;
            }
            else
            {
                tbText.Text = "";
                btnApply.Visibility = Visibility.Visible;
                tbPIN.Visibility = Visibility.Hidden;
            }
        }

        public new void Show()
        {
            this.Topmost = true;
            base.Show();

            //this.Owner = System.Windows.Application.Current.MainWindow;

            var workingArea = System.Windows.SystemParameters.WorkArea;

            this.Left = workingArea.Right - this.ActualWidth - 20;
            double top = workingArea.Bottom - this.ActualHeight - 50;

            foreach (Window window in System.Windows.Application.Current.Windows)
            {
                string windowName = window.GetType().Name;

                //Prevent showing same notification twice
                if (windowName.Equals("NewNotification") && window != this)
                {
                    var otherNotification = window as NewNotification;
                    if (otherNotification.serverName.Equals(this.serverName) && String.Equals(otherNotification.token, this.token, StringComparison.OrdinalIgnoreCase))
                    {
                        this.Close();
                        return;
                    }
                }

                //Adjust position
                if (windowName.Equals("NewNotification") && window != this)
                {
                    window.Topmost = true;
                    top = window.Top - window.ActualHeight;
                }
            }

            this.Top = top;

            AsyncUtils.DelayCall(200, () =>
            {
                this.Activate();
            });
        }

        private void NotificationClosed(object sender, EventArgs e)
        {
            foreach (Window window in System.Windows.Application.Current.Windows)
            {
                string windowName = window.GetType().Name;

                if (windowName.Equals("NewNotification") && window != this)
                {
                    // Adjust any windows that were above this one to drop down
                    if (window.Top < this.Top)
                    {
                        window.Top = window.Top + window.ActualHeight;
                    }
                }
            }
        }
        private void btnApply_Click(object sender, RoutedEventArgs e)
        {
            //simple prevention against brute-force
            if (preventSpam == false)
            {
                WSCommunication.Accept(subscriptionKey, enteredPin);
                preventSpam = true;
                DelayedAntispamUnlock();
            }
        }

        private void DelayedAntispamUnlock()
        {
            new Thread(() =>
            {
                Thread.CurrentThread.IsBackground = true;
                Thread.Sleep(1000);
                preventSpam = false;
            }).Start();
        }

        private void btnClose_Click(object sender, RoutedEventArgs e)
        {
            WSCommunication.Reject(subscriptionKey);
            this.Close();
        }

        public void setWindowTimeout()
        {
            this.dispatcherTimer = new System.Windows.Threading.DispatcherTimer();
            dispatcherTimer.Tick += new EventHandler((s, x) =>
            {
                dispatcherTimer.Stop();
                this.Close();
            });

            dispatcherTimer.Interval = new TimeSpan(0, 0, 5, 0, 0);

            dispatcherTimer.Start();
        }

        private void Window_MouseDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left)
                this.DragMove();
        }

        internal void setToken(string token)
        {
            this.token = token;
            tbCode.Text = token;
        }

        internal void setServerName(string serverName)
        {
            this.serverName = serverName;
            tbServerName.Text = serverName;
        }

        internal void setSubscriptionKey(string subscriptionKey)
        {
            this.subscriptionKey = subscriptionKey;
        }

        internal string getSubscriptionKey()
        {
            return this.subscriptionKey;
        }

        internal void setTts(string tts)
        {
            if (!string.IsNullOrEmpty(tts))
            {
                tbTime.Text = $"Ankommet {tts}";
            }
        }

        /*
         * Handle pincode entry
         */
        private void Window_TextInput(object sender, TextCompositionEventArgs e)
        {
            if (!isPinRegistered)
            {
                return;
            }

            //simple prevention against brute-force
            if (preventSpam == false)
            {
                Regex regex = new Regex("[^0-9]+");
                if (!regex.IsMatch(e.Text))
                {
                    if (enteredPin.Length < 3)
                    {
                        enteredPin += e.Text;
                        DisplayPincode();
                    }
                    //entered last character
                    else if (enteredPin.Length == 3)
                    {
                        enteredPin += e.Text;
                        DisplayPincode();
                        WSCommunication.Accept(subscriptionKey, enteredPin);

                        preventSpam = true;
                        DelayedAntispamUnlock();
                    }
                }
            }
        }

        /*
         * Update Pincode UI 
         */
        private void DisplayPincode()
        {
            for (int i = 0; i < enteredPin.Length; i++)
            {
                replaceAtIndex(ref pin, i, CLOSED_CIRCLE);
            }
            for (int i = enteredPin.Length; i < 4; i++)
            {
                replaceAtIndex(ref pin, i, OPEN_CIRCLE);
            }

            tbPIN.Text = pin;
        }

        private void replaceAtIndex(ref string text, int index, char replaceWith)
        {
            char[] ch = text.ToCharArray();
            ch[index] = replaceWith;
            text = new string(ch);
        }

        /*
         * We need this method to catch backspace or delete in order to clear pincode
         */
        private void Window_KeyDown(object sender, KeyEventArgs e)
        {
            if (!isPinRegistered)
            {
                return;
            }

            if ((e.Key == Key.Back || e.Key == Key.Delete) && enteredPin.Length > 0)
            {
                enteredPin = enteredPin.Substring(0, enteredPin.Length - 1);
                DisplayPincode();
            }
        }

        /*
         * Code that triggers the shaking animation of Pincode textbox
         */
        private void ShakeAnimation()
        {
            Storyboard sb = this.FindResource("ShakeAnimation") as Storyboard;
            if (sb != null)
            {
                BeginStoryboard(sb);
            }
        }

        /*
         * When we get response from WebSockets
         * Clear the PIN entry
         * Trigger shake animation
         * Display error message
         */
        internal void IncorrectPincode(PinResult pinResult)
        {
            enteredPin = "";
            DisplayPincode();
            ShakeAnimation();

            FontAwesome5.ImageAwesome faIcon = new FontAwesome5.ImageAwesome();
            faIcon.Icon = FontAwesome5.EFontAwesomeIcon.Solid_ExclamationCircle;
            faIcon.Foreground = Brushes.White;
            faIcon.Margin = new Thickness(0, 0, 4, -2);
            faIcon.Height = tbText.FontSize;

            if (pinResult != null)
            {
                switch (pinResult.status)
                {
                    case WebSockets.PinResultStatus.WRONG_PIN:
                        tbText.Text = "";
                        tbText.Inlines.Add(faIcon);
                        tbText.Inlines.Add("Forkert Pin!");
                        break;
                    case WebSockets.PinResultStatus.LOCKED:
                        tbText.Text = "";
                        tbText.Inlines.Add(faIcon);
                        tbText.Inlines.Add(string.Format("Klient låst indtil: {0}", pinResult.lockedUntil));
                        break;
                }
            }
        }

        private void Window_Deactivated(object sender, EventArgs e)
        {
            if (reactivatedWindowCounter < 10)
            {
                AsyncUtils.DelayCall(200, () =>
                {
                    reactivatedWindowCounter++;
                    this.Activate();
                });
            }
        }

    }
}
