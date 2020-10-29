using System;
using System.Data.Common;
using System.Data.SqlClient;

namespace OS2faktorPlugin
{
    class Dao
    {
        private static log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        public static User GetUser(string connectionString, string statement, string sAMAccountName)
        {
            try
            {
                using (DbConnection connection = new SqlConnection(connectionString))
                {
                    connection.Open();

                    string sql = BuildStatement(statement, sAMAccountName);
                    log.Debug("Executing: " + sql);

                    using (DbCommand command = new SqlCommand(sql, (SqlConnection)connection))
                    {
                        using (DbDataReader reader = command.ExecuteReader())
                        {
                            if (reader.Read())
                            {
                                string cpr = (string)reader["ssn"];

                                cpr = cpr.Replace("-", "");
                                cpr = cpr.Replace(" ", "");

                                if (cpr.Length != 10)
                                {
                                    log.Error("CprService returned invalid cpr (" + cpr + ") for " + sAMAccountName);
                                    return null;
                                }

                                return new User()
                                {
                                    Cpr = cpr
                                };
                            }

                            log.Warn("Could not find CPR in SQL for: " + sAMAccountName);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                log.Error("Failed to connect to SQL", ex);
            }

            return null;
        }

        private static string BuildStatement(string statement, string sAMAccountName)
        {
            return statement.Replace("{sAMAccountName}", sAMAccountName);
        }
    }
}
