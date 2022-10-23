using Microsoft.IdentityServer.Web.Authentication.External;
using System.Collections.Generic;

namespace OS2faktorPlugin
{
    class OS2faktorPickDeviceForm : IAdapterPresentationForm
    {
        private List<ClientDTO> availableClients = null;
        private bool showActive = false;
        private bool setLocalStorageNull;

        public OS2faktorPickDeviceForm(List<ClientDTO> availableClients, bool showActive, bool setLocalStorageNull)
        {
            this.availableClients = availableClients;
            this.showActive = showActive;
            this.setLocalStorageNull = setLocalStorageNull;
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
                    else if ("EDGE".Equals(client.type))
                    {
                        cssClass = "fa-edge";
                    }
                    else if ("TOTP".Equals(client.type))
                    {
                        cssClass = "fa-mobile";
                    }

                    clientArray += "{ \"name\" : \"" + client.name + "\", \"deviceId\" : \"" + client.deviceId + "\", \"cssClass\" : \"" + cssClass + "\" }";
                    first = false;
                }
            }
            clientArray += "]";

            htmlTemplate = htmlTemplate.Replace("@@SETLOCALSTORAGENULL@@", this.setLocalStorageNull ? "true" : "false");
            return htmlTemplate.Replace("@@CLIENTARRAY@@", clientArray).Replace("@@SHOWACTIVE@@", ((showActive == true) ? "true" : "false"));
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
