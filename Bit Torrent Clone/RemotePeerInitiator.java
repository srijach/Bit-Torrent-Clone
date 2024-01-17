
import java.io.*;
import java.util.*;

/**
 * This class gets all the information from the PeerInfo.cfg
 * and initiates the peer processes remotely
 */
public class RemotePeerInitiator
{
	public Vector<Process> peersList = new Vector<Process>();
	public Vector<PeerStateInfo> peersStateList = new Vector<PeerStateInfo>();

	public void getConfiguration() {
		try {
			int itr = 0;
			String tempStr;
			FileReader fr = new FileReader("PeerInfo.cfg");
			BufferedReader configReader = new BufferedReader(fr);

			while((tempStr = configReader.readLine()) != null) {
				 String[] configElements = tempStr.split("\\s+");
				 PeerStateInfo peerState = new PeerStateInfo(configElements[0], configElements[1], configElements[2], itr);
		         peersStateList.addElement(peerState);
		         itr++;
			}
			configReader.close();
		}
		catch (Exception err)
		{
			System.out.println("Exception occurred:" +err.toString());
		}
	}
	/**
	 * Conditional checker that checks if everyone received the file completely
	 */
	public static synchronized boolean isFinished() {

		try {
			FileReader fr = new FileReader("PeerInfo.cfg");
			BufferedReader in = new BufferedReader(fr);

			int hasFileCount = 1;
			String record;

			while ((record = in.readLine()) != null) {
				int tempCount = Integer.parseInt(record.trim().split("\\s+")[3]);
				hasFileCount = hasFileCount * tempCount;
			}
			boolean res = false;
			if(hasFileCount != 0)
				res = true;

			in.close();
			return res;

		} catch (Exception err) {
			return false;
		}
	}

	/**
	 * Main Entry for initiating remote peers
	 * @param args
	 */
	public static void main(String[] args) 
	{
		try 
		{
			RemotePeerInitiator driver = new RemotePeerInitiator();
			driver.getConfiguration();

			String currDirPath = System.getProperty("user.dir");

			for (int i = 0; i < driver.peersStateList.size(); i++) {
				PeerStateInfo peerState = (PeerStateInfo) driver.peersStateList.elementAt(i);
				System.out.println("Spawn the peer " + peerState.PID +  " hosted at " + peerState.address);
				String command = "ssh " + peerState.address + " cd " + currDirPath + "; java peerProcess " + peerState.PID;
				driver.peersList.add(Runtime.getRuntime().exec(command));
				System.out.println(command);
			}		
			
			System.out.println("Waiting for file to be distributed fully..." );
			
			boolean hasFinished = false;
			while(true) {
				hasFinished = isFinished();
				if (hasFinished) {
					System.out.println("All peers have exited from the system!");
					break;
				}
				else {
					try {
						int sleepTime = 5000;
						Thread.currentThread();
						Thread.sleep(sleepTime);
					} catch (InterruptedException err) {
						// nothing to do
					}
				}
			}
		}
		catch (Exception err) {
			System.out.println("Error occurred: " + err.toString());
		}
	}
}