using Microsoft.IdentityServer.Web.Authentication.External;

namespace OS2faktorPlugin
{
    class OS2faktorCheckRememberDeviceForm : IAdapterPresentationForm
    {

        public OS2faktorCheckRememberDeviceForm()
        {
          
        }

        /// Returns the HTML Form fragment that contains the adapter user interface. This data will be included in the web page that is presented
        /// to the cient.
        public string GetFormHtml(int lcid)
        {
            string htmlTemplate = Resources.OS2faktorCheckRememberDeviceForm;

            return htmlTemplate;
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
