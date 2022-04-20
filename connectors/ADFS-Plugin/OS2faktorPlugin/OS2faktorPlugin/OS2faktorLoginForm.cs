using Microsoft.IdentityServer.Web.Authentication.External;

namespace OS2faktorPlugin
{
    class OS2faktorLoginForm : IAdapterPresentationForm
    {
        private string challenge;
        private string redirectUrl;
        private string pollingUrl;
        private string chromeClient;
        private bool setLocalStorageNull;
        private bool showRememberDevice;
        private bool useiFrame;

        public OS2faktorLoginForm(string challenge, string redirectUrl, string pollingUrl, string chromeClient, bool setLocalStorageNull, bool showRememberDevice, bool useiFrame)
        {
            this.chromeClient = chromeClient;
            this.challenge = challenge;
            this.redirectUrl = redirectUrl;
            this.pollingUrl = pollingUrl;
            this.setLocalStorageNull = setLocalStorageNull;
            this.showRememberDevice = showRememberDevice;
            this.useiFrame = useiFrame;
        }

        /// Returns the HTML Form fragment that contains the adapter user interface. This data will be included in the web page that is presented
        /// to the cient.
        public string GetFormHtml(int lcid)
        {
            string htmlTemplate = Resources.OS2faktorLoginForm;

            htmlTemplate = htmlTemplate.Replace("@@CHROMECLIENT@@", this.chromeClient);
            htmlTemplate = htmlTemplate.Replace("@@CHALLENGE@@", this.challenge);
            htmlTemplate = htmlTemplate.Replace("@@USEIFRAME@@", this.useiFrame ? "true" : "false");
            htmlTemplate = htmlTemplate.Replace("@@REDIRECTURL@@", this.redirectUrl);
            htmlTemplate = htmlTemplate.Replace("@@SETLOCALSTORAGENULL@@", this.setLocalStorageNull ? "true" : "false");
            htmlTemplate = htmlTemplate.Replace("@@SHOWREMEMBERDEVICE@@", this.showRememberDevice ? "true" : "false");
            return htmlTemplate.Replace("@@POLLINGURL@@", this.pollingUrl);
        }

        /// Return any external resources, ie references to libraries etc., that should be included in 
        /// the HEAD section of the presentation form html. 
        public string GetFormPreRenderHtml(int lcid)
        {
            return null;
        }

        // Returns the title string for the web page which presents the HTML form content to the end user
        public string GetPageTitle(int lcid)
        {
            return "OS2faktor";
        }
    }
}
