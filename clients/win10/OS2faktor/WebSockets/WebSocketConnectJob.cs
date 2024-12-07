using Quartz;
using System.Threading.Tasks;

namespace OS2faktor
{
    internal class WebSocketConnectJob : IJob
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        public Task Execute(IJobExecutionContext context)
        {
            log.Debug("IsAlive: " + WSCommunication.WebSocket.IsAlive);
            if (!WSCommunication.WebSocket.IsAlive)
            {   
                WSCommunication.Connect();
            }

            return Task.FromResult(0);
        }
    }
}