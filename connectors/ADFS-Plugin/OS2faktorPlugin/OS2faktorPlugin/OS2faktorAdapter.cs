using System.Net;
using System.Collections.Generic;
using System.Security.Claims;
using Microsoft.IdentityServer.Web.Authentication.External;
using Microsoft.Win32;

namespace OS2faktorPlugin
{
    public class OS2faktorAdapter : IAuthenticationAdapter
    {
        private static log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private const string REGISTRY_LOCATION = @"SOFTWARE\Wow6432node\Digital Identity\OS2faktor";
        private const string REGKEY_APIKEY = "ApiKey";
        private const string REGKEY_CPR = "CprField";
        private const string REGKEY_PID = "PidField";
        private const string REGKEY_PSEUDONYM = "PseudonymField";
        private const string REGKEY_URL = "OS2faktorUrl";
        private const string REGKEY_DEBUG = "Debug";
        private const string REGKEY_DEVICEID = "DeviceIdField";
        private const string REGKEY_TRUSTALL = "TrustAllSSLCerts";
        private const string REGKEY_GETDEVICEURL = "GetDeviceUrl";
        private const string REGKEY_ALLOWSELFREG = "AllowSelfRegistration";
        private const string REGKEY_CONNECTORVERSION = "ConnectorVersion";
        private const string REGKEY_REQUIREPIN = "RequirePin";
        private const string REGKEY_ADURL = "ADUrl";
        private const string REGKEY_ADUSERNAME = "ADUsername";
        private const string REGKEY_ADPASSWORD = "ADPassword";
        private const string REGKEY_CPR_WEBSERVICE = "CprWebservice";
        private const string REGKEY_CPR_SQL_CONNECTIONSTRING = "ConnectionString";
        private const string REGKEY_CPR_SQL_STATEMENT = "SQL";

        private string url;
        private string getDeviceUrl;
        private string deviceIdField;
        private string pidField;
        private string pseudonymField;
        private string cprField;
        private bool allowSelfRegistration;
        private string cprWebserviceUrl;
        private string connectionString;
        private string sqlStatement;
        private ServiceStub serviceStub;
        private ADConnector adConnector;

        public IAuthenticationAdapterMetadata Metadata
        {
            get { return new OS2faktorMetadata(); }
        }

        public IAdapterPresentation BeginAuthentication(Claim identityClaim, HttpListenerRequest request, IAuthenticationContext authContext)
        {
            string upn = ExtractAndStoreUpn(identityClaim.Value, authContext);

            var clients = GetClients(upn);
            if (clients == null || clients.Count == 0)
            {
                log.Info(upn + " did not have any OS2faktor clients registered");

                return new OS2faktorNoDevicesForm(getDeviceUrl, allowSelfRegistration);
            }

            if (clients.Count == 1)
            {
                var client = clients[0];

                SubscriptionResponse response = serviceStub.CreateSubscription(client.deviceId);
                if (response == null)
                {
                    log.Warn("Failed to create subscription for " + upn);

                    return new OS2faktorErrorForm();
                }

                StoreSubscriptionKey(response.subscriptionKey, authContext);

                string challenge = response.challenge;
                string redirectUrl = (string.IsNullOrEmpty(response.redirectUrl)) ? "" : response.redirectUrl;
                string pollingUrl = url + (url.EndsWith("/") ? "" : "/") + "api/notification/" + response.pollingKey + "/poll";

                string chromeClient = "false";
                if ("CHROME".Equals(client.type))
                {
                    chromeClient = "true";
                }

                return new OS2faktorLoginForm(challenge, redirectUrl, pollingUrl, chromeClient);
            }

            return new OS2faktorPickDeviceForm(clients);
        }

        public bool IsAvailableForUser(Claim identityClaim, IAuthenticationContext authContext)
        {
            return true;
        }

        public void OnAuthenticationPipelineLoad(IAuthenticationMethodConfigData configData)
        {
            var key = Registry.LocalMachine.OpenSubKey(REGISTRY_LOCATION);

            bool debug = false;
            try
            {
                string debugStr = (string)key.GetValue(REGKEY_DEBUG, "false");
                debug = "true".Equals(debugStr);
            }
            catch (System.Exception)
            {
                ; // cannot do much about that
            }

            Initializer.init(debug);

            log.Info("Initializing OS2faktor");


            string apiKey = (string)key.GetValue(REGKEY_APIKEY, "NOT_SET");
            url = (string)key.GetValue(REGKEY_URL, "https://backend.os2faktor.dk");
            getDeviceUrl = (string)key.GetValue(REGKEY_GETDEVICEURL, "https://www.os2faktor.dk/");
            deviceIdField = (string)key.GetValue(REGKEY_DEVICEID, "");
            cprField = (string)key.GetValue(REGKEY_CPR, "");
            pidField = (string)key.GetValue(REGKEY_PID, "");
            pseudonymField = (string)key.GetValue(REGKEY_PSEUDONYM, "");
            string allowSelfRegStr = (string)key.GetValue(REGKEY_ALLOWSELFREG, "false");
            allowSelfRegistration = "true".Equals(allowSelfRegStr);
            string connectorVersion = (string)key.GetValue(REGKEY_CONNECTORVERSION);
            string requirePinString = (string)key.GetValue(REGKEY_REQUIREPIN, "false");
            bool requirePin = "true".Equals(requirePinString);
            cprWebserviceUrl = (string)key.GetValue(REGKEY_CPR_WEBSERVICE, "");
            connectionString = (string)key.GetValue(REGKEY_CPR_SQL_CONNECTIONSTRING, "");
            sqlStatement = (string)key.GetValue(REGKEY_CPR_SQL_STATEMENT, "");

            string adUrl = (string)key.GetValue(REGKEY_ADURL, "");
            string adUsername = (string)key.GetValue(REGKEY_ADUSERNAME, "");
            string adPassword = (string)key.GetValue(REGKEY_ADPASSWORD, "");
            adConnector = new ADConnector(adUrl, adUsername, adPassword);

            serviceStub = new ServiceStub(url, apiKey, connectorVersion, requirePin);

            string trustAll = (string)key.GetValue(REGKEY_TRUSTALL, "false");
            if ("true".Equals(trustAll))
            {
                ServicePointManager.ServerCertificateValidationCallback += (sender, certificate, chain, sslPolicyErrors) => true;
            }

            log.Info("pidField: " + pidField + ", pseudonymField: " + pseudonymField + ", deviceIdField: " + deviceIdField + ", cprField: " + cprField + ", url: " + url + ", allowSelfRegistration: " + allowSelfRegistration + ", requirePin: " + requirePin + " usingSQL: " + ((string.IsNullOrEmpty(connectionString) ? "false" : "true")));
        }

        public void OnAuthenticationPipelineUnload()
        {
            // destructor
        }

        public IAdapterPresentation OnError(HttpListenerRequest request, ExternalAuthenticationException ex)
        {
            return new OS2faktorErrorForm();
        }

        public IAdapterPresentation TryEndAuthentication(IAuthenticationContext authContext, IProofData proofData, HttpListenerRequest request, out Claim[] outgoingClaims)
        {
            outgoingClaims = new Claim[0];

            string deviceId = null;
            if (allowSelfRegistration && proofData?.Properties != null && proofData.Properties.ContainsKey("val1"))
            {
                deviceId = proofData.Properties["val1"] + "-";
                deviceId += ((proofData.Properties.ContainsKey("val2")) ? proofData.Properties["val2"] : "") + "-";
                deviceId += ((proofData.Properties.ContainsKey("val3")) ? proofData.Properties["val3"] : "") + "-";
                deviceId += ((proofData.Properties.ContainsKey("val4")) ? proofData.Properties["val4"] : "");

                if (deviceId.Length != 15)
                {
                    log.Warn("Invalid clientUuid supplied: '" + deviceId + "'");

                    return new OS2faktorNoDevicesForm(getDeviceUrl, allowSelfRegistration, true);
                }
            }

            if (deviceId == null && proofData?.Properties != null && proofData.Properties.ContainsKey("chosenClient"))
            {
                deviceId = (string) proofData.Properties["chosenClient"];
            }

            // if the user picked a client, generate challenge and display next page
            if (deviceId != null && deviceId.Length > 0)
            {
                // validate that the user is allowed to use this device
                string upn = GetUpn(authContext);
                var clients = GetClients(upn); // TODO: perhaps store the list of clients in the context so we do not call backend twice

                ClientDTO currentClient = null;
                if (clients == null)
                {
                    log.Warn("User does not exist when attempting to register new device: " + upn);

                    return new OS2faktorErrorForm();
                }
                else if (clients.Count == 0)
                {
                    if (!allowSelfRegistration)
                    {
                        log.Warn("User tried to register a device, but registration is not allowed: " + upn);

                        return new OS2faktorErrorForm();
                    }

                    StoreNewDeviceId(deviceId, authContext);
                }
                else
                {
                    bool found = false;

                    foreach (var client in clients)
                    {
                        if (client.deviceId.Equals(deviceId))
                        {
                            currentClient = client;
                            found = true;
                            break;
                        }
                    }

                    if (!found)
                    {
                        log.Warn("User tried to use a device not registered to this user: upn=" + upn + ", deviceId=" + deviceId);

                        return new OS2faktorErrorForm();
                    }
                }

                SubscriptionResponse response = serviceStub.CreateSubscription(deviceId);
                if (response == null)
                {
                    log.Warn("Failed to create subscription for client " + deviceId);

                    // TODO: if the client does not exist, perhaps we should go back to the first login page and try again?
                    return new OS2faktorErrorForm();
                }

                StoreSubscriptionKey(response.subscriptionKey, authContext);

                string challenge = response.challenge;
                string redirectUrl = (string.IsNullOrEmpty(response.redirectUrl)) ? "" : response.redirectUrl;
                string pollingUrl = url + (url.EndsWith("/") ? "" : "/") + "api/notification/" + response.pollingKey + "/poll";

                string chromeClient = "false";
                if (currentClient != null && "CHROME".Equals(currentClient.type))
                {
                    chromeClient = "true";
                }

                return new OS2faktorLoginForm(challenge, redirectUrl, pollingUrl, chromeClient);
            }

            // otherwise validate
            string subscriptionKey = GetSubscriptionKey(authContext);
            SubscriptionResponse info = serviceStub.GetSubscriptionInfo(subscriptionKey);
            if (info != null && info.clientAuthenticated == true)
            {
                // authn complete - return authn method
                outgoingClaims = new[]
                {
                     // Return the required authentication method claim, indicating the particulate authentication method used.
                     new Claim("http://schemas.microsoft.com/ws/2008/06/identity/claims/authenticationmethod", OS2faktorMetadata.AUTH_METHOD)
                };

                if (allowSelfRegistration)
                {
                    string newDeviceId = GetNewDeviceId(authContext);
                    if (newDeviceId != null && newDeviceId.Length > 0)
                    {
                        adConnector.SetDeviceIdOnUser(GetUpn(authContext), deviceIdField, newDeviceId);
                    }
                }

                return null;
            }
            else if (info != null && info.clientRejected == true)
            {
                return new OS2faktorRejectedForm();
            }
            else
            {
                log.Warn("Failed to fetch subscription " + subscriptionKey);

                return new OS2faktorErrorForm();
            }
        }

        private string ExtractAndStoreUpn(string value, IAuthenticationContext authContext)
        {
            string upn = value;

            // deal with sAMAccountName@DOMAIN
            int index = value.IndexOf("@");
            if (index > 0)
            {
                upn = value.Substring(0, index);
            }

            // deal with DOMAIN\sAMAccountName
            index = upn.IndexOf("\\");
            if (index > 0)
            {
                upn = upn.Substring(index + 1);
            }

            if (authContext.Data.ContainsKey("upn"))
            {
                authContext.Data.Remove("upn");
            }

            authContext.Data.Add("upn", upn);

            return upn;
        }

        private static string GetUpn(IAuthenticationContext authContext)
        {
            string upn = "";

            if (authContext.Data.ContainsKey("upn"))
            {
                upn = (string)authContext.Data["upn"];
            }

            return upn;
        }

        private void StoreSubscriptionKey(string subscriptionKey, IAuthenticationContext authContext)
        {
            if (authContext.Data.ContainsKey("subscription"))
            {
                authContext.Data.Remove("subscription");
            }

            authContext.Data.Add("subscription", subscriptionKey);
        }

        private static string GetSubscriptionKey(IAuthenticationContext authContext)
        {
            string subscriptionKey = null;

            if (authContext.Data.ContainsKey("subscription"))
            {
                subscriptionKey = (string) authContext.Data["subscription"];
            }

            return subscriptionKey;
        }

        private void StoreNewDeviceId(string deviceId, IAuthenticationContext authContext)
        {
            if (authContext.Data.ContainsKey("newDeviceId"))
            {
                authContext.Data.Remove("newDeviceId");
            }

            authContext.Data.Add("newDeviceId", deviceId);
        }

        private static string GetNewDeviceId(IAuthenticationContext authContext)
        {
            string deviceId = null;

            if (authContext.Data.ContainsKey("newDeviceId"))
            {
                deviceId = (string)authContext.Data["newDeviceId"];
            }

            return deviceId;
        }

        // return null if the user does not exist, and an empty list of the user does not have any clients
        private List<ClientDTO> GetClients(string upn)
        {
            User user = null;

            if (!string.IsNullOrEmpty(cprWebserviceUrl))
            {
                log.Debug("Reading from webservice");
                user = CprService.GetUser(cprWebserviceUrl, upn);
            }

            if (user == null && !string.IsNullOrEmpty(connectionString) && !string.IsNullOrEmpty(sqlStatement))
            {
                log.Debug("Reading from SQL");
                user = Dao.GetUser(connectionString, sqlStatement, upn);
            }

            if (user == null)
            {
                log.Debug("Reading from AD");
                user = adConnector.GetUser(upn, cprField, pidField, pseudonymField, deviceIdField);
            }

            if (user == null)
            {
                log.Error("Could not find user anywhere: " + upn);

                return null;
            }

            List<ClientDTO> clients = new List<ClientDTO>();
            if (user.Cpr != null || user.DeviceIds != null || user.Pseudonym != null || user.Pid != null)
            {
                var result = serviceStub.GetClients(user.Cpr, user.Pid, user.Pseudonym, user.DeviceIds);
                if (result != null)
                {
                    clients = result;
                }
            }
            else
            {
                log.Warn("Could not find any relevant information on user: " + upn);
            }

            return clients;
        }
    }
}
