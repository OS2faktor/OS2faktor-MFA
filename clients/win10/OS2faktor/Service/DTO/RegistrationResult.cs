using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace OS2faktor.Service.DTO
{
    public class RegistrationResult
    {
        private bool success;
        private bool invalidPin;
        private string deviceId;
        private string apiKey;

        public bool Success { get => success; set => success = value; }
        public bool InvalidPin { get => invalidPin; set => invalidPin = value; }
        public string DeviceId { get => deviceId; set => deviceId = value; }
        public string ApiKey { get => apiKey; set => apiKey = value; }
    }
}
