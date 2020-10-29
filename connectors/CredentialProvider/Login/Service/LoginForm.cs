using CefSharp;
using CefSharp.WinForms;
using CefSharp.WinForms.Internals;
using System;
using System.Drawing;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Web;
using System.Windows.Forms;

namespace Login.Service
{
    public partial class LoginForm : Form
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private readonly string baseUrl = Config.GetProxyUrl();
        public ChromiumWebBrowser chromeBrowser;
        const string format = "\"uid\":\"{0}\",\"nonce\":\"{1}\"";
        private string request;

        public LoginForm(string username)
        {
            InitializeComponent();

            InitializeChromium(username);
            
        }

        private void InitializeChromium(string username)
        {

            request = Base64Encode("{" + string.Format(format, username, Guid.NewGuid().ToString()) + "}");
            string url = string.Format(baseUrl + "/auth/login?request={0}", Uri.EscapeDataString(request));

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

        public static string Base64Encode(string plainText)
        {
            var plainTextBytes = System.Text.Encoding.UTF8.GetBytes(plainText);
            return System.Convert.ToBase64String(plainTextBytes);
        }

        private void ChromeBrowser_AddressChanged(object sender, AddressChangedEventArgs e)
        {
            if (e.Address.Contains(@"closeWindow=true"))
            {
                this.DialogResult = DialogResult.Cancel;
                this.Close();
            }
            if (e.Address.Contains(@"success="))
            {
                Uri url = new Uri(e.Address);
                string success = HttpUtility.ParseQueryString(url.Query).Get("success");
                switch (success)
                {
                    case "0":
                        this.DialogResult = DialogResult.Abort;
                        break;
                    case "1":
                        if (e.Address.Contains(@"signature="))
                        {
                            string signature = HttpUtility.ParseQueryString(url.Query).Get("signature");
                            signature = signature.Replace(" ", "+");
                            bool verified = Verify(request, System.Convert.FromBase64String(signature));
                            if (verified)
                            {
                                this.DialogResult = DialogResult.OK;
                                break;
                            }
                        }
                        this.DialogResult = DialogResult.Abort;
                        break;
                    case "2":
                        this.DialogResult = DialogResult.No;
                        break;
                    default:
                        this.DialogResult = DialogResult.None;
                        break;
                }

                this.InvokeOnUiThreadIfRequired(() => this.Close());
            }

        }

        private bool Verify(string text, byte[] signature)
        {
            try
            {
                X509Certificate2 cert = CertificateLoader.LoadCertificateFromMyStore(Config.GetCertificateThumbprint());

                RSACryptoServiceProvider csp = (RSACryptoServiceProvider)cert.PublicKey.Key;

                // Hash the data
                SHA256Managed sha256 = new SHA256Managed();
                var encoder = new UTF8Encoding();
                byte[] data = encoder.GetBytes(text);
                byte[] hash = sha256.ComputeHash(data);

                // Verify the signature with the hash
                return csp.VerifyHash(hash, CryptoConfig.MapNameToOID("SHA256"), signature);
            }
            catch (Exception ex)
            {
                log.Error("Signature validation failed.",ex);
                return false;
            }
        }

        private void ChromeBrowser_LoadError(object sender, LoadErrorEventArgs e)
        {
            log.Warn("LoadError: " + e.FailedUrl);
            log.Warn(" .. errorCode: " + e.ErrorCode);
            log.Warn(" .. errorText: " + e.ErrorText);
        }

        private void btnCancel_Click(object sender, EventArgs e)
        {
            this.DialogResult = DialogResult.Cancel;
            this.Close();
        }
    }
}
