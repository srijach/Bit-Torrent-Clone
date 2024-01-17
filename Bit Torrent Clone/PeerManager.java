
import java.net.*;
import java.io.*;
import java.util.Arrays;

/**
 * This class implements Runnable interface which intends to execute on threads.
 * It manages the peers and their repective sockets.
 */
public class PeerManager implements Runnable, DefaultMessageConfiguration
{
	private InputStream iStream;
	private Socket clientSkt = null;
	private int connectionType;
	private OutputStream oStream;

	private HandShake hndShkMsg;
	
	String myPID, remPID;
	
	final int CONNECTIONACTIVE = 1;
	final int CONNECTIONPASSIVE = 0;

	/**
	 * This method opens and closes the socket by getting the input stream
	 * @param iStream
	 * @param skt
	 */
	public void openClose(InputStream iStream, Socket skt)
	{
		try {
			iStream = skt.getInputStream();
			iStream.close();
		} catch (IOException err) {
			err.printStackTrace();
		}
		
	}

	/**
	 * Parameterised constructor for PeerManager that takes socket object
	 * @param peerSkt
	 * @param connectionType
	 * @param myPID
	 */
	public PeerManager(Socket peerSkt, int connectionType, String myPID) {
		
		this.clientSkt = peerSkt;
		this.connectionType = connectionType;
		this.myPID = myPID;
		try 
		{
			oStream = peerSkt.getOutputStream();
			iStream = peerSkt.getInputStream();
		}
		catch (Exception err)
		{
			peerProcess.writeLog(this.myPID
								+ " Error : "
								+ err.getMessage());
		}
	}

	/**
	 * Parameterised constructor for PeerManager that doesnot take socket object but instead
	 * creates it from the given IP address and port number.
	 * @param address
	 * @param portNumber
	 * @param connectionType
	 * @param myPID
	 */
	public PeerManager(String address, int portNumber, int connectionType, String myPID)
	{	
		try {
			this.connectionType = connectionType;
			this.myPID = myPID;
			this.clientSkt = new Socket(address, portNumber);
		} 
		catch (UnknownHostException e) {
			peerProcess.writeLog(myPID + " Peer Manager : " + e.getMessage());
		} 
		catch (IOException e) {
			peerProcess.writeLog(myPID + " Peer Manager : " + e.getMessage());
		}
		this.connectionType = connectionType;
		try {
			iStream = clientSkt.getInputStream();
			oStream = clientSkt.getOutputStream();
		} 
		catch (Exception ex) {
			peerProcess.writeLog(myPID + " Peer Manager : " + ex.getMessage());
		}
	}

	/**
	 * Starts the handshake process and logs it.
	 * @return boolean
	 */
	public boolean InitiateHndShk()
	{
		try {
			HandShake hsObj = new HandShake(DefaultMessageConfiguration.HEADER_HANDSHAKE, this.myPID);
			oStream.write(HandShake.ToByteArray(hsObj));
		} 
		catch (IOException err) {
			peerProcess.writeLog(this.myPID
					+ " Initiate Hand Shake : "
					+ err.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Thread method that is runnable.
	 */
	public void run() 
	{
		int bufferSize = 32;
		byte[] hndShkBuffer = new byte[bufferSize];
		byte[] noPayloadBufferData = new byte[LENGTH_DATA_MSG + TYPE_DATA_MSG];
		byte[] messageLength;
		byte[] messageCategory;
		PayLoadContainer payLoadContainer = new PayLoadContainer();

		try{
			if(this.connectionType == CONNECTIONACTIVE) {
				if(!InitiateHndShk()) {
					peerProcess.writeLog(myPID
							+ " HANDSHAKE dispatch failed.");
					System.exit(0);
				}
				else
					peerProcess.writeLog(myPID + " HANDSHAKE message sent...");
				while(true) {
					iStream.read(hndShkBuffer);
					hndShkMsg = HandShake.ToHandShakeType(hndShkBuffer);
					if(hndShkMsg.getHeaderString().equals(DefaultMessageConfiguration.HEADER_HANDSHAKE)) {
						remPID = hndShkMsg.getPeerIDString();
						peerProcess.writeLog(myPID
								+ " attempts to send first HANDSHAKE message to the Peer "
								+ remPID);
						peerProcess.writeLog(myPID
								+ " Received HANDSHAKE from the Peer " + remPID);

						peerProcess.pIDSktMap.put(remPID, this.clientSkt);
						break;
					}
					else continue;
				}
				
				// Here we are dispatching the Bit Field message
				PayLoad pl = new PayLoad(BITFIELD_DATA_MSG, peerProcess.myBFM.convertToBytes());
				byte[] bytePl = PayLoad.toByteArray(pl);
				oStream.write(bytePl);
				peerProcess.peerInfoMap.get(remPID).state = 8;
			}
			// Establishing a passive type socket connection
			else {
				while(true) {
					iStream.read(hndShkBuffer);
					hndShkMsg = HandShake.ToHandShakeType(hndShkBuffer);
					if(hndShkMsg.getHeaderString().equals(DefaultMessageConfiguration.HEADER_HANDSHAKE)) {
						remPID = hndShkMsg.getPeerIDString();
						peerProcess.writeLog(myPID
								+ " attempts to send HANDSHAKE message to the Peer " + remPID);
						peerProcess.writeLog(myPID
								+ " Received HANDSHAKE from the Peer " + remPID);

						peerProcess.pIDSktMap.put(remPID, this.clientSkt);
						break;
					}
					else continue;
				}
				if(!InitiateHndShk()) {
					peerProcess.writeLog(myPID + " HANDSHAKE dispatch failed.");
					System.exit(0);
				}
				else
					peerProcess.writeLog(myPID + " HANDSHAKE acknowledged and TCP connection established.");
				peerProcess.peerInfoMap.get(remPID).state = 2;
			}
			// receive data messages constantly from the peers
			while(true) {
				int headBytes = iStream.read(noPayloadBufferData);
				if(headBytes == -1)
					break;

				// creating message shell
				messageLength = new byte[LENGTH_DATA_MSG];
				messageCategory = new byte[TYPE_DATA_MSG];
				System.arraycopy(noPayloadBufferData, 0, messageLength, 0, LENGTH_DATA_MSG);
				System.arraycopy(noPayloadBufferData, LENGTH_DATA_MSG, messageCategory, 0, TYPE_DATA_MSG);

				// setting up the payload
				PayLoad dataPayload = new PayLoad();
				dataPayload.setPayloadType(messageCategory);
				dataPayload.setMessageLength(messageLength);

				if(Arrays.asList(DefaultMessageConfiguration.CHOKE_DATA_MSG, DefaultMessageConfiguration.UNCHOKE_DATA_MSG,
						DefaultMessageConfiguration.INTERESTED_DATA_MSG, DefaultMessageConfiguration.NOTINTERESTED_DATA_MSG)
						.contains(dataPayload.getPayloadTypeString())){
					payLoadContainer.messageData = dataPayload;
					payLoadContainer.senderPID = this.remPID;
					peerProcess.appendToMessageQ(payLoadContainer);
				}

				else {
					int seen = 0;
					int seenBytes;
					byte[] dataBufferPl = new byte[dataPayload.getMessageLengthInt()-1];
					while(seen < dataPayload.getMessageLengthInt()-1){
						seenBytes = iStream.read(dataBufferPl, seen, dataPayload.getMessageLengthInt()-1-seen);
						if(seenBytes == -1)
							return;
						seen += seenBytes;
					}
					
					byte[] bufferWithPl = new byte [dataPayload.getMessageLengthInt() + LENGTH_DATA_MSG];
					System.arraycopy(noPayloadBufferData, 0, bufferWithPl, 0, LENGTH_DATA_MSG + TYPE_DATA_MSG);
					System.arraycopy(dataBufferPl, 0, bufferWithPl, LENGTH_DATA_MSG + TYPE_DATA_MSG, dataBufferPl.length);
					
					PayLoad msgWithPl = PayLoad.fromByteArray(bufferWithPl);
					payLoadContainer.messageData = msgWithPl;
					payLoadContainer.senderPID = remPID;
					peerProcess.appendToMessageQ(payLoadContainer);
					dataBufferPl = null;
					bufferWithPl = null;
					seen = 0;
					seenBytes = 0;
				}
			}
		}
		catch(IOException err){
			// peerProcess.writeLog(myPID + " encountered an error while running: " + err);
		}
	}

}