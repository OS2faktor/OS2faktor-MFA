using Newtonsoft.Json;
using System;
using System.Net.Http;

namespace OS2faktorPlugin
{
    class OS2faktorService
    {
        private static log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        public static User GetUser(string url, string sAMAccountName, string domain, string apiKey)
        {
            log.Info("Calling OS2faktorService for: " + sAMAccountName);

            try
            {
                using (HttpClient client = new HttpClient())
                {
                    var builder = new UriBuilder(url);
                    builder.Query = "userId=" + sAMAccountName + "&domain=" + domain;

                    using (var request = new HttpRequestMessage(HttpMethod.Get, builder.Uri.AbsoluteUri))
                    {
                        request.Headers.Add("Accept", "application/json");
                        request.Headers.Add("ApiKey", apiKey);

                        var response = client.SendAsync(request).Result;

                        response.EnsureSuccessStatusCode();

                        var jsonString = response.Content.ReadAsStringAsync();
                        jsonString.Wait();

                        var tmpStr = jsonString.Result;
                        var os2faktorResponse = JsonConvert.DeserializeObject<OS2faktorResponse>(tmpStr);

                        string cpr = os2faktorResponse.cpr;
                        if (cpr == null)
                        {
                            log.Error("No cpr returned from OS2faktor on lookup on " + sAMAccountName);
                            return null;
                        }

                        return new User()
                        {
                            Cpr = cpr
                        };
                    }
                }
            }
            catch (Exception ex)
            {
                log.Error("Failed to get information from OS2faktor", ex);
            }

            return null;
        }
    }
}
