using Microsoft.IdentityServer.Web.Authentication.External;

namespace OS2faktorPlugin
{
    class OS2faktorErrorForm : IAdapterPresentationForm
    {
        public string GetFormHtml(int lcid)
        {
            return Resources.OS2faktorErrorForm;
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
