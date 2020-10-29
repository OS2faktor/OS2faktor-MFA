using System;
using System.Collections.Generic;
using System.DirectoryServices;

namespace OS2faktorPlugin
{
    class ADConnector
    {
        private static log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        private string adUrl;
        private string adUsername;
        private string adPassword;

        public ADConnector(string adUrl, string adUsername, string adPassword)
        {
            this.adUrl = adUrl;
            this.adUsername = adUsername;
            this.adPassword = adPassword;
        }

        public void SetDeviceIdOnUser(string username, string deviceIdField, string deviceId)
        {
            try
            {
                using (DirectoryEntry entry = GetDirectoryEntry())
                {
                    using (DirectorySearcher search = new DirectorySearcher(entry))
                    {
                        search.Filter = string.Format("(&(objectClass=user)(sAMAccountName={0}))", username);

                        if (!string.IsNullOrEmpty(deviceIdField))
                        {
                            search.PropertiesToLoad.Add(deviceIdField);
                        }

                        SearchResult result = search.FindOne();
                        if (result != null)
                        {
                            var de = result.GetDirectoryEntry();

                            if (!de.Properties.Contains(deviceIdField))
                            {
                                log.Info("Setting deviceId on: " + username);

                                de.Properties[deviceIdField].Add(deviceId);
                                de.CommitChanges();
                            }
                            else
                            {
                                log.Warn("User already had a deviceId registered: " + username);
                            }
                        }
                        else
                        {
                            log.Warn("Could not find user: " + username);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                log.Error("Failed to store deviceId on user: " + username, ex);
            }
        }

        public User GetUser(string username, string cprField, string pidField, string pseudonymField, string deviceIdField)
        {
            User user = new User();

            using (DirectoryEntry entry = GetDirectoryEntry())
            {
                using (DirectorySearcher search = new DirectorySearcher(entry))
                {
                    search.Filter = string.Format("(&(objectClass=user)(sAMAccountName={0}))", username);

                    if (!string.IsNullOrEmpty(cprField))
                    {
                        search.PropertiesToLoad.Add(cprField);
                    }

                    if (!string.IsNullOrEmpty(pidField))
                    {
                        search.PropertiesToLoad.Add(pidField);
                    }

                    if (!string.IsNullOrEmpty(pseudonymField))
                    {
                        search.PropertiesToLoad.Add(pseudonymField);
                    }

                    if (!string.IsNullOrEmpty(deviceIdField))
                    {
                        search.PropertiesToLoad.Add(deviceIdField);
                    }

                    SearchResult result = search.FindOne();
                    if (result != null)
                    {
                        ResultPropertyValueCollection cprResult = null;
                        ResultPropertyValueCollection deviceIdResult = null;
                        ResultPropertyValueCollection pidResult = null;
                        ResultPropertyValueCollection pseudonymResult = null;

                        if (result.Properties != null)
                        {
                            if (result.Properties.Contains(cprField))
                            {
                                cprResult = result.Properties[cprField];
                            }
                            if (result.Properties.Contains(deviceIdField))
                            {
                                deviceIdResult = result.Properties[deviceIdField];
                            }
                            if (result.Properties.Contains(pidField))
                            {
                                pidResult = result.Properties[pidField];
                            }
                            if (result.Properties.Contains(pseudonymField))
                            {
                                pseudonymResult = result.Properties[pseudonymField];
                            }
                        }
                        else
                        {
                            log.Warn("Found no attributes for " + username);
                        }

                        if (pseudonymResult != null && pseudonymResult.Count > 0)
                        {
                            user.Pseudonym = pseudonymResult[0].ToString();
                        }

                        if (pidResult != null && pidResult.Count > 0)
                        {
                            user.Pid = pidResult[0].ToString();
                        }

                        if (cprResult != null && cprResult.Count > 0)
                        {
                            user.Cpr = cprResult[0].ToString();
                        }

                        if (deviceIdResult != null && deviceIdResult.Count > 0)
                        {
                            var deviceIdsList = new List<string>();

                            for (int i = 0; i < deviceIdResult.Count; i++)
                            {
                                var tmp = deviceIdResult[i].ToString();
                                var tokens = tmp.Split(',');
                                foreach (var token in tokens)
                                {
                                    deviceIdsList.Add(token);
                                }
                            }

                            user.DeviceIds = deviceIdsList.ToArray();
                        }
                    }
                }
            }

            return user;
        }

        private DirectoryEntry GetDirectoryEntry()
        {
            if (string.IsNullOrEmpty(adUrl))
            {
                return new DirectoryEntry();
            }
            else if (string.IsNullOrEmpty(adUsername) || string.IsNullOrEmpty(adPassword))
            {
                return new DirectoryEntry(adUrl);
            }
            else
            {
                return new DirectoryEntry(adUrl, adUsername, adPassword);
            }
        }
    }
}
