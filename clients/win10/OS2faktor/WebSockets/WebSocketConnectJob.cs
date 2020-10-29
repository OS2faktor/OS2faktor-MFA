using Quartz;
using System.Threading.Tasks;

namespace OS2faktor
{
    internal class WebSocketConnectJob : IJob
    {
        public Task Execute(IJobExecutionContext context)
        {   
            if (!WSCommunication.WebSocket.IsAlive)
            {   
                WSCommunication.Connect();
            }

            return Task.FromResult(0);
        }
    }
}