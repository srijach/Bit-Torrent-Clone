
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This class implements the message configurations and starts the peers
 */
public class peerProcess implements DefaultMessageConfiguration
{
	public ServerSocket serverSkt = null;
	public int PEER_PORT;

	public String PEER_IP = null;
	public static String PID;
	public int PIdx;
	public static Thread msgThread;
	public Thread clientThread;
	public static boolean isFinished = false;
	public static BitFieldMessage myBFM = null;
	public static volatile Queue<PayLoadContainer> msgQueue = new LinkedList<>();
	public static volatile Timer unchokingTimer;
	public static volatile Timer preferedNeighbourTimer;
	public static Vector<Thread> rcvThreadVector = new Vector<>();
	public static Vector<Thread> sndThreadVector = new Vector<>();
	public static Hashtable<String, Socket> pIDSktMap = new Hashtable<>();
	public static volatile Hashtable<String, PeerStateInfo> peerInfoMap = new Hashtable<>();
	public static volatile Hashtable<String, PeerStateInfo> prefNbrMap = new Hashtable<>();
	public static volatile Hashtable<String, PeerStateInfo> unchokedNbrMap = new Hashtable<>();

	/**
	 * Adds the payload wrapper to the message queue
	 * @param payloadcontainer object
	 */
	public static synchronized void appendToMessageQ(PayLoadContainer plc)
	{
		msgQueue.add(plc);
	}

	/**
	 * Removes the payload wrapper from the message queue
	 * @return PayLoadContainer object
	 */
	public static synchronized PayLoadContainer deleteFromMessageQ()
	{
		PayLoadContainer plc = null;

		if(!msgQueue.isEmpty()) plc = msgQueue.remove();
		return plc;
	}

	/**
	 * Gets the peer information from PeerInfo.cfg repeatedly.
	 */
	public static void getPeerInfoRepeat()
	{
		try 
		{
			String temp;
			FileReader fr = new FileReader("PeerInfo.cfg");
			BufferedReader br = new BufferedReader(fr);
			while ((temp = br.readLine()) != null)
			{
				String[] args = temp.trim().split("\\s+");
				String pId = args[0];
				int isDone = Integer.parseInt(args[3]);
				if(isDone == 1)	{
					peerInfoMap.get(pId).isCompleted = 1;
					peerInfoMap.get(pId).isInterested = 0;
					peerInfoMap.get(pId).isChoked = 0;
				}
			}
			br.close();
		}
		catch (Exception e) {
			writeLog(PID + e.toString());
		}
	}

	/**
	 * This class handles the preferred neighbors with specified intervals
	 */
	public static class PreferedNeighbors extends TimerTask {
		public void run()
		{
			getPeerInfoRepeat();
			Enumeration<String> peerInfoKeys = peerInfoMap.keys();
			int noOfinterestedPeers = 0;
			String tempStr = "";
			while(peerInfoKeys.hasMoreElements())
			{
				String peerInfoKey = (String)peerInfoKeys.nextElement();
				PeerStateInfo info = peerInfoMap.get(peerInfoKey);
				if(peerInfoKey.equals(PID)) {
					continue;
				}
				if (info.isCompleted == 0 && info.isHandShakeDone == 1)
				{
					noOfinterestedPeers++;
				} 
				else if(info.isCompleted == 1)
				{
					try
					{
						prefNbrMap.remove(peerInfoKey);
					}
					catch (Exception e) { //do nothing
					}
				}
			}
			if(noOfinterestedPeers > CommonConfiguration.NumberOfPreferredNeighbors)
			{
				boolean checkPoint = prefNbrMap.isEmpty();
				if(!checkPoint) prefNbrMap.clear();

				List<PeerStateInfo> psi = new ArrayList<>(peerInfoMap.values());
				Collections.sort(psi, new OrderByDataRate(false));

				int cnt = 0;
				for (int i = 0; i < psi.size(); i++)
				{
					if (cnt > CommonConfiguration.NumberOfPreferredNeighbors - 1)
					{
						break;
					}
					if(psi.get(i).isHandShakeDone == 1
							&& !psi.get(i).PID.equals(PID)
							&& peerInfoMap.get(psi.get(i).PID).isCompleted == 0){
						peerInfoMap.get(psi.get(i).PID).isFavorableNbr = 1;
						prefNbrMap.put(psi.get(i).PID, peerInfoMap.get(psi.get(i).PID));
						cnt++;
						tempStr = tempStr + psi.get(i).PID + ", ";
						
						if (peerInfoMap.get(psi.get(i).PID).isChoked == 1)
						{
							passUnchokeMsg(peerProcess.pIDSktMap.get(psi.get(i).PID), psi.get(i).PID);
							peerProcess.peerInfoMap.get(psi.get(i).PID).isChoked = 0;
							passHaveMsg(peerProcess.pIDSktMap.get(psi.get(i).PID), psi.get(i).PID);
							peerProcess.peerInfoMap.get(psi.get(i).PID).state = 3;
						}
					}
				}
			}
			else
			{
				peerInfoKeys = peerInfoMap.keys();
				while(peerInfoKeys.hasMoreElements())
				{
					String peerInfoKey = (String)peerInfoKeys.nextElement();
					PeerStateInfo info = peerInfoMap.get(peerInfoKey);
					if(peerInfoKey.equals(PID)) {
						continue;
					}
					if (info.isCompleted == 0
							&& info.isHandShakeDone == 1){
						if(!prefNbrMap.containsKey(peerInfoKey))
						{
							tempStr = tempStr + peerInfoKey + ", ";
							prefNbrMap.put(peerInfoKey, peerInfoMap.get(peerInfoKey));
							peerInfoMap.get(peerInfoKey).isFavorableNbr = 1;
						}
						if (info.isChoked == 1)	{
							passUnchokeMsg(peerProcess.pIDSktMap.get(peerInfoKey), peerInfoKey);
							peerProcess.peerInfoMap.get(peerInfoKey).isChoked = 0;
							passHaveMsg(peerProcess.pIDSktMap.get(peerInfoKey), peerInfoKey);
							peerProcess.peerInfoMap.get(peerInfoKey).state = 3;
						}
					}
					
				}
			}
			if (tempStr != "")
				peerProcess.writeLog(peerProcess.PID + " picked the neighbours as preferred: " + tempStr);
		}
	}

	/**
	 * Passes the UNCHOKE message to the respective peer
	 * @param skt
	 * @param remPID
	 */
	private static void passUnchokeMsg(Socket skt, String remPID) {
		writeLog(PID + " is passing UNCHOKE message to the remote Peer " + remPID);
		PayLoad pl = new PayLoad(UNCHOKE_DATA_MSG);
		byte[] bytesPl = PayLoad.toByteArray(pl);
		dispatchMessage(skt, bytesPl);

	}

	/**
	 * Passes the HAVE message to the respective peer
	 * @param skt
	 * @param remPId
	 */
	private static void passHaveMsg(Socket skt, String remPId) {
		byte[] convertedByteArr = peerProcess.myBFM.convertToBytes();
		writeLog(PID + " passing a HAVE message to the Peer " + remPId);
		PayLoad pl = new PayLoad(HAVE_DATA_MSG, convertedByteArr);
		dispatchMessage(skt, PayLoad.toByteArray(pl));
		convertedByteArr = null;
	}

	/**
	 * Writes the message bytes to the respective output stream of the socket
	 * @param skt
	 * @param convertedByteArr
	 * @return
	 */
	private static int dispatchMessage(Socket skt, byte[] convertedByteArr) {
		try
		{
			OutputStream oStream = skt.getOutputStream();
			oStream.write(convertedByteArr);
		}
		catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	/**
	 * This class handles the optimistically unchoked neighbors with the given intervals
	 */
	public static class UnchokedNbrHandler extends TimerTask {
		public void run() {
			getPeerInfoRepeat();

			if(!unchokedNbrMap.isEmpty()) {
				unchokedNbrMap.clear();
			}

			Enumeration<String> peerInfoKeys = peerInfoMap.keys();
			Vector<PeerStateInfo> peerInfoVector = new Vector<>();

			while(peerInfoKeys.hasMoreElements()) {
				String peerInfoKey = (String) peerInfoKeys.nextElement();
				PeerStateInfo info = peerInfoMap.get(peerInfoKey);

				if (info.isChoked == 1 && !peerInfoKey.equals(PID) && info.isCompleted == 0 && info.isHandShakeDone == 1)
					peerInfoVector.add(info);
			}

			if (peerInfoVector.size() > 0) {
				Collections.shuffle(peerInfoVector);
				PeerStateInfo firstPeer = peerInfoVector.firstElement();
				peerInfoMap.get(firstPeer.PID).isOptimisticUnchoked = 1;
				unchokedNbrMap.put(firstPeer.PID, peerInfoMap.get(firstPeer.PID));
				peerProcess.writeLog(peerProcess.PID + " setting the optimistically unchoked neighbor to " + firstPeer.PID);
				
				if (peerInfoMap.get(firstPeer.PID).isChoked == 1) {
					peerProcess.peerInfoMap.get(firstPeer.PID).isChoked = 0;
					passUnchokeMsg(peerProcess.pIDSktMap.get(firstPeer.PID), firstPeer.PID);
					passHaveMsg(peerProcess.pIDSktMap.get(firstPeer.PID), firstPeer.PID);
					peerProcess.peerInfoMap.get(firstPeer.PID).state = 3;
				}
			}
		}
	}

	/**
	 * Starts the unchoked neighbors
	 */
	public static void initUnchokedNbrs() {
		preferedNeighbourTimer = new Timer();
		preferedNeighbourTimer.schedule(new UnchokedNbrHandler(), 0,CommonConfiguration.OptimisticUnchokingInterval * 1000);
	}

	/**
	 * Stops the unchoked neighbors
	 */
	public static void haltUnchokedNbrs() {
		preferedNeighbourTimer.cancel();
	}

	/**
	 * Starts the preferred neighbors
	 */
	public static void initPrefNbrs() {
		preferedNeighbourTimer = new Timer();
		preferedNeighbourTimer.schedule(new PreferedNeighbors(),0,CommonConfiguration.UnchokingInterval * 1000);
	}

	/**
	 * Stops the preferred neighbors
	 */
	public static void haltPrefNbrs() {
		preferedNeighbourTimer.cancel();
	}

	/**
	 * Writes the log to the file
	 * @param message
	 */
	public static void writeLog(String message) {
		Logger.saveLog(DataTransformUtils.getCurrentTime() + ": Peer " + message);
		System.out.println(DataTransformUtils.getCurrentTime() + ": Peer " + message);
	}
	
	/**
	 * Reads the configuration details from the Common.cfg file and
	 * stores to CommonConfiguration constants
	 */
	public static void fetchCommonConfigurations() {
		String row;
		try
		{
			FileReader fr = new FileReader("Common.cfg");
			BufferedReader br = new BufferedReader(fr);

			while ((row = br.readLine()) != null) {
				String[] words = row.split("\\s+");
				if (words[0].equalsIgnoreCase("NumberOfPreferredNeighbors"))
				{
					CommonConfiguration.NumberOfPreferredNeighbors = Integer.parseInt(words[1]);
				}
				else if (words[0].equalsIgnoreCase("UnchokingInterval"))
				{
					CommonConfiguration.UnchokingInterval = Integer.parseInt(words[1]);
				}
				else if (words[0].equalsIgnoreCase("OptimisticUnchokingInterval"))
				{
					CommonConfiguration.OptimisticUnchokingInterval = Integer.parseInt(words[1]);
				}
				else if (words[0].equalsIgnoreCase("FileName"))
				{
					CommonConfiguration.FileName = words[1];
				}
				else if (words[0].equalsIgnoreCase("FileSize"))
				{
					CommonConfiguration.FileSize = Integer.parseInt(words[1]);
				}
				else if (words[0].equalsIgnoreCase("PieceSize"))
				{
					CommonConfiguration.PieceSize = Integer.parseInt(words[1]);
				}
			}
			br.close();
		}
		catch (Exception er) {
			writeLog(PID + er.toString());
		}
	}

	/**
	 * Reads the Peer details from the PeerInfo.cfg file 
	 * and stores to peer Info map
	 */
	public static void fetchPeerDetails() {
		String row;
		try
		{
			FileReader fr = new FileReader("PeerInfo.cfg");
			BufferedReader br = new BufferedReader(fr);

			int i = 0;
			while ((row = br.readLine()) != null)
			{
				String[] words = row.split("\\s+");
				PeerStateInfo psi = new PeerStateInfo(words[0], words[1], words[2], Integer.parseInt(words[3]), i);
				peerInfoMap.put(words[0], psi);
				i++;
			}
			br.close();
		} catch (Exception err) {
			writeLog(PID + err.toString());
		}
	}

	/**
	 * Main Entry Point
	 * @param args
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args) 
	{
		peerProcess peerThread = new peerProcess();
		PID = args[0];
		try
		{
			// logs the output to a file
			Logger.InitLogging("logs/log_peer_" + PID +".log");
			writeLog(PID + " is started");

			// fetches Common.cfg file and updates to CommonConfiguration class
			fetchCommonConfigurations();
			writeLog("Fetched common config");

			// fetches PeerInfo.cfg file and updates to PeerStateInfo class
			fetchPeerDetails();
			writeLog("Fetched Peer Details");

			populatePrefNbrs();
			// writeLog("testing");

			boolean checkForPeerOne = false;
			Enumeration<String> enumKeys = peerInfoMap.keys();
			
			while(enumKeys.hasMoreElements()) {
				PeerStateInfo peerStateInfo = peerInfoMap.get(enumKeys.nextElement());
				if(peerStateInfo.PID.equals(PID)) {
					peerThread.PEER_PORT = Integer.parseInt(peerStateInfo.port);
					peerThread.PIdx = peerStateInfo.Index;

					if(peerStateInfo.getIsInvokedFirst() == 1) {
						checkForPeerOne = true;
						break;
					}
				}
			}

			myBFM = new BitFieldMessage();
			int peerOneFlag = checkForPeerOne ? 1: 0;
			myBFM.createMyBitField(PID, peerOneFlag);
			
			msgThread = new Thread(new MsgHandler(PID));
			msgThread.start();
			
			if(checkForPeerOne) {
				try
				{
					peerThread.serverSkt = new ServerSocket(peerThread.PEER_PORT);
					peerThread.clientThread = new Thread(new ThreadManager(peerThread.serverSkt, PID));
					peerThread.clientThread.start();
				}
				catch(SocketTimeoutException ste)
				{
					writeLog(PID + " ran into timeout error: " + ste.toString());
					Logger.terminateLogging();
					System.exit(0);
				}
				catch(IOException io) {
					writeLog(PID + " ran into errors while listening through thread: " + peerThread.PEER_PORT + io.toString());
					Logger.terminateLogging();
					System.exit(0);
				}
			}

			else {
				generateBlankFile();
				enumKeys = peerInfoMap.keys();

				while(enumKeys.hasMoreElements()) {
					PeerStateInfo peerStateInfo = peerInfoMap.get(enumKeys.nextElement());
					if(peerThread.PIdx > peerStateInfo.Index) {
						Thread t = new Thread(
								new PeerManager(peerStateInfo.getAddress(), Integer.parseInt(peerStateInfo.getPort()),
										1, PID));
						rcvThreadVector.add(t);
						t.start();
					}
				}

				try {
					peerThread.serverSkt = new ServerSocket(peerThread.PEER_PORT);
					peerThread.clientThread = new Thread(new ThreadManager(peerThread.serverSkt, PID));
					peerThread.clientThread.start();
				}
				catch(SocketTimeoutException ste) {
					writeLog(PID
							+ " ran into time out error while starting the thread: "
							+ ste.toString());

					Logger.terminateLogging();
					System.exit(0);
				}
				catch(IOException io) {
					writeLog(PID
							+ " ran into IO exception while starting the thread: "
							+ peerThread.PEER_PORT
							+ " "
							+ io.toString());

					Logger.terminateLogging();
					System.exit(0);
				}
			}
			
			initPrefNbrs();
			initUnchokedNbrs();
			
			while(true) {
				isFinished = isFinished();
				if (isFinished) {
					writeLog("All peers have finished downloading the file.");
					haltPrefNbrs();
					haltUnchokedNbrs();
					try
					{
						Thread.currentThread();
						Thread.sleep(2000);
					}
					catch (InterruptedException ex) {
						// nothing to do
					}
					if (peerThread.clientThread.isAlive()){
						peerThread.clientThread.stop();
					}
					if (msgThread.isAlive()) {
						msgThread.stop();
					}

					for (int i = 0; i < rcvThreadVector.size(); i++)
					{
						if (rcvThreadVector.get(i).isAlive())
						{
							rcvThreadVector.get(i).stop();
						}
					}

					for (int i = 0; i < sndThreadVector.size(); i++)
					{
						if (sndThreadVector.get(i).isAlive())
						{
							sndThreadVector.get(i).stop();
						}
					}
					break;
				}
				else
				{
					try
					{
						Thread.currentThread();
						Thread.sleep(5000);
					}
					catch (InterruptedException ie) {
						// nothing to do
					}
				}
			}
		}
		catch(Exception err)
		{
			writeLog(PID
					+ " Exception at the end of the process : "
					+ err.getMessage());
		}
		finally {
			writeLog(PID + " Peer process will exit now");
			Logger.terminateLogging();
			System.exit(0);
		}
	}

	/**
	 * Populates the preferred neighbors to the prefNbrMap
	 */
	private static void populatePrefNbrs()
	{
		Enumeration<String> enumKeys = peerInfoMap.keys();
		while(enumKeys.hasMoreElements()) {
			String element = (String)enumKeys.nextElement();
			if(!element.equals(PID)){
				prefNbrMap.put(element, peerInfoMap.get(element));
			}
		}
	}

	/**
	 * Creates an empty file
	 */
	public static void generateBlankFile() {
		try {
			File f = new File(PID);
			f.mkdir();
			OutputStream stream = new FileOutputStream(new File(PID, CommonConfiguration.FileName), true);
			byte dataByte = 0;
			for (int i = 0; i < CommonConfiguration.FileSize; i++)
				stream.write(dataByte);
			stream.close();
		}
		catch (Exception e)
		{
			writeLog(PID + " exception while generating the new file : " + e.getMessage());
		}

	}

	/**
	 * Conditional checker that checks if everyone received the file completely
	 */
	public static synchronized boolean isFinished() {
		String row;
		int numberOfFiles = 1;

		try {
			FileReader fr = new FileReader("PeerInfo.cfg");
			BufferedReader br = new BufferedReader(fr);

			while ((row = br.readLine()) != null) {
				numberOfFiles = numberOfFiles * Integer.parseInt(row.trim().split("\\s+")[3]);
			}
			if (numberOfFiles == 0)
			{
				br.close();
				return false;
			}
			else
			{
				br.close();
				return true;
			}

		} catch (Exception e) {
			writeLog(e.toString());
			return false;
		}

	}
}
