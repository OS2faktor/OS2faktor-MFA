using System.Net;
using System.Collections.Generic;
using System.Security.Claims;
using Microsoft.IdentityServer.Web.Authentication.External;
using Microsoft.Win32;
using System.Security.Cryptography;
using System;
using System.Globalization;
using System.IO;
using System.IO.Compression;
using System.Xml.Linq;

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
        private const string REGKEY_SORT_BY_ACTIVE = "SortByActive";
        private const string REGKEY_USE_IFRAME = "UseIFrame";
        private const string REGKEY_HMAC_KEY = "HmacKey";
        private const string REGKEY_REMEMBER_DEVICE_ALLOWED = "RememberDeviceAllowed";
        private const string REGKEY_REMEMBER_DEVICE_DAYS = "RememberDeviceDays";
        private const string REGKEY_ALLOWED_RELYING_PARTIES = "RememberDeviceRelyingParties";
        private const string REGKEY_OS2FAKTOR_LOGIN_BASEURL = "OS2faktorLoginBaseUrl";

        private const string REGKEY_IDENTIFY_SERVICE_URL = "IdentifyServiceUrl";
        private const string REGKEY_IDENTIFY_STS_URL = "IdentifyStsUrl";
        private const string REGKEY_IDENTIFY_CLIENT_ID = "IdentifyClientId";
        private const string REGKEY_IDENTIFY_CLIENT_SECRET = "IdentifyClientSecret";
        private const string REGKEY_IDENTIFY_USERNAME = "IdentifyUsername";
        private const string REGKEY_IDENTIFY_PASSWORD = "IdentifyPassword";

        private string url;
        private string getDeviceUrl;
        private string deviceIdField;
        private string pidField;
        private string pseudonymField;
        private string cprField;
        private bool allowSelfRegistration;
        private bool sortByActive;
        private string cprWebserviceUrl;
        private string connectionString;
        private string sqlStatement;
        private string hmacKey;
        private bool rememberDeviceAllowed;
        private bool useIframe;
        private string rememberDeviceDays;
        private List<string> allowedRelyingParties;
        private ServiceStub serviceStub;
        private ADConnector adConnector;
        private bool useOS2faktorLoginPassthrough = false;

        // settings for Identify lookup
        private string identifyServiceUrl, identifyStsUrl, identifyClientId, identifyClientSecret, identifyUsername, identifyPassword;

        public IAuthenticationAdapterMetadata Metadata
        {
            get { return new OS2faktorMetadata(); }
        }

        public IAdapterPresentation BeginAuthentication(Claim identityClaim, HttpListenerRequest request, IAuthenticationContext authContext)
        {
            string upn = ExtractAndStoreUpn(identityClaim.Value, authContext);

            bool hasValidatedRelyingParties = false;
            if (rememberDeviceAllowed)
            {
                hasValidatedRelyingParties = CheckRelyingParties(request, authContext);
                if (hasValidatedRelyingParties)
                {
                    return new OS2faktorCheckRememberDeviceForm();
                }
            }

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

                return new OS2faktorLoginForm(challenge, redirectUrl, pollingUrl, chromeClient, false, (rememberDeviceAllowed && hasValidatedRelyingParties), useIframe);
            }

            return new OS2faktorPickDeviceForm(clients, sortByActive, false);
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
            string sortByActiveStr = (string)key.GetValue(REGKEY_SORT_BY_ACTIVE, "false");
            sortByActive = "true".Equals(sortByActiveStr);
            string useIframeStr = (string)key.GetValue(REGKEY_USE_IFRAME, "false");
            useIframe = "true".Equals(useIframeStr);
            string connectorVersion = (string)key.GetValue(REGKEY_CONNECTORVERSION);
            string requirePinString = (string)key.GetValue(REGKEY_REQUIREPIN, "false");
            bool requirePin = "true".Equals(requirePinString);
            cprWebserviceUrl = (string)key.GetValue(REGKEY_CPR_WEBSERVICE, "");
            connectionString = (string)key.GetValue(REGKEY_CPR_SQL_CONNECTIONSTRING, "");
            sqlStatement = (string)key.GetValue(REGKEY_CPR_SQL_STATEMENT, "");
            string rememberDeviceAllowedStr = (string)key.GetValue(REGKEY_REMEMBER_DEVICE_ALLOWED, "false");
            rememberDeviceAllowed = "true".Equals(rememberDeviceAllowedStr);
            rememberDeviceDays = (string)key.GetValue(REGKEY_REMEMBER_DEVICE_DAYS, "");
            hmacKey = (string)key.GetValue(REGKEY_HMAC_KEY, "");
            var registryValue = key.GetValue(REGKEY_ALLOWED_RELYING_PARTIES, new string[] { });
            allowedRelyingParties = new List<string>(registryValue as string[]);

            string adUrl = (string)key.GetValue(REGKEY_ADURL, "");
            string adUsername = (string)key.GetValue(REGKEY_ADUSERNAME, "");
            string adPassword = (string)key.GetValue(REGKEY_ADPASSWORD, "");
            adConnector = new ADConnector(adUrl, adUsername, adPassword);

            string os2faktorLoginBaseUrl = (string)key.GetValue(REGKEY_OS2FAKTOR_LOGIN_BASEURL, "");
            if (!string.IsNullOrEmpty(os2faktorLoginBaseUrl))
            {
                useOS2faktorLoginPassthrough = true;
            }

            serviceStub = new ServiceStub(url, apiKey, connectorVersion, requirePin, os2faktorLoginBaseUrl);

            string trustAll = (string)key.GetValue(REGKEY_TRUSTALL, "false");
            if ("true".Equals(trustAll))
            {
                ServicePointManager.ServerCertificateValidationCallback += (sender, certificate, chain, sslPolicyErrors) => true;
            }

            // identify settings
            identifyServiceUrl = (string)key.GetValue(REGKEY_IDENTIFY_SERVICE_URL, "");
            identifyStsUrl = (string)key.GetValue(REGKEY_IDENTIFY_STS_URL, "");
            identifyClientId = (string)key.GetValue(REGKEY_IDENTIFY_CLIENT_ID, "");
            identifyClientSecret = (string)key.GetValue(REGKEY_IDENTIFY_CLIENT_SECRET, "");
            identifyUsername = (string)key.GetValue(REGKEY_IDENTIFY_USERNAME, "");
            identifyPassword = (string)key.GetValue(REGKEY_IDENTIFY_PASSWORD, "");

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

            // deal with "rememberMe" logic
            bool relyingPartyAllowsRememberMe = (rememberDeviceAllowed && DoesRelyingPartySupportRememberMe(authContext));
            if (relyingPartyAllowsRememberMe)
            {
                // deal with repost (when "rememberMe" is checked, we send the rememberMeToken to the frontend for saving, and auto-post back, this
                // deals with that case, and just sends the user straight through without any further validation)
                if (IsRememberDevice(authContext))
                {
                    // authn complete - return authn method
                    outgoingClaims = new[]
                    {
                         // Return the required authentication method claim, indicating the particulate authentication method used.
                         new Claim("http://schemas.microsoft.com/ws/2008/06/identity/claims/authenticationmethod", OS2faktorMetadata.AUTH_METHOD)
                    };

                    return null;
                }

                // a "rememberMe" token is supplied, and we need to validate if it is ok, or send the user back to the ordinary login flow
                if (proofData?.Properties != null && proofData.Properties.ContainsKey("rememberMeToken"))
                {
                    string rememberMeToken = (string)proofData.Properties["rememberMeToken"];
                    var upn = GetUpn(authContext);

                    bool validToken = false;
                    if (!string.IsNullOrEmpty((string)proofData.Properties["rememberMeToken"]))
                    {
                        validToken = validateRememberMeToken(rememberDeviceDays, rememberMeToken, hmacKey, upn);
                    } 

                    if (validToken)
                    {
                        // authn complete - return authn method
                        outgoingClaims = new[]
                        {
                            // Return the required authentication method claim, indicating the particulate authentication method used.
                            new Claim("http://schemas.microsoft.com/ws/2008/06/identity/claims/authenticationmethod", OS2faktorMetadata.AUTH_METHOD)
                        };

                        log.Debug("User performed rememberMe login: " + upn);

                        return null;
                    }
                    else
                    {
                        // TODO: This code looks like something we could refactor into a helper method - it must be used elsewhere I think

                        log.Warn("The rememberMeToken could not be validated. Going back to the intended login page and deleting the rememberMeToken in local storage.");
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

                            return new OS2faktorLoginForm(challenge, redirectUrl, pollingUrl, chromeClient, true, relyingPartyAllowsRememberMe, useIframe);
                        }

                        return new OS2faktorPickDeviceForm(clients, sortByActive, true);
                    }
                }
            }

            // deal with the selfRegistration flow (TODO: this should be a candidate for removal in the future)
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

            // what client was chosen?
            if (deviceId == null && proofData?.Properties != null && proofData.Properties.ContainsKey("chosenClient"))
            {
                deviceId = (string)proofData.Properties["chosenClient"];
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

                return new OS2faktorLoginForm(challenge, redirectUrl, pollingUrl, chromeClient, false, relyingPartyAllowsRememberMe, useIframe);
            }

            // otherwise validate
            string subscriptionKey = GetSubscriptionKey(authContext);
            SubscriptionResponse info = serviceStub.GetSubscriptionInfo(subscriptionKey);
            if (info != null && info.clientAuthenticated == true)
            {
                if (allowSelfRegistration)
                {
                    string newDeviceId = GetNewDeviceId(authContext);
                    if (newDeviceId != null && newDeviceId.Length > 0)
                    {
                        adConnector.SetDeviceIdOnUser(GetUpn(authContext), deviceIdField, newDeviceId);
                    }
                }

                if (proofData?.Properties != null && proofData.Properties.ContainsKey("rememberDevice") && rememberDeviceAllowed)
                {
                    if ((string)proofData.Properties["rememberDevice"] == "on")
                    {
                        StoreRememberDevice(authContext);
                        string upn = GetUpn(authContext);
                        string today = DateTime.Now.ToString("yyyy-MM-dd");
                        string hmac = Encode(upn + ":" + today, hmacKey);

                        log.Debug("Sending rememberMe token to frontend for " + upn);

                        return new OS2faktorRememberDeviceForm(upn + ":" + today + ":" + hmac);
                    }
                }

                // authn complete - return authn method
                outgoingClaims = new[]
                {
                     // Return the required authentication method claim, indicating the particulate authentication method used.
                     new Claim("http://schemas.microsoft.com/ws/2008/06/identity/claims/authenticationmethod", OS2faktorMetadata.AUTH_METHOD)
                };

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
                subscriptionKey = (string)authContext.Data["subscription"];
            }

            return subscriptionKey;
        }

        private void StoreRememberDevice(IAuthenticationContext authContext)
        {
            if (authContext.Data.ContainsKey("rememberDevice"))
            {
                authContext.Data.Remove("rememberDevice");
            }

            authContext.Data.Add("rememberDevice", true);
        }

        private static bool IsRememberDevice(IAuthenticationContext authContext)
        {
            bool rememberDevice = false;

            if (authContext.Data.ContainsKey("rememberDevice"))
            {
                rememberDevice = (bool)authContext.Data["rememberDevice"];
            }

            return rememberDevice;
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

        public static string Encode(string message, string key)
        {
            byte[] rawKey = System.Text.Encoding.UTF8.GetBytes(key);
            byte[] rawMessage = System.Text.Encoding.UTF8.GetBytes(message);

            using (HMACSHA256 hmac = new HMACSHA256(rawKey))
            {
                byte[] result = hmac.ComputeHash(rawMessage);
                return Convert.ToBase64String(result);
            }
        }

        // set the context of the relying party with regards to supporting RememberMeTokens
        private void SetRelyingPartySupportsRememberMe(IAuthenticationContext authContext, bool validated)
        {
            if (authContext.Data.ContainsKey("validatedRelyingParties"))
            {
                authContext.Data.Remove("validatedRelyingParties");
            }

            authContext.Data.Add("validatedRelyingParties", validated);
        }

        // are we in the context of a RelyingParty that supports RememberMeTokens
        private static bool DoesRelyingPartySupportRememberMe(IAuthenticationContext authContext)
        {
            bool validatedRelyingParties = false;

            if (authContext.Data.ContainsKey("validatedRelyingParties"))
            {
                validatedRelyingParties = (bool)authContext.Data["validatedRelyingParties"];
            }

            return validatedRelyingParties;
        }

        // validate if a given RememberMeToken is still valid (signature, timestamp, etc)
        private static bool validateRememberMeToken(string rememberDeviceDays, string rememberMeToken, string hmacKey, string upn)
        {
            try
            {
                string[] tokenSplit = rememberMeToken.Split(':');
                string upnFromToken = tokenSplit[0];
                if (!upn.Equals(upnFromToken))
                {
                    return false;
                }

                string dateStringFromToken = tokenSplit[1];
                string hmac = Encode(upnFromToken + ":" + dateStringFromToken, hmacKey);
                string rememberMeTokenShouldBe = upnFromToken + ":" + dateStringFromToken + ":" + hmac;
                string[] dateSplit = dateStringFromToken.Split('-');

                int month = 1, day = 1, year = 1;
                Int32.TryParse(dateSplit[0], out year);
                Int32.TryParse(dateSplit[1], out month);
                Int32.TryParse(dateSplit[2], out day);

                int addDays = 0;
                Int32.TryParse(rememberDeviceDays, out addDays);

                DateTime dateFromToken = new DateTime(year, month, day, new GregorianCalendar());
                DateTime today = DateTime.Now;
                DateTime compareDate = dateFromToken.AddDays(addDays);

                if (rememberMeToken.Equals(rememberMeTokenShouldBe) && today < compareDate && (dateFromToken < today || dateFromToken == today))
                {
                    return true;
                }
            }
            catch (Exception ex)
            {
                log.Error("Failure during validation of rememberMeToken: " + rememberMeToken, ex);
            }

            return false;
        }

        // check if the current RelyingParty is on the list of allowedRelyingParties to use RememberMeTokens
        private bool CheckRelyingParties(HttpListenerRequest request, IAuthenticationContext authContext) 
        {
            try 
            {
                string samlRequest = request.QueryString.Get("SAMLRequest");

                if (!string.IsNullOrEmpty(samlRequest))
                {
                    byte[] data = Convert.FromBase64String(samlRequest);

                    using (var stream = new MemoryStream(data, 0, data.Length))
                    {
                        using (var inflater = new DeflateStream(stream, CompressionMode.Decompress))
                        {
                            using (var streamReader = new StreamReader(inflater))
                            {
                                string xml = streamReader.ReadToEnd();
                                XElement xElement = XElement.Parse(xml);

                                var nodes = xElement.Nodes();
                                foreach (XNode node in nodes)
                                {
                                    XElement nodeAsXElement = node as XElement;
                                    if (nodeAsXElement.Name.LocalName.Equals("Issuer"))
                                    {
                                        foreach (string party in allowedRelyingParties)
                                        {
                                            if (nodeAsXElement.Value.Equals(party))
                                            {
                                                SetRelyingPartySupportsRememberMe(authContext, true);

                                                log.Debug("Validated relying party : " + nodeAsXElement.Value);
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else
                {
                    string wsFedRealm = request.QueryString.Get("wtrealm");

                    if (!string.IsNullOrEmpty(wsFedRealm))
                    {
                        foreach (string party in allowedRelyingParties)
                        {
                            if (wsFedRealm.Equals(party))
                            {
                                SetRelyingPartySupportsRememberMe(authContext, true);

                                log.Debug("Validated relying party : " + wsFedRealm);

                                return true;
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                log.Warn("Validation of Relying Parties failed.", ex);
            }

            return false;
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

            if (user == null && !string.IsNullOrEmpty(identifyServiceUrl))
            {
                log.Debug("Reading from IdentifyService");
                user = IdentifyService.GetUser(identifyServiceUrl, upn, identifyStsUrl, identifyClientId, identifyClientSecret, identifyUsername, identifyPassword);
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
            if (useOS2faktorLoginPassthrough)
            {
                var result = serviceStub.GetClientsFromOS2faktorLogin(user.Cpr);
                if (result != null)
                {
                    clients = result;
                }
            }

            // even if the above call is enabled, if it fails, we try to call directly as a fallback mechanism
            if (clients.Count == 0)
            {
                if (user.Cpr != null || user.DeviceIds != null || user.Pseudonym != null || user.Pid != null)
                {
                    var result = serviceStub.GetClients(user.Cpr, user.Pid, user.Pseudonym, user.DeviceIds, sortByActive);
                    if (result != null)
                    {
                        clients = result;
                    }
                }
                else
                {
                    log.Warn("Could not find any relevant information on user: " + upn);
                }
            }

            return clients;
        }
    }
}
