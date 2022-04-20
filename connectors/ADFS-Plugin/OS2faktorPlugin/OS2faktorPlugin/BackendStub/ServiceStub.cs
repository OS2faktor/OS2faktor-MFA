using System;
using System.Text;
using System.Security.Cryptography;
using System.Collections.Generic;
using System.Diagnostics;
using RestSharp;
using Newtonsoft.Json;
using System.Linq;
using System.Net;

namespace OS2faktorPlugin
{
    public class ServiceStub
    {
        private static log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private RestClient client, os2faktorLoginClient;
        private string apiKey;
        private string connectorVersion;
        private bool requirePin;

        public ServiceStub(string baseUrl, string apiKey, string connectorVersion, bool requirePin, string os2faktorLoginBaseUrl)
        {
            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;

            client = new RestClient();

            if (baseUrl.EndsWith("/"))
            {
                baseUrl = baseUrl.Substring(0, baseUrl.Length - 1);
            }

            client.BaseUrl = new Uri(baseUrl);

            this.connectorVersion = connectorVersion;
            this.apiKey = apiKey;
            this.requirePin = requirePin;

            if (!string.IsNullOrEmpty(os2faktorLoginBaseUrl))
            {
                os2faktorLoginClient = new RestClient();

                if (os2faktorLoginBaseUrl.EndsWith("/"))
                {
                    os2faktorLoginBaseUrl = os2faktorLoginBaseUrl.Substring(0, os2faktorLoginBaseUrl.Length - 1);
                }

                os2faktorLoginClient.BaseUrl = new Uri(os2faktorLoginBaseUrl);
            }
        }

        // only drawback is that there is no sorting on activity, only name
        public List<ClientDTO> GetClientsFromOS2faktorLogin(string cpr)
        {
            if (os2faktorLoginClient == null)
            {
                log.Error("No OS2faktorLogin URL configured!");
                return new List<ClientDTO>();
            }

            string path = "/api/mfa/clients";

            var request = new RestRequest(path);
            request.AddHeader("ApiKey", apiKey);
            request.AddQueryParameter("cpr", cpr);

            IRestResponse<List<ClientDTO>> response = null;
            var stopWatch = new Stopwatch();
            try
            {
                stopWatch.Start();
                response = os2faktorLoginClient.Execute<List<ClientDTO>>(request);
            }
            catch (Exception ex)
            {
                log.Error("Failed to connect", ex);
            }
            finally
            {
                if (log.IsDebugEnabled)
                {
                    log.Debug(LogRequest(request, response, stopWatch.ElapsedMilliseconds));
                }
            }

            if (response == null || !response.StatusCode.Equals(System.Net.HttpStatusCode.OK))
            {
                return null;
            }

            List<ClientDTO> clients = response.Data;

            if (requirePin)
            {
                clients.RemoveAll(c => !c.hasPincode);
            }

            // sort by name
            clients.Sort(new ClientComparer());

            if (log.IsDebugEnabled)
            {
                int i = 1;
                foreach (var client in clients)
                {
                    log.Debug((i++) + " : " + client.name);
                }
            }

            return clients;
        }

        public List<ClientDTO> GetClients(string cpr, string pid, string pseudonym, string[] deviceIds, bool sortByActive)
        {
            string path = "/api/server/clients";

            var request = new RestRequest(path);
            request.AddHeader("ApiKey", apiKey);
            request.AddHeader("ConnectorVersion", connectorVersion);

            if (!string.IsNullOrEmpty(cpr))
            {
                request.AddQueryParameter("ssn", EncodeSSN(cpr));
            }
            else if (!string.IsNullOrEmpty(pid))
            {
                request.AddQueryParameter("pid", pid);
            }
            else if (!string.IsNullOrEmpty(pseudonym))
            {
                request.AddQueryParameter("pseudonym", pseudonym);
            }

            if (deviceIds != null && deviceIds.Length > 0)
            {
                foreach (string deviceId in deviceIds)
                {
                    request.AddQueryParameter("deviceId", deviceId);
                }
            }

            IRestResponse<List<ClientDTO>> response = null;
            var stopWatch = new Stopwatch();
            try
            {
                stopWatch.Start();
                response = client.Execute<List<ClientDTO>>(request);
            }
            catch (Exception ex)
            {
                log.Error("Failed to connect", ex);
            }
            finally
            {
                if (log.IsDebugEnabled)
                {
                    log.Debug(LogRequest(request, response, stopWatch.ElapsedMilliseconds));
                }
            }

            if (response == null || !response.StatusCode.Equals(System.Net.HttpStatusCode.OK))
            {
                return null;
            }

            List<ClientDTO> clients = response.Data;

            if (requirePin)
            {
                clients.RemoveAll(c => !c.hasPincode);
            }

            if (sortByActive)
            {
                // sort, pick prime first, then sort by lastUsed
                clients.Sort(new ClientComparerLastUsed());
            }
            else
            {
                // sort, pick prime first, then sort by name
                clients.Sort(new ClientComparer());
            }

            if (log.IsDebugEnabled)
            {
                int i = 1;
                foreach (var client in clients)
                {
                    log.Debug((i++) + " : " + client.name);
                }
            }

            return clients;
        }

        public SubscriptionResponse CreateSubscription(string uuid)
        {
            string path = "/api/server/client/" + uuid + "/authenticate";

            var request = new RestRequest(path);
            request.AddHeader("ApiKey", apiKey);
            request.Method = Method.PUT;

            IRestResponse<SubscriptionResponse> response = null;
            var stopWatch = new Stopwatch();
            try
            {
                stopWatch.Start();
                response = client.Execute<SubscriptionResponse>(request);
            }
            catch (Exception ex)
            {
                log.Error("Failed to connect", ex);
            }
            finally
            {
                if (log.IsDebugEnabled)
                {
                    log.Debug(LogRequest(request, response, stopWatch.ElapsedMilliseconds));
                }
            }

            if (response == null || !response.StatusCode.Equals(System.Net.HttpStatusCode.OK))
            {
                return null;
            }

            return response.Data;
        }

        public SubscriptionResponse GetSubscriptionInfo(string subscriptionKey)
        {
            string path = "/api/server/notification/" + subscriptionKey + "/status";

            var request = new RestRequest(path);
            request.AddHeader("ApiKey", apiKey);

            IRestResponse<SubscriptionResponse> response = null;
            var stopWatch = new Stopwatch();
            try
            {
                stopWatch.Start();
                response = client.Execute<SubscriptionResponse>(request);
            }
            catch (Exception ex)
            {
                log.Error("Failed to connect", ex);
            }
            finally
            {
                if (log.IsDebugEnabled)
                {
                    log.Debug(LogRequest(request, response, stopWatch.ElapsedMilliseconds));
                }
            }

            if (response == null || !response.StatusCode.Equals(System.Net.HttpStatusCode.OK))
            {
                return null;
            }

            return response.Data;
        }

        private string LogRequest(IRestRequest request, IRestResponse response, long durationMs)
        {
            var requestToLog = new
            {
                resource = request.Resource,

                parameters = request.Parameters.Select(parameter => new
                {
                    name = parameter.Name,
                    value = parameter.Value,
                    type = parameter.Type.ToString()
                }),

                method = request.Method.ToString(),
                uri = client.BuildUri(request),
            };

            var responseToLog = new
            {
                statusCode = response.StatusCode,
                content = response.Content,
                headers = response.Headers,
                responseUri = response.ResponseUri,
                errorMessage = response.ErrorMessage,
            };

            return string.Format("Request completed in {0} ms, Request: {1}, Response: {2}", durationMs, JsonConvert.SerializeObject(requestToLog), JsonConvert.SerializeObject(responseToLog));
        }

        private string EncodeSSN(string ssn)
        {
            if (string.IsNullOrEmpty(ssn))
            {
                return null;
            }
            else if (ssn.Length > 11)
            {
                // likely already encoded
                return ssn;
            }

            ssn = ssn.Replace("-", "");

            byte[] bytes = Encoding.UTF8.GetBytes(ssn);
            SHA256Managed digest = new SHA256Managed();
            byte[] hash = digest.ComputeHash(bytes);

            return Convert.ToBase64String(hash);
        }
    }

    public class ClientComparer : Comparer<ClientDTO>
    {
        public override int Compare(ClientDTO x, ClientDTO y)
        {
            if (x.prime)
            {
                return -1;
            }

            if (y.prime)
            {
                return 1;
            }

            return x.name.CompareTo(y.name);
        }
    }

    public class ClientComparerLastUsed : Comparer<ClientDTO>
    {
        public override int Compare(ClientDTO x, ClientDTO y)
        {
            if (x.prime)
            {
                return -1;
            }

            if (y.prime)
            {
                return 1;
            }

            if (x.lastUsed == null)
            {
                return 1;
            }

            if (y.lastUsed == null)
            {
                return -1;
            }

            return x.lastUsed.CompareTo(y.lastUsed);
        }
    }
}
