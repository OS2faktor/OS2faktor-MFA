using Microsoft.Win32;
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
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private static bool? encryptionEnabled = null;

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

        public static void EncryptInMemoryData(ref byte[] Buffer, MemoryProtectionScope Scope)
        {
            if (Buffer == null)
            {
                throw new ArgumentNullException(nameof(Buffer));
            }
            
            if (Buffer.Length <= 0)
            {
                throw new ArgumentException("The buffer length was 0.", nameof(Buffer));
            }

            if (Buffer.Length % 16 > 0)
            {
                Array.Resize<byte>(ref Buffer, getCorrectSize(Buffer.Length));
            }

            // Encrypt the data in memory. The result is stored in the same array as the original data.
            ProtectedMemory.Protect(Buffer, Scope);
        }

        public static void DecryptInMemoryData(ref byte[] Buffer, MemoryProtectionScope Scope)
        {
            if (Buffer == null)
            {
                throw new ArgumentNullException(nameof(Buffer));
            }

            if (Buffer.Length <= 0)
            {
                throw new ArgumentException("The buffer length was 0.", nameof(Buffer));
            }

            // Decrypt the data in memory. The result is stored in the same array as the original data.
            ProtectedMemory.Unprotect(Buffer, Scope);
            TrimEnd(ref Buffer);
        }

        public static string Base64Encode(byte[] plainTextBytes)
        {
            return Convert.ToBase64String(plainTextBytes);
        }

        public static byte[] Base64Decode(string base64EncodedData)
        {
            return Convert.FromBase64String(base64EncodedData);
        }

        private static int getCorrectSize(int currentSize)
        {
            int start = 16;
            while(currentSize >= start)
            {
                start += 16;
            }
            return start;
        }

        private static void TrimEnd(ref byte[] array)
        {
            int lastIndex = Array.FindLastIndex(array, b => b != 0);

            Array.Resize(ref array, lastIndex + 1);
        }

        public static string GetEncryptedAndEncodedApiKey(string unencryptedApiKey)
        {
            if (string.IsNullOrEmpty(unencryptedApiKey))
            {
                return null;
            }

            if (EncryptionEnabled())
            {
                byte[] toEncrypt = Encoding.UTF8.GetBytes(unencryptedApiKey);
                EncryptInMemoryData(ref toEncrypt, MemoryProtectionScope.SameLogon);
                string base64Encoded = Base64Encode(toEncrypt);

                return base64Encoded;
            }
            else
            {
                return unencryptedApiKey;
            }
        }

        public static string GetDecryptedApiKey(string apiKeyEncoded)
        {
            // input empty, return empty
            if (string.IsNullOrEmpty(apiKeyEncoded))
            {
                return null;
            }

            string decryptedApiKey = null;
            if (EncryptionEnabled())
            {
                byte[] toDecrypt = EncryptionUtil.Base64Decode(apiKeyEncoded);
                EncryptionUtil.DecryptInMemoryData(ref toDecrypt, MemoryProtectionScope.SameLogon);

                decryptedApiKey = Encoding.UTF8.GetString(toDecrypt);
            }
            else
            {
                decryptedApiKey = apiKeyEncoded;
            }

            return decryptedApiKey;
        }

        public static bool EncryptionEnabled()
        {
            // caching
            if (encryptionEnabled != null)
            {
                return (bool) encryptionEnabled;
            }

            var regKey = Registry.LocalMachine.OpenSubKey("SOFTWARE\\OS2\\OS2faktor");
            var regKeyValue = (string)regKey.GetValue("Encryption");
            if (regKeyValue != null && "true".Equals(regKeyValue.ToLower()))
            {
                encryptionEnabled = true;
            }
            else
            {
                encryptionEnabled = false;
            }

            return (bool) encryptionEnabled;
        }
    }
}
