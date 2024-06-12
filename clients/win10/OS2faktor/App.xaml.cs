using Microsoft.Win32;
using OS2faktor.Service;
using OS2faktor.Service.DTO;
using OS2faktor.Utils;
using Quartz;
using Quartz.Impl;
using System;
using System.Drawing;
using System.IO;
using System.Net;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Threading;
using System.Xml.Linq;

namespace OS2faktor
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : Application
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        public static readonly string CLIENT_VERSION = OS2faktor.Properties.Settings.Default.version;
        private static IScheduler sched;
        private System.Windows.Forms.NotifyIcon notifyIcon;
        private BackendService backendService;

        private void Application_Startup(object sender, StartupEventArgs e)
        {
            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;

            this.ShutdownMode = ShutdownMode.OnExplicitShutdown; //this prevents application form closing when we close a window.

            // allow restart manager to restart application (used by InnoSetup)
            RestartManagerWrapper.RegisterApplicationRestart(null, 0);

            // Allow Selfsigned certs
            ServicePointManager.ServerCertificateValidationCallback += (s, certificate, chain, sslPolicyErrors) => true;

            // Attempt to migrate any existing configuration (old client installations)
            MigrateExistingConfiguration();

            WSCommunication.Init();

            var bitmap = new Bitmap(OS2faktor.Properties.Resources.IconPNG);
            var iconHandle = bitmap.GetHicon();

            notifyIcon = new System.Windows.Forms.NotifyIcon();
            notifyIcon.Text = "OS2faktor";
            notifyIcon.Icon = Icon.FromHandle(iconHandle);
            notifyIcon.ContextMenu = CreateContextMenu();
            notifyIcon.Visible = true;

            //Fetch clients status from backend
            backendService = new BackendService();
            if (IsRegistered())
            {
                backendService.GetInitialStatusAsync().ContinueWith((finishedTask) =>
                {
                    StatusResult result = finishedTask.Result;
                    if (result != null)
                    {
                        if (!result.Disabled) //If exists
                        {
                            if (result.PinProtected && !OS2faktor.Properties.Settings.Default.IsPinRegistered)
                            {
                                OS2faktor.Properties.Settings.Default.IsPinRegistered = true;
                            }

                            if (result.NemIdRegistered && !OS2faktor.Properties.Settings.Default.IsNemIDRegistered)
                            {
                                OS2faktor.Properties.Settings.Default.IsNemIDRegistered = true;
                            }
                        }
                        else if (!result.LookupFailed)
                        {
                            //Client was removed in the backend
                            OS2faktor.Properties.Settings.Default.apiKey = null;
                            OS2faktor.Properties.Settings.Default.deviceId = null;
                            OS2faktor.Properties.Settings.Default.IsNemIDRegistered = false;
                            OS2faktor.Properties.Settings.Default.IsPinRegistered = false;
                        }
                        else
                        {
                            log.Error("Cannot connect to backend to fetch status");
                        }
                        OS2faktor.Properties.Settings.Default.Save();
                        OS2faktor.Properties.Settings.Default.Reload();
                    }
                    UpdateContextMenuVisibility();
                });
            }

            UpdateContextMenuVisibility();

            // Wait for Scheduler to be initialized
            var task = InitScheduler();
            task.Wait();
            sched = task.Result;

            // Create jobs
            InitSchedulerWebSocket();
        }

        private void MigrateExistingConfiguration()
        {
            // if registered, just skip this
            if (!string.IsNullOrEmpty(OS2faktor.Properties.Settings.Default.deviceId))
            {
                return;
            }

            bool migrated = false;
            try
            {
                string appDataPath = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
                appDataPath = Path.Combine(appDataPath, "Digital_Identity");

                if (Directory.Exists(appDataPath))
                {
                    foreach (var dir in Directory.GetDirectories(appDataPath))
                    {
                        if (dir.Contains("OS2FaktorKlient.exe"))
                        {
                            // read file and see if contains useful information
                            string configPath = Path.Combine(dir, "1.4.0.0\\user.config");

                            OldConfigContent content = ParseOldConfigFile(configPath);
                            if (content != null)
                            {
                                OS2faktor.Properties.Settings.Default.apiKey = content.apiKey;
                                OS2faktor.Properties.Settings.Default.deviceId = content.deviceId;
                                OS2faktor.Properties.Settings.Default.IsNemIDRegistered = content.isNemIdRegistered;
                                OS2faktor.Properties.Settings.Default.IsPinRegistered = content.isPinRegistered;
                                OS2faktor.Properties.Settings.Default.Save();
                                OS2faktor.Properties.Settings.Default.Reload();

                                log.Info("Migrated data from previous 1.4.0 installation");
                                migrated = true;
                            }
                            else
                            {
                                configPath = Path.Combine(dir, "1.3.0.0\\user.config");

                                content = ParseOldConfigFile(configPath);
                                if (content != null)
                                {
                                    OS2faktor.Properties.Settings.Default.apiKey = content.apiKey;
                                    OS2faktor.Properties.Settings.Default.deviceId = content.deviceId;
                                    OS2faktor.Properties.Settings.Default.IsNemIDRegistered = content.isNemIdRegistered;
                                    OS2faktor.Properties.Settings.Default.IsPinRegistered = content.isPinRegistered;
                                    OS2faktor.Properties.Settings.Default.Save();
                                    OS2faktor.Properties.Settings.Default.Reload();

                                    log.Info("Migrated data from previous 1.3.0 installation");
                                    migrated = true;
                                }
                            }
                        }
                    }
                }

                if (!migrated)
                {
                    appDataPath = appDataPath.Replace("Roaming", "Local");
                    if (Directory.Exists(appDataPath))
                    {
                        foreach (var dir in Directory.GetDirectories(appDataPath))
                        {
                            if (dir.Contains("OS2FaktorKlient.exe"))
                            {
                                var configPath = Path.Combine(dir, "1.0.0.0\\user.config");

                                var content = ParseOldConfigFile(configPath);
                                if (content != null)
                                {
                                    OS2faktor.Properties.Settings.Default.apiKey = content.apiKey;
                                    OS2faktor.Properties.Settings.Default.deviceId = content.deviceId;
                                    OS2faktor.Properties.Settings.Default.IsNemIDRegistered = content.isNemIdRegistered;
                                    OS2faktor.Properties.Settings.Default.IsPinRegistered = false;
                                    OS2faktor.Properties.Settings.Default.Save();
                                    OS2faktor.Properties.Settings.Default.Reload();

                                    log.Info("Migrated data from previous 1.0.0 installation");
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                log.Error("Failed migration: ", ex);
            }
        }

        class OldConfigContent
        {
            public string apiKey;
            public string deviceId;
            public bool isNemIdRegistered;
            public bool isPinRegistered;
        }

        private OldConfigContent ParseOldConfigFile(string path)
        {
            if (!File.Exists(path))
            {
                return null;
            }

            var elem = XElement.Load(path);
            if (elem == null)
            {
                return null;
            }

            var userSettings = elem.Element("userSettings");
            if (userSettings == null)
            {
                return null;
            }

            var settings = userSettings.Element("OS2faktor.Properties.Settings");
            if (settings == null)
            {
                return null;
            }

            OldConfigContent content = new OldConfigContent();
            foreach (var setting in settings.Elements())
            {
                var nameAttribute = setting.Attribute("name");
                if (nameAttribute == null)
                {
                    continue;
                }

                var name = nameAttribute.Value;
                var value = setting.Value;

                if ("apiKey".Equals(name))
                {
                    content.apiKey = value;
                }
                else if ("deviceId".Equals(name))
                {
                    content.deviceId = value;
                }
                else if ("IsNemIDRegistered".Equals(name))
                {
                    content.isNemIdRegistered = "True".Equals(value);
                }
                else if ("IsPinRegistered".Equals(name))
                {
                    content.isPinRegistered = "True".Equals(value);
                }
            }

            // skip not registered installations
            if (string.IsNullOrEmpty(content.apiKey) || string.IsNullOrEmpty(content.deviceId))
            {
                return null;
            }

            return content;
        }


        private static async Task InitSchedulerWebSocket()
        {
            // create job
            IJobDetail job = JobBuilder.Create<WebSocketConnectJob>()
                    .WithIdentity("job1", "group1")
                    .Build();

            // create trigger
            ITrigger trigger = TriggerBuilder.Create()
                .WithIdentity("trigger1", "group1")
                .WithSimpleSchedule(x => x.WithIntervalInSeconds(30).RepeatForever())
                .Build();

            // Schedule the job using the job and trigger 
            await sched.ScheduleJob(job, trigger);
        }

        private static async Task<IScheduler> InitScheduler()
        {
            // construct a scheduler factory
            ISchedulerFactory schedFact = new StdSchedulerFactory();

            // get a scheduler, start the schedular before triggers or anything else
            IScheduler sched = await schedFact.GetScheduler();
            await sched.Start();

            return sched;
        }

        private System.Windows.Forms.ContextMenu CreateContextMenu()
        {
            System.Windows.Forms.ContextMenu contextMenu1 = new System.Windows.Forms.ContextMenu();

            System.Windows.Forms.MenuItem exitMenu = new System.Windows.Forms.MenuItem();
            exitMenu.Tag = "Exit";
            exitMenu.Text = "Luk";
            exitMenu.Click += new EventHandler(exitMenu_Click);

            System.Windows.Forms.MenuItem resetMenu = new System.Windows.Forms.MenuItem();
            resetMenu.Tag = "Reset";
            resetMenu.Text = "Nulstil enhed";
            resetMenu.Click += new EventHandler(resetMenu_Click);

            System.Windows.Forms.MenuItem registerMenu = new System.Windows.Forms.MenuItem();
            registerMenu.Tag = "Register";
            registerMenu.Text = "Aktiver enhed";
            registerMenu.Click += new EventHandler(registerMenu_Click);

            System.Windows.Forms.MenuItem registerNemIDMenu = new System.Windows.Forms.MenuItem();
            registerNemIDMenu.Tag = "MitID";
            registerNemIDMenu.Text = "Udfør aktivering med MitID";
            registerNemIDMenu.Click += new EventHandler(registerNemIDMenu_Click);

            System.Windows.Forms.MenuItem registerPinMenu = new System.Windows.Forms.MenuItem();
            registerPinMenu.Tag = "Pin";
            registerPinMenu.Text = "Beskyt med pinkode";
            registerPinMenu.Click += new EventHandler(registerPinMenu_Click);

            System.Windows.Forms.MenuItem deviceIdMenu = new System.Windows.Forms.MenuItem();
            deviceIdMenu.Tag = "DeviceId";
            deviceIdMenu.Text = "Ikke aktiveret endnu";
            deviceIdMenu.Click += new EventHandler(deviceIdMenu_Click);

            System.Windows.Forms.MenuItem appVersionMenu = new System.Windows.Forms.MenuItem();
            appVersionMenu.Tag = "AppVersion";
            appVersionMenu.Text = "Version: " + OS2faktor.Properties.Settings.Default.version;
            appVersionMenu.Enabled = false;

            System.Windows.Forms.MenuItem selfServiceMenu = new System.Windows.Forms.MenuItem();
            selfServiceMenu.Tag = "SelfService";
            selfServiceMenu.Text = "Administration af enheder";
            selfServiceMenu.Click += new EventHandler(selfServiceMenu_Click);

            // Initialize contextMenu1
            contextMenu1.MenuItems.Add(resetMenu);
            contextMenu1.MenuItems.Add(registerMenu);
            contextMenu1.MenuItems.Add(registerNemIDMenu);
            contextMenu1.MenuItems.Add(registerPinMenu);
            contextMenu1.MenuItems.Add(selfServiceMenu);
            contextMenu1.MenuItems.Add(exitMenu);

            contextMenu1.MenuItems.Add("-").Tag = "Splitter";
            contextMenu1.MenuItems.Add(deviceIdMenu);
            contextMenu1.MenuItems.Add("-").Tag = "Splitter";
            contextMenu1.MenuItems.Add(appVersionMenu);

            return contextMenu1;
        }

        private void selfServiceMenu_Click(object sender, EventArgs e)
        {
            App.Current.Dispatcher.Invoke(DispatcherPriority.Normal, new Action(() =>
            {
                var selfservice = new SelfServiceWindow();
                selfservice.Show();
            }));
        }

        private void deviceIdMenu_Click(object sender, EventArgs e)
        {
            Clipboard.SetDataObject(OS2faktor.Properties.Settings.Default.deviceId);
        }

        private void registerMenu_Click(object sender, EventArgs e)
        {
            App.Current.Dispatcher.Invoke(DispatcherPriority.Normal, new Action(() =>
            {
                var registration = new RegistrationWindow();
                registration.Show();
            }));
        }

        private void registerPinMenu_Click(object sender, EventArgs e)
        {
            App.Current.Dispatcher.Invoke(DispatcherPriority.Normal, new Action(() =>
            {
                var pinRegistration = new RegisterPinDialog();
                pinRegistration.Show();
            }));
        }

        private void registerNemIDMenu_Click(object sender, EventArgs e)
        {
            App.Current.Dispatcher.Invoke(DispatcherPriority.Normal, new Action(() =>
            {
                var registrationNemID = new RegistrationNemIDWindow();
                registrationNemID.Show();
            }));
        }

        private void resetMenu_Click(object sender, EventArgs e)
        {
            string message = "Er du sikker på at du vil nulstille klienten?";
            string title = "Nulstil klient";

            var buttons = MessageBoxButton.YesNo;
            var result = MessageBox.Show(message, title, buttons);

            if (result.Equals(MessageBoxResult.Yes))
            {
                WSCommunication.Disconnect();

                _ = backendService.Delete(OS2faktor.Properties.Settings.Default.deviceId);
                

                OS2faktor.Properties.Settings.Default.apiKey = null;
                OS2faktor.Properties.Settings.Default.deviceId = null;
                OS2faktor.Properties.Settings.Default.IsNemIDRegistered = false;
                OS2faktor.Properties.Settings.Default.IsPinRegistered = false;
                OS2faktor.Properties.Settings.Default.Save();
                OS2faktor.Properties.Settings.Default.Reload();

                UpdateContextMenuVisibility();
            }
        }

        private void exitMenu_Click(object sender, EventArgs e)
        {
            WSCommunication.Disconnect();
            Application.Current.Shutdown();
        }

        public void UpdateContextMenuVisibility()
        {
            if (IsRegistered())
            {
                FindMenuItemByTag("DeviceId").Text = "OS2faktor ID: " + OS2faktor.Properties.Settings.Default.deviceId;

                FindMenuItemByTag("Reset").Enabled = true;
                FindMenuItemByTag("Register").Enabled = false;
                FindMenuItemByTag("DeviceId").Enabled = true;
                FindMenuItemByTag("SelfService").Enabled = true;
                FindMenuItemByTag("Pin").Enabled = true;
                FindMenuItemByTag("MitID").Enabled = true;

                if (OS2faktor.Properties.Settings.Default.IsNemIDRegistered)
                {
                    FindMenuItemByTag("MitID").Enabled = false;
                }

                if (OS2faktor.Properties.Settings.Default.IsPinRegistered)
                {
                    FindMenuItemByTag("Pin").Enabled = false;
                }
            }
            else
            {
                FindMenuItemByTag("DeviceId").Text = "Ikke aktiveret endnu";

                FindMenuItemByTag("Reset").Enabled = false;
                FindMenuItemByTag("Register").Enabled = true;
                FindMenuItemByTag("DeviceId").Enabled = false;
                FindMenuItemByTag("SelfService").Enabled = false;
                FindMenuItemByTag("MitID").Enabled = false;
                FindMenuItemByTag("Pin").Enabled = false;
            }
        }

        //It will return an item or throw an exception
        private System.Windows.Forms.MenuItem FindMenuItemByTag(string tag)
        {
            var enumerator = notifyIcon.ContextMenu?.MenuItems?.GetEnumerator();
            while ((bool)enumerator?.MoveNext())
            {
                var item = (System.Windows.Forms.MenuItem)enumerator.Current;
                if (string.Equals(item.Tag as string, tag, StringComparison.CurrentCultureIgnoreCase))
                {
                    return item;
                }
            }
            throw new Exception("MenuItem for tag: " + tag + " not found.");
        }

        private bool IsRegistered()
        {
            if (!string.IsNullOrEmpty(OS2faktor.Properties.Settings.Default.deviceId) && !string.IsNullOrEmpty(OS2faktor.Properties.Settings.Default.apiKey))
            {
                return true;
            }

            return false;
        }

        private void Application_Exit(object sender, ExitEventArgs e)
        {
            notifyIcon.Visible = false;
            notifyIcon.Icon = null;
            notifyIcon.Dispose();
        }
    }
}
