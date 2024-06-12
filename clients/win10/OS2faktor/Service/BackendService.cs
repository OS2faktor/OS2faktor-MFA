using OS2faktor.Service.DTO;
using System;
using System.Net.Http;
using System.Threading.Tasks;

namespace OS2faktor.Service
{
    public class BackendService
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        string backendUrl = Properties.Settings.Default.backendUrl;

        public async Task<StatusResult> GetInitialStatusAsync()
        {
            using (HttpClient client = new HttpClient())
            {
                StatusResult clientStatus = new StatusResult();
                client.DefaultRequestHeaders.Add("ApiKey", Properties.Settings.Default.apiKey);
                client.DefaultRequestHeaders.Add("DeviceId", Properties.Settings.Default.deviceId);

                try
                {
                    HttpResponseMessage response = await client.GetAsync($"{backendUrl}/api/client/v2/status");
                    if (response.IsSuccessStatusCode)
                    {
                        clientStatus = await response.Content.ReadAsAsync<StatusResult>();
                    }
                    else if (response.StatusCode != System.Net.HttpStatusCode.Unauthorized)
                    {
                        clientStatus.LookupFailed = true;
                    }
                    else
                    {
                        clientStatus.Disabled = true;
                    }
                }
                catch (HttpRequestException e)
                {
                    log.Warn($"Error occured while trying to connect to Backend:{backendUrl}: ", e);
                    clientStatus.LookupFailed = true;
                }

                return clientStatus;
            }
        }

        public async Task<RegistrationResult> Register(string deviceName, string pinCode)
        {
            using (HttpClient client = new HttpClient())
            {
                var registrationResult = new RegistrationResult();
                RegistrationForm registrationForm = new RegistrationForm(deviceName, pinCode, "WINDOWS");
                client.DefaultRequestHeaders.Add("clientVersion", Properties.Settings.Default.version);

                try
                {
                    HttpResponseMessage response = await client.PostAsJsonAsync($"{backendUrl}/xapi/client/v2/register", registrationForm);
                    if (response.IsSuccessStatusCode)
                    {
                        registrationResult = await response.Content.ReadAsAsync<RegistrationResult>();
                    }
                    else
                    {
                        registrationResult.Success = false;
                        registrationResult.InvalidPin = false;
                    }
                }
                catch (HttpRequestException e)
                {
                    log.Warn($"Error occured while trying to connect to Backend:{backendUrl}: ", e);
                    registrationResult.Success = false;
                    registrationResult.InvalidPin = false;
                }

                return registrationResult;
            }
        }


        public async Task<DeletionResult> Delete(string deviceId)
        {
            using (HttpClient client = new HttpClient())
            {
                var deletionResult = new DeletionResult();
                client.DefaultRequestHeaders.Add("ApiKey", Properties.Settings.Default.apiKey);
                client.DefaultRequestHeaders.Add("DeviceId", Properties.Settings.Default.deviceId);

                try
                {
                    HttpResponseMessage response = await client.DeleteAsync($"{backendUrl}/api/client/");
                    if (response.IsSuccessStatusCode) 
                    {
                        deletionResult = await response.Content.ReadAsAsync<DeletionResult>();
                    }
                    else
                    {
                        deletionResult.Success = false;
                    }
                }
                catch(HttpRequestException e)
                {
                    log.Warn($"Error occured while trying to connect to the Backend:{backendUrl}: ", e);
                    deletionResult.Success = false;
                }
                return deletionResult;
            }   
        }
    }
}
