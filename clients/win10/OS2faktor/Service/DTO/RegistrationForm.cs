using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace OS2faktor.Service.DTO
{
    public class RegistrationForm
    {
        [JsonProperty]
        private string name;

        [JsonProperty]
        private string pincode;

        [JsonProperty]
        private string type;

        [JsonIgnore]
        public string Name { get => name; set => name = value; }

        [JsonIgnore]
        public string PinCode { get => pincode; set => pincode = value; }

        [JsonIgnore]
        public string Type { get => type; set => type = value; }

        public RegistrationForm(string deviceName, string pinCode, string type)
        {
            Name = deviceName;
            PinCode = pinCode;
            Type = type;
        }
    }
}
