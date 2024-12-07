using System;
using System.Windows.Threading;
using WebSocketSharp;
using Newtonsoft.Json;
using System.Dynamic;
using System.Windows;
using OS2faktor.WebSockets;
using OS2faktor.UI;

namespace OS2faktor
{
    internal static class WSCommunication
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        internal static WebSocket WebSocket { get; set; } = new WebSocket(OS2faktor.Properties.Settings.Default.websocketUrl);

        // called by Quartz, and WILL attempt to connect as long as a deviceId and apiKey is registered.
        // we need to wipe deviceId and apiKey if they are no longer suitable (e.g. when resetting and/or if the backend
        // informs us the client no longer exists)
        internal static void Connect()
        {
            if (string.IsNullOrEmpty(Properties.Settings.Default.deviceId) || string.IsNullOrEmpty(Properties.Settings.Default.apiKey))
            {
                return;
            }

            log.Debug("Attempting to connect");

            WebSocket.SslConfiguration.EnabledSslProtocols = System.Security.Authentication.SslProtocols.Tls12;
            WebSocket.SetCredentials(Properties.Settings.Default.deviceId, Properties.Settings.Default.apiKey, true);
            WebSocket.Connect();
        }

        internal static void Reconnect()
        {
            Disconnect();

            WebSocket = new WebSocket(OS2faktor.Properties.Settings.Default.websocketUrl);

            Init();
            Connect();
        }

        internal static void Init()
        {
            log.Debug("Initializing WebSocket");

            // This event occurs when the WebSocket connection has been established.
            WebSocket.OnOpen += (sender, e) =>
            {
                log.Debug("WebSocket connection opened");
            };

            // This event occurs when the WebSocket receives a message.
            // 
            // e.Data property returns a string, so it is mainly used to get the text message data.
            // e.RawData property returns a byte[], so it is mainly used to get the binary message data.
            WebSocket.OnMessage += (sender, e) =>
            {
                log.Debug("WebSocket: Message received");

                if (e.IsText)
                {
                    // Do something with e.Data.
                    log.Debug("Message: " + e.Data);

                    dynamic message = JsonConvert.DeserializeObject(e.Data);
                    dynamic dataObject = JsonConvert.DeserializeObject(message.data.Value);

                    WSMessageType wsMessageType = dataObject.messageType;
                    string subscriptionKey = dataObject.subscriptionKey;

                    switch (wsMessageType)
                    {
                        case WSMessageType.KEEP_ALIVE:
                            IsAlive();
                            break;
                        case WSMessageType.NOTIFICATION:
                            string challenge = dataObject.challenge;
                            string serverName = dataObject.serverName;
                            string tts = dataObject.tts;

                            App.Current.Dispatcher.Invoke(DispatcherPriority.Normal, new Action(() =>
                            {
                                var notify = new NewNotification();
                                notify.setToken(challenge);
                                notify.setServerName(serverName);
                                notify.setSubscriptionKey(subscriptionKey);
                                notify.setTts(tts);
                                notify.setWindowTimeout();

                                notify.Show(); // must be run after setting token and serverName
                            }));

                            break;
                        case WSMessageType.INCORRECT_PIN:
                            PinResult pinResult = dataObject.pinResult.ToObject<PinResult>();
                            App.Current.Dispatcher.Invoke(DispatcherPriority.Normal, new Action(() =>
                            {
                                foreach (Window window in System.Windows.Application.Current.Windows)
                                {
                                    string windowName = window.GetType().Name;

                                    if (windowName.Equals("NewNotification"))
                                    {
                                        var NewNotification = window as NewNotification;
                                        if (NewNotification.getSubscriptionKey().Equals(subscriptionKey))
                                        {
                                            NewNotification.IncorrectPincode(pinResult);
                                            return;
                                        }
                                    }
                                }
                            }));
                            break;
                        case WSMessageType.CORRECT_PIN:
                            App.Current.Dispatcher.Invoke(DispatcherPriority.Normal, new Action(() =>
                            {
                                foreach (Window window in System.Windows.Application.Current.Windows)
                                {
                                    string windowName = window.GetType().Name;

                                    if (windowName.Equals("NewNotification"))
                                    {
                                        var NewNotification = window as NewNotification;
                                        if (NewNotification.getSubscriptionKey().Equals(subscriptionKey))
                                        {
                                            NewNotification.Close();
                                        }
                                    }
                                }
                            }));
                            break;
                        default:
                            break;
                    }

                }
                else if (e.IsBinary)
                {
                    log.Warn("Got an unexpected binary message from server");
                }
            };

            WebSocket.OnError += (sender, e) =>
            {
                log.Error("Error: " + e.Message);
            };

            WebSocket.OnClose += (sender, e) =>
            {
                log.Error("Connection closed. Code:" + e.Code + " Reason:" + e.Reason);
                if (e.Code == 1008)
                {
                    Properties.Settings.Default.apiKey = null;
                    Properties.Settings.Default.deviceId = null;
                    Properties.Settings.Default.IsNemIDRegistered = false;
                    Properties.Settings.Default.IsPinRegistered = false;
                    Properties.Settings.Default.Save();
                    Properties.Settings.Default.Reload();

                    ((App)App.Current).UpdateContextMenuVisibility();
                }

                // in case of a shutdown - create a new instance, which will be connected through WebSocketConnectionJob
                WebSocket = new WebSocket(OS2faktor.Properties.Settings.Default.websocketUrl);
                Init();
            };
        }

        internal static void Disconnect()
        {
            WebSocket.Close();
        }

        internal static void Accept(string subscriptionKey, string pincode)
        {
            dynamic response = new ExpandoObject();
            response.subscriptionKey = subscriptionKey;
            response.status = "ACCEPT";
            response.version = OS2faktor.App.CLIENT_VERSION;
            response.pin = pincode;

            WebSocket.Send(JsonConvert.SerializeObject(response));
        }

        internal static void Reject(string subscriptionKey)
        {
            dynamic response = new ExpandoObject();
            response.subscriptionKey = subscriptionKey;
            response.status = "REJECT";
            response.version = OS2faktor.App.CLIENT_VERSION;

            WebSocket.Send(JsonConvert.SerializeObject(response));
        }

        internal static void IsAlive()
        {
            dynamic response = new ExpandoObject();
            response.status = "ISALIVE";
            response.version = OS2faktor.App.CLIENT_VERSION;

            WebSocket.Send(JsonConvert.SerializeObject(response));
        }
    }
}
