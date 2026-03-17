using OS2faktor.Service;
using OS2faktor.Service.DTO;
using OS2faktor.Utils;
using OS2faktor.WebSockets;
using System;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
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
        private const int CONTROL_CODE_LENGTH = 2;

        private string token;
        private string serverName;
        private string subscriptionKey;
        private DispatcherTimer dispatcherTimer;
        private bool preventSpam = false;
        private bool isPinRegistered = OS2faktor.Properties.Settings.Default.IsPinRegistered;
        private int reactivatedWindowCounter = 0;

        private int pinLength;
        private string pin;
        private string enteredPin = "";

        // Passwordless flow state
        private bool isPasswordlessFlow = false;
        private bool inControlCodeMode = false;
        private string storedPin = "";
        private string enteredControlCode = "";

        public NewNotification()
        {
            pinLength = OS2faktor.Properties.Settings.Default.pinLength;
            if (pinLength == 0) pinLength = 4;

            pin = new string(OPEN_CIRCLE, pinLength);

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

            var workingArea = System.Windows.SystemParameters.WorkArea;

            this.Left = workingArea.Right - this.ActualWidth - 20;
            double top = workingArea.Bottom - this.ActualHeight - 50;

            foreach (Window window in System.Windows.Application.Current.Windows)
            {
                string windowName = window.GetType().Name;

                // Prevent showing same notification twice
                if (windowName.Equals("NewNotification") && window != this)
                {
                    var otherNotification = window as NewNotification;
                    if (otherNotification.serverName.Equals(this.serverName) && String.Equals(otherNotification.token, this.token, StringComparison.OrdinalIgnoreCase))
                    {
                        this.Close();
                        return;
                    }
                }

                // Adjust position
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
                    if (window.Top < this.Top)
                    {
                        window.Top = window.Top + window.ActualHeight;
                    }
                }
            }
        }

        private void btnApply_Click(object sender, RoutedEventArgs e)
        {
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

            if ("??".Equals(token))
            {
                isPasswordlessFlow = true;
                tbCode.Visibility = Visibility.Hidden;
            }
            else
            {
                tbCode.Text = token;
            }
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
         * Handle pincode / control code entry
         */
        private void Window_TextInput(object sender, TextCompositionEventArgs e)
        {
            if (!isPinRegistered)
            {
                return;
            }

            if (preventSpam)
            {
                return;
            }

            Regex regex = new Regex("[^0-9]+");
            if (regex.IsMatch(e.Text))
            {
                return;
            }

            if (inControlCodeMode)
            {
                HandleControlCodeInput(e.Text);
            }
            else
            {
                HandlePinInput(e.Text);
            }
        }

        private void HandlePinInput(string digit)
        {
            if (enteredPin.Length < pinLength - 1)
            {
                enteredPin += digit;
                DisplayPincode();
            }
            else if (enteredPin.Length == pinLength - 1)
            {
                enteredPin += digit;
                DisplayPincode();

                if (isPasswordlessFlow)
                {
                    // Validate PIN via REST before asking for control code
                    preventSpam = true;
                    var backendService = new BackendService();
                    backendService.ValidatePinAsync(enteredPin).ContinueWith(task =>
                    {
                        var result = task.Result;
                        App.Current.Dispatcher.Invoke(DispatcherPriority.Normal, new Action(() =>
                        {
                            preventSpam = false;

                            if (result.Status == "OK")
                            {
                                storedPin = enteredPin;
                                enteredPin = "";
                                SwitchToControlCodeMode();
                            }
                            else if (result.Status == "LOCKED")
                            {
                                enteredPin = "";
                                DisplayPincode();
                                ShakeAnimation();
                                ShowTextWithIcon(string.Format("Klient låst indtil: {0}", result.LockedUntil));
                            }
                            else
                            {
                                enteredPin = "";
                                DisplayPincode();
                                ShakeAnimation();
                                ShowTextWithIcon("Forkert PIN-kode!");
                            }
                        }));
                    });
                }
                else
                {
                    // Visual approval: send PIN directly via WebSocket
                    WSCommunication.Accept(subscriptionKey, enteredPin);
                    preventSpam = true;
                    DelayedAntispamUnlock();
                }
            }
        }

        // passwordless flow
        private void HandleControlCodeInput(string digit)
        {
            if (enteredControlCode.Length < CONTROL_CODE_LENGTH - 1)
            {
                enteredControlCode += digit;
                DisplayPincode();
            }
            else if (enteredControlCode.Length == CONTROL_CODE_LENGTH - 1)
            {
                enteredControlCode += digit;
                DisplayPincode();

                WSCommunication.Accept(subscriptionKey, storedPin, enteredControlCode);
                preventSpam = true;
                DelayedAntispamUnlock();
            }
        }

        private void SwitchToControlCodeMode()
        {
            inControlCodeMode = true;
            enteredControlCode = "";
            pin = new string(OPEN_CIRCLE, CONTROL_CODE_LENGTH);
            tbPIN.Text = pin;
            tbText.Text = "Indtast kontrolkode";
        }

        /*
         * Update PIN / control code UI
         */
        private void DisplayPincode()
        {
            string entered = inControlCodeMode ? enteredControlCode : enteredPin;
            int length = inControlCodeMode ? CONTROL_CODE_LENGTH : pinLength;

            char[] ch = new char[length];
            for (int i = 0; i < length; i++)
            {
                ch[i] = i < entered.Length ? CLOSED_CIRCLE : OPEN_CIRCLE;
            }

            pin = new string(ch);
            tbPIN.Text = pin;
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

            if (e.Key == Key.Back || e.Key == Key.Delete)
            {
                if (inControlCodeMode && enteredControlCode.Length > 0)
                {
                    enteredControlCode = enteredControlCode.Substring(0, enteredControlCode.Length - 1);
                    DisplayPincode();
                }
                else if (!inControlCodeMode && enteredPin.Length > 0)
                {
                    enteredPin = enteredPin.Substring(0, enteredPin.Length - 1);
                    DisplayPincode();
                }
            }
        }

        /*
         * Code that triggers the shaking animation of the PIN/code textbox
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
         * When we get an INCORRECT_PIN response from WebSockets:
         * - In control code mode (passwordless): reset control code entry
         * - In PIN entry mode (visual approval): clear PIN and show error
         */
        internal void IncorrectPincode(PinResult pinResult)
        {
            if (inControlCodeMode)
            {
                enteredControlCode = "";
                DisplayPincode();
                ShakeAnimation();

                if (pinResult != null && pinResult.status == PinResultStatus.LOCKED)
                {
                    inControlCodeMode = false;
                    ShowTextWithIcon(string.Format("Klient låst indtil: {0}", pinResult.lockedUntil));
                }
                else
                {
                    ShowTextWithIcon("Forkert kontrolkode!");
                }
            }
            else
            {
                enteredPin = "";
                DisplayPincode();
                ShakeAnimation();

                if (pinResult != null)
                {
                    switch (pinResult.status)
                    {
                        case PinResultStatus.WRONG_PIN:
                            ShowTextWithIcon("Forkert Pin!");
                            break;
                        case PinResultStatus.LOCKED:
                            ShowTextWithIcon(string.Format("Klient låst indtil: {0}", pinResult.lockedUntil));
                            break;
                    }
                }
            }
        }

        private void ShowTextWithIcon(string message)
        {
            FontAwesome5.ImageAwesome faIcon = new FontAwesome5.ImageAwesome();
            faIcon.Icon = FontAwesome5.EFontAwesomeIcon.Solid_ExclamationCircle;
            faIcon.Foreground = Brushes.White;
            faIcon.Margin = new Thickness(0, 0, 4, -2);
            faIcon.Height = tbText.FontSize;

            tbText.Text = "";
            tbText.Inlines.Add(faIcon);
            tbText.Inlines.Add(message);
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
