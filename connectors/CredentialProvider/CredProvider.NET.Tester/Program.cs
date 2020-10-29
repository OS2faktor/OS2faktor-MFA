using System;

namespace CredProvider.NET.Tester
{
    class Program
    {
        static void Main(string[] args)
        {
            var networkCredential = CredentialsDialog.GetCredentials("Hey!", "We would like a login.");

            if (networkCredential != null)
            {
                Console.WriteLine($"Username: \'{networkCredential.UserName}\'");
            }
            else
            {
                Console.WriteLine("No credential detected.");
            }
            Console.ReadLine();
        }
    }
}
