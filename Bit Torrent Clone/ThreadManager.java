
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class allocates and de-allocates the threads
 */
public class ThreadManager implements Runnable
{
	Thread sender;
	Socket clientSocket;
	private ServerSocket serverSocket;
	private String PID;

	/**
	 * Param-Constructor for ThreadManager
	 * @param skt
	 * @param pid
	 */
	public ThreadManager(ServerSocket skt, String pid)
	{
		this.PID = pid;
		this.serverSocket = skt;
	}

	/**
	 * Runnable thread method
	 */
	public void run() 
	{
		while(true)
		{
			try
			{
				clientSocket = serverSocket.accept();
				sender = new Thread(new PeerManager(clientSocket,0, PID));
				peerProcess.writeLog(PID + " established connection successfully");
				peerProcess.sndThreadVector.add(sender);
				sender.start();
			}
			catch(Exception e)
			{
				peerProcess.writeLog(this.PID + "Connection Error " + e.toString());
			}
		}
	}

	/**
	 * Releases the thread once it's completed using
	 */
	public void freeThread()
	{
		try 
		{
			if(!clientSocket.isClosed())
			clientSocket.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}


