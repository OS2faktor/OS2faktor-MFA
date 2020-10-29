using System;
using System.IO;

namespace VPN
{
    class FileUtil
    {
        public static string GeneratePowershellScript(string powershellPath, string uid, string pwd)
        {
            string content = File.ReadAllText(powershellPath);

            content = content.Replace("%uid", uid);
            content = content.Replace("%pwd", pwd);

            String tempFolder = Config.GetTempFolder();
            if (!tempFolder.EndsWith("\\"))
            {
                tempFolder = tempFolder + "\\";
            }

            string file = tempFolder + uid + ".ps1";
            File.WriteAllText(file, content);

            return file;
        }
    }
}
