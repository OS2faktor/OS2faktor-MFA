using Microsoft.IdentityServer.Web.Authentication.External;
using System.Collections.Generic;

namespace OS2faktorPlugin
{
    class OS2faktorPickDeviceForm : IAdapterPresentationForm
    {
        private List<ClientDTO> availableClients = null;

        public OS2faktorPickDeviceForm(List<ClientDTO> availableClients)
        {
            this.availableClients = availableClients;
        }

        public string GetFormHtml(int lcid)
        {
            string htmlTemplate = Resources.OS2faktorPickDeviceForm;

            string clientArray = "[";
            if (availableClients != null && availableClients.Count > 0)
            {
                bool first = true;
                foreach (var client in availableClients)
                {
                    if (!first)
                    {
                        clientArray += ",";
                    }

                    string cssClass = "fa-windows";
                    if ("IOS".Equals(client.type))
                    {
                        cssClass = "fa-apple";
                    }
                    else if ("ANDROID".Equals(client.type))
                    {
                        cssClass = "fa-android";
                    }
                    else if ("YUBIKEY".Equals(client.type))
                    {
                        cssClass = "fa-key";
                    }
                    else if ("CHROME".Equals(client.type))
                    {
                        cssClass = "fa-chrome";
                    }

                    clientArray += "{ \"name\" : \"" + client.name + "\", \"deviceId\" : \"" + client.deviceId + "\", \"cssClass\" : \"" + cssClass + "\" }";
                    first = false;
                }
            }
            clientArray += "]";

            return htmlTemplate.Replace("@@CLIENTARRAY@@", clientArray);
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
