using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace OS2faktor.Service.DTO
{
    public class DeletionResult
    {
        private bool success;
        private string deviceId;
        private string apiKey;
        private bool invalidDeviceId;

        public bool Success { get => success; set => success = value; }
        public string DeviceId { get => deviceId; set => deviceId = value; }
        public string ApiKey { get => apiKey; set => apiKey = value; }

    }
}
