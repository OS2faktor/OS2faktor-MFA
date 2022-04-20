using Newtonsoft.Json;
using RestSharp;
using RestSharp.Authenticators;
using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Runtime.CompilerServices;
using System.Runtime.Serialization.Json;
using System.Text;

namespace OS2faktorPlugin
{
    class IdentifyService
    {
        private static log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private static DateTime accessTokenExpires = DateTime.MinValue;
        private static string accessToken;

        public static User GetUser(string url, string sAMAccountName, string stsUrl, string clientId, string clientSecret, string username, string password)
        {
            log.Info("Calling IdentifyService for: " + sAMAccountName);

            string token = GetAccessToken(stsUrl, clientId, clientSecret, username, password);
            if (token == null)
            {
                return null;
            }

            try
            {
                using (HttpClient client = new HttpClient())
                {
                    var builder = new UriBuilder(url);
                    builder.Query = "filter=username eq \"" + sAMAccountName + "\" and active eq \"true\"&sortOrder=Ascending&attributes=urn:scim:schemas:extension:safewhere:identify:1.0";

                    using (var request = new HttpRequestMessage(HttpMethod.Get, builder.Uri))
                    {
                        request.Headers.Add("Accept", "application/json");
                        request.Headers.Add("Authorization", token);

                        var response = client.SendAsync(request).Result;

                        var jsonString = response.Content.ReadAsStringAsync();
                        jsonString.Wait();
                        var tmpStr = jsonString.Result;
                        var identifyResponse = JsonConvert.DeserializeObject<IdentifyResponse>(tmpStr);

                        string cpr = null;

                        foreach (IdentifyResource resource in identifyResponse.resources)
                        {
                            foreach (IdentifyClaim claim in resource.record.claims)
                            {
                                if ("dk:gov:saml:attribute:CprNumberIdentifier".Equals(claim.type))
                                {
                                    cpr = claim.value;
                                    break;
                                }
                            }
                        }

                        if (cpr == null)
                        {
                            log.Error("No dk:gov:saml:attribute:CprNumberIdentifier claim available on " + sAMAccountName);
                            return null;
                        }

                        string cpr2 = cpr.Replace("-", "");
                        cpr2 = cpr2.Replace(" ", "");

                        if (cpr2.Length != 10)
                        {
                            log.Error("Identify returned invalid cpr (" + cpr + ") for " + sAMAccountName);
                            return null;
                        }

                        return new User()
                        {
                            Cpr = cpr2
                        };
                    }
                }
            }
            catch (Exception ex)
            {
                log.Error("Failed to get information from identify", ex);
            }

            return null;
        }

        [MethodImpl(MethodImplOptions.Synchronized)]
        private static string GetAccessToken(string url, string clientId, string clientSecret, string username, string password)
        {
            try
            {
                // semi-smart caching
                if (!string.IsNullOrEmpty(accessToken) && DateTime.Compare(DateTime.Now, accessTokenExpires) < 0)
                {
                    return accessToken;
                }

                using (HttpClient client = new HttpClient())
                {
                    var values = new List<KeyValuePair<string, string>>();
                    values.Add(new KeyValuePair<string, string>("grant_type", "password"));
                    values.Add(new KeyValuePair<string, string>("scope", "identify*scim"));
                    values.Add(new KeyValuePair<string, string>("client_id", clientId));
                    values.Add(new KeyValuePair<string, string>("client_secret", clientSecret));
                    values.Add(new KeyValuePair<string, string>("username", username));
                    values.Add(new KeyValuePair<string, string>("password", password));

                    using (var content = new FormUrlEncodedContent(values))
                    {
                        content.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue("application/x-www-form-urlencoded");

                        var response = client.PostAsync(url, content).Result;

                        var jsonString = response.Content.ReadAsStringAsync();
                        jsonString.Wait();
                        var tokenResponse = JsonConvert.DeserializeObject<IdentifyTokenResponse>(jsonString.Result);

                        accessTokenExpires = DateTime.Now.AddSeconds(tokenResponse.expires_in - (10 * 60));
                        accessToken = tokenResponse.access_token;
                    }
                }

                return accessToken;
            }
            catch (Exception ex)
            {
                log.Error("Failed to get access token", ex);
            }

            return null;
        }
    }
}
