using RestSharp;
using RestSharp.Authenticators;
using System;
using System.IO;
using System.Net;
using System.Runtime.Serialization.Json;
using System.Text;

namespace OS2faktorPlugin
{
    class CprService
    {
        private static log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        public static User GetUser(string url, string sAMAccountName)
        {
            using (WebClient client = new WebClient())
            {
                client.Credentials = CredentialCache.DefaultCredentials;

                try
                {
                    url = url.Replace("{sAMAccountName}", sAMAccountName);
                    log.Debug("Calling CPRService: " + url);

                    string content = client.DownloadString(url);

                    var serializer = new DataContractJsonSerializer(typeof(CprServiceResponse));
                    using (var ms = new MemoryStream(Encoding.Unicode.GetBytes(content)))
                    {
                        CprServiceResponse response = (CprServiceResponse)serializer.ReadObject(ms);

                        string cpr = response.result;
                        cpr = cpr.Replace("-", "");
                        cpr = cpr.Replace(" ", "");

                        if (cpr.Length != 10)
                        {
                            log.Error("CprService returned invalid cpr (" + response.result + ") for " + sAMAccountName);
                            return null;
                        }

                        return new User()
                        {
                            Cpr = cpr
                        };
                    }

                }
                catch (Exception ex)
                {
                    log.Error("Failed to connect", ex);
                }
            }

            return null;



            /*
            Uri uri = new Uri(url.Replace("{sAMAccountName}", sAMAccountName));
            RestClient client = GetClient(uri.Scheme + "://" + uri.Host);

            var request = new RestRequest(uri.PathAndQuery);
            request.Method = Method.GET;

            IRestResponse<CprServiceResponse> response = null;
            try
            {
                response = client.Execute<CprServiceResponse>(request);
            }
            catch (Exception ex)
            {
                log.Error("Failed to connect to CprService for " + sAMAccountName, ex);
                return null;
            }

            if (response == null)
            {
                log.Error("Got <null> response from CprService for " + sAMAccountName);
                return null;
            }
            else if (!response.StatusCode.Equals(System.Net.HttpStatusCode.OK))
            {
                log.Error("CprService responded with " + response.StatusCode + " for " + sAMAccountName);
                return null;
            }
            else if (response.Data == null || string.IsNullOrEmpty(response.Data.result))
            {
                log.Error("CprService responded with null or empty cpr number for: " + sAMAccountName);

                return null;
            }

            string cpr = response.Data.result;
            cpr.Replace("-", "");
            cpr.Replace(" ", "");

            if (cpr.Length != 10)
            {
                log.Error("CprService returned invalid cpr for " + sAMAccountName);
            }

            return new User()
            {
                Cpr = cpr
            };
            */
        }

        private static RestClient GetClient(string url)
        {
            RestClient client = new RestClient(url);
            client.Authenticator = new NtlmAuthenticator();

            return client;
        }
    }
}
