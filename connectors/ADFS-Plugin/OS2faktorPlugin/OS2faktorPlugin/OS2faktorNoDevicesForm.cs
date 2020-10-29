using Microsoft.IdentityServer.Web.Authentication.External;
using System.Collections.Generic;

namespace OS2faktorPlugin
{
    class OS2faktorNoDevicesForm : IAdapterPresentationForm
    {
        private string getDeviceUrl;
        private string allowSelfRegistration;
        private string showError;

        public OS2faktorNoDevicesForm(string getDeviceUrl, bool allowSelfRegistration, bool showError = false)
        {
            this.getDeviceUrl = getDeviceUrl;
            this.allowSelfRegistration = (allowSelfRegistration) ? "true" : "false";
            this.showError = (showError) ? "true" : "false";
        }

        public string GetFormHtml(int lcid)
        {
            string htmlTemplate = Resources.OS2faktorNoDevicesForm;
            htmlTemplate = htmlTemplate.Replace("@@ALLOWSELFREG@@", this.allowSelfRegistration);
            htmlTemplate = htmlTemplate.Replace("@@SHOWERROR@@", this.showError);

            return htmlTemplate.Replace("@@GETDEVICEURL@@", this.getDeviceUrl);
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
