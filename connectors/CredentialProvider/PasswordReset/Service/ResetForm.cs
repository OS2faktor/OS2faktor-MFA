using CefSharp;
using CefSharp.WinForms;
using CefSharp.WinForms.Internals;
using System;
using System.Web;
using System.Windows.Forms;

namespace PasswordReset.Service
{
    public partial class ResetForm : Form
    {
        private static log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private readonly string proxyUrl = Config.GetProxyUrl();
        public ChromiumWebBrowser chromeBrowser;
        private string user;
        private string pass;

        public ResetForm()
        {
            InitializeComponent();
            
            //at the initialization, start chromium

            InitializeChromium();
        }

        private void btnExit_Click(object sender, EventArgs e)
        {
            this.DialogResult = DialogResult.Cancel;
            this.Close();
        }

        private void InitializeChromium()
        {
            string url = proxyUrl;
            log.Debug("Opening: " + url);

            // Create browser
            chromeBrowser = new ChromiumWebBrowser(url);

            // Add thr browser to the form
            panelTop.Controls.Add(chromeBrowser);

            // make the browser fill the top panel
            chromeBrowser.Dock = DockStyle.Fill;
            chromeBrowser.AddressChanged += ChromeBrowser_AddressChanged;
            chromeBrowser.LoadError += ChromeBrowser_LoadError;
            chromeBrowser.LifeSpanHandler = new LifeSpanHandler();
            chromeBrowser.MenuHandler = new DisabledMenuHandler();
            chromeBrowser.RequestHandler = new RequestHandler();
        }

        private void ChromeBrowser_AddressChanged(object sender, AddressChangedEventArgs e)
        {
            Uri url = new Uri(e.Address);

            log.Debug("AddressChanged: " + url.ToString());

            if (e.Address.Contains(@"status="))
            {
                string status = HttpUtility.ParseQueryString(new Uri(e.Address).Query).Get("status");
                this.DialogResult = DialogResult.No;

                switch (status)
                {
                    case "success":
                        this.user = HttpUtility.ParseQueryString(new Uri(e.Address).Query).Get("username");
                        this.pass = HttpUtility.ParseQueryString(new Uri(e.Address).Query).Get("newPassword");
                        this.DialogResult = DialogResult.OK;
                        break;
                    case "blocked":
                        this.DialogResult = DialogResult.No;
                        break;
                    case "failure":
                    default:
                        this.DialogResult = DialogResult.Abort;
                        break;
                }

                //close dialog anyways
                this.InvokeOnUiThreadIfRequired(() => this.Close());
            }
        }

        private void ChromeBrowser_LoadError(object sender, LoadErrorEventArgs e)
        {
            log.Warn("LoadError: " + e.FailedUrl);
            log.Warn(" .. errorCode: " + e.ErrorCode);
            log.Warn(" .. errorText: " + e.ErrorText);
        }

        public string GetUsername()
        {
            return this.user;
        }

        public string GetPassword()
        {
            return this.pass;
        }
    }
}
