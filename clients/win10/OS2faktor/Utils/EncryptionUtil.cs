using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;

namespace OS2faktor.Utils
{
    class EncryptionUtil
    {
        public static string EncodePin(string pin)
        {
            if (string.IsNullOrEmpty(pin))
            {
                return "";
            }

            byte[] bytes = Encoding.UTF8.GetBytes(pin);
            SHA256Managed digest = new SHA256Managed();
            byte[] hash = digest.ComputeHash(bytes);

            return Convert.ToBase64String(hash);
        }
    }
}
