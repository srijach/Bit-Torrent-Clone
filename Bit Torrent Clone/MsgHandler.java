
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;

/**
 * This class implements the functionality of message handler.
 */
public class MsgHandler implements Runnable, DefaultMessageConfiguration
{
	public static int peerState = -1;
	private static String peerID = null;
	private static boolean isActive = true;
	RandomAccessFile accessFile;

	/**
	 * This is a parametrized constructor
	 * @param pid
	 */
	public MsgHandler(String pid) {
		peerID = pid;
	}

	/**
	 * This is a default constructor
	 */
	public MsgHandler() { }

	/**
	 * This function writes log with respect to message processor
	 */
	public void showMeta(String dataType, int state) {
		peerProcess.writeLog("Message Processor : msgType = "+ dataType + " State = "+state);
	}

	/**
	 * method to run the implemented thread
	 */
	public void run() {
		String messageCategory;
		PayLoad pl;
		PayLoadContainer payLoadContainer;
		String remotePID;

		while(isActive)
		{
			payLoadContainer  = peerProcess.deleteFromMessageQ();
			while(payLoadContainer == null) {
				Thread.currentThread();
				try
				{
					Thread.sleep(500);
				}
				catch (InterruptedException err)
				{
				   err.printStackTrace();
				}
				payLoadContainer  = peerProcess.deleteFromMessageQ();
			}

			pl = payLoadContainer.getMessageData();
			messageCategory = pl.getPayloadTypeString();
			// messageCategory = pl.getMessageTypeString();
			remotePID = payLoadContainer.getSenderPID();

			int remotePeerState = peerProcess.peerInfoMap.get(remotePID).state;

			if(messageCategory.equals(HAVE_DATA_MSG) && remotePeerState != 14) {
				peerProcess.writeLog(peerProcess.PID + " received the HAVE message by Peer " + remotePID);
				if(checkIfInterested(pl, remotePID)) {
					passInterestedMsg(peerProcess.pIDSktMap.get(remotePID), remotePID);
					peerProcess.peerInfoMap.get(remotePID).state = 9;
				}
				else {
					passNotInterestedMsg(peerProcess.pIDSktMap.get(remotePID), remotePID);
					peerProcess.peerInfoMap.get(remotePID).state = 13;
				}
			}
			else {
			 switch (remotePeerState) {
			 case 2:
			   if (messageCategory.equals(BITFIELD_DATA_MSG))
			   {
		 		  peerProcess.writeLog(peerProcess.PID + " received a BITFIELD message from Peer " + remotePID);
	 			  passBitFieldMsg(peerProcess.pIDSktMap.get(remotePID), remotePID);
 				  peerProcess.peerInfoMap.get(remotePID).state = 3;
			   }
			   break;
			 case 3:
			   if (messageCategory.equals(NOTINTERESTED_DATA_MSG))
			   {
					peerProcess.writeLog(peerProcess.PID + " received a NOT INTERESTED message from Peer " + remotePID);
					peerProcess.peerInfoMap.get(remotePID).isInterested = 0;
					peerProcess.peerInfoMap.get(remotePID).state = 5;
					peerProcess.peerInfoMap.get(remotePID).isHandShakeDone = 1;
			   }
			   else if (messageCategory.equals(INTERESTED_DATA_MSG))
			   {
					peerProcess.writeLog(peerProcess.PID + " received an INTERESTED message from Peer " + remotePID);
					peerProcess.peerInfoMap.get(remotePID).isInterested = 1;
					peerProcess.peerInfoMap.get(remotePID).isHandShakeDone = 1;
					if(!peerProcess.prefNbrMap.containsKey(remotePID) && !peerProcess.unchokedNbrMap.containsKey(remotePID))
					{
						passChokeMsg(peerProcess.pIDSktMap.get(remotePID), remotePID);
						peerProcess.peerInfoMap.get(remotePID).isChoked = 1;
						peerProcess.peerInfoMap.get(remotePID).state  = 6;
					}
					else {
						peerProcess.peerInfoMap.get(remotePID).isChoked = 0;
						passUnchokeMsg(peerProcess.pIDSktMap.get(remotePID), remotePID);
						peerProcess.peerInfoMap.get(remotePID).state = 4 ;
					}
			   }
			   break;
			 case 4:
				 if (messageCategory.equals(REQUEST_DATA_MSG)) {
            peerProcess.writeLog(peerProcess.PID + " received a REQUEST message from Peer " + remotePID);
						transferDataPiece(peerProcess.pIDSktMap.get(remotePID), pl, remotePID);
						if(!peerProcess.prefNbrMap.containsKey(remotePID)
								&& !peerProcess.unchokedNbrMap.containsKey(remotePID)) {
							passChokeMsg(peerProcess.pIDSktMap.get(remotePID), remotePID);
							peerProcess.peerInfoMap.get(remotePID).isChoked = 1;
							peerProcess.peerInfoMap.get(remotePID).state = 6;
						}
				 }
				 break;
			 case 8:
				 if (messageCategory.equals(BITFIELD_DATA_MSG)) {
						if(checkIfInterested(pl,remotePID)) {
							passInterestedMsg(peerProcess.pIDSktMap.get(remotePID), remotePID);
							peerProcess.peerInfoMap.get(remotePID).state = 9;
						}
						else {
							passNotInterestedMsg(peerProcess.pIDSktMap.get(remotePID), remotePID);
							peerProcess.peerInfoMap.get(remotePID).state = 13;
						}
				 }
				 break;
			 case 9:
				 if (messageCategory.equals(CHOKE_DATA_MSG)) {
						peerProcess.writeLog(peerProcess.PID + " CHOKED by Peer " + remotePID);
						peerProcess.peerInfoMap.get(remotePID).state = 14;
				 }
				 else if (messageCategory.equals(UNCHOKE_DATA_MSG)) {
						peerProcess.writeLog(peerProcess.PID + " UN-CHOKED by Peer " + remotePID);
						int mismatchInd = peerProcess.myBFM.firstDifference(peerProcess.peerInfoMap.get(remotePID).bitField);
						if(mismatchInd != -1) {
							dispatchMessage(peerProcess.pIDSktMap.get(remotePID), mismatchInd, remotePID);
							peerProcess.peerInfoMap.get(remotePID).state = 11;
							peerProcess.peerInfoMap.get(remotePID).start = new Date();
						}
						else
							peerProcess.peerInfoMap.get(remotePID).state = 13;
				 }
				 break;
			 case 11:
				 if (messageCategory.equals(PIECE_DATA_MSG)) {
					    byte[] payloadBuff = pl.getPayload();
						peerProcess.peerInfoMap.get(remotePID).finish = new Date();
						long elapsedTime = peerProcess.peerInfoMap.get(remotePID).finish.getTime() - peerProcess.peerInfoMap.get(remotePID).start.getTime();
						int totalData = payloadBuff.length + LENGTH_DATA_MSG + TYPE_DATA_MSG;
						peerProcess.peerInfoMap.get(remotePID).rate = ((double)(totalData)/(double)elapsedTime) * 100;

						DataPiece dataPiece = DataPiece.extractDataPieceFromPayload(payloadBuff);
						peerProcess.myBFM.modifyBitField(remotePID, dataPiece);

						int nextPieceToFetchInd = peerProcess.myBFM.firstDifference(peerProcess.peerInfoMap.get(remotePID).bitField);
						if(nextPieceToFetchInd != -1) {
							dispatchMessage(peerProcess.pIDSktMap.get(remotePID),nextPieceToFetchInd, remotePID);
							peerProcess.peerInfoMap.get(remotePID).state  = 11;

							peerProcess.peerInfoMap.get(remotePID).start = new Date();
						}
						else
							peerProcess.peerInfoMap.get(remotePID).state = 13;

						peerProcess.getPeerInfoRepeat();

						Enumeration<String> peerStatesList = peerProcess.peerInfoMap.keys();
						while(peerStatesList.hasMoreElements())
						{
							String pState = (String) peerStatesList.nextElement();
							PeerStateInfo peerState = peerProcess.peerInfoMap.get(pState);

							if(pState.equals(peerProcess.PID))
								continue;

							if (peerState.isCompleted == 0 && peerState.isChoked == 0 && peerState.isHandShakeDone == 1) {
								passHaveMsg(peerProcess.pIDSktMap.get(pState), pState);
								peerProcess.peerInfoMap.get(pState).state = 3;
							}
						}

						payloadBuff = null;
						pl = null;
				 }
				 else if (messageCategory.equals(CHOKE_DATA_MSG)) {
						peerProcess.writeLog(peerProcess.PID + " CHOKED by Peer " + remotePID);
						peerProcess.peerInfoMap.get(remotePID).state = 14;
				 }
				 break;
			 case 14:
				 if (messageCategory.equals(HAVE_DATA_MSG)) {
						if(checkIfInterested(pl,remotePID)) {
							passInterestedMsg(peerProcess.pIDSktMap.get(remotePID), remotePID);
							peerProcess.peerInfoMap.get(remotePID).state = 9;
						}
						else {
							passNotInterestedMsg(peerProcess.pIDSktMap.get(remotePID), remotePID);
							peerProcess.peerInfoMap.get(remotePID).state = 13;
						}
				 }
				 else if (messageCategory.equals(UNCHOKE_DATA_MSG)) {
						peerProcess.writeLog(peerProcess.PID + " UN-CHOKED by Peer " + remotePID);
						peerProcess.peerInfoMap.get(remotePID).state = 14;
				 }
				 break;
			 }
			}
		}
	}

	/**
	 * This method implements the main functionality of data piece transfer
	 * @param skt
	 * @param pl
	 * @param remPId
	 */
	private void transferDataPiece(Socket skt, PayLoad pl, String remPId)
	{
		byte[] byteArr = pl.getPayload();
		int piecePosition = DataTransformUtils.ToIntegerFromBytes(byteArr);

		peerProcess.writeLog(peerProcess.PID + " transferring the piece " + piecePosition + " to the Peer " + remPId);

		byte[] receivedBytes = new byte[CommonConfiguration.PieceSize];
		int totalReceivedBytes = 0;

		File f = new File(peerProcess.PID, CommonConfiguration.FileName);
		try {
			accessFile = new RandomAccessFile(f,"r");
			accessFile.seek(piecePosition* CommonConfiguration.PieceSize);
			totalReceivedBytes = accessFile.read(receivedBytes, 0, CommonConfiguration.PieceSize);
		}
		catch (IOException e) {
			peerProcess.writeLog(peerProcess.PID + " error while reading the byte " +  e.toString());
		}
		if( totalReceivedBytes == 0){
			peerProcess.writeLog(peerProcess.PID + " No bytes read from the file");
		}
		else if (totalReceivedBytes < 0){
			peerProcess.writeLog(peerProcess.PID + " improper file load");
		}

		byte[] dataBuff = new byte[totalReceivedBytes + DefaultMessageConfiguration.DATA_PIECE_INDEX_LENGTH];
		System.arraycopy(byteArr, 0, dataBuff, 0, DefaultMessageConfiguration.DATA_PIECE_INDEX_LENGTH);
		System.arraycopy(receivedBytes, 0, dataBuff, DefaultMessageConfiguration.DATA_PIECE_INDEX_LENGTH, totalReceivedBytes);

		PayLoad dispatchedMsg = new PayLoad(PIECE_DATA_MSG, dataBuff);
		byte[] byteArray =  PayLoad.toByteArray(dispatchedMsg);
		passPayload(skt, byteArray);

		receivedBytes = null;
		dispatchedMsg = null;
		byteArray = null;
		byteArr = null;
		dataBuff = null;

		try
		{
			accessFile.close();
		}
		catch(Exception e){
			//nothing to do
		}
	}

	/**
	 * This method sends or dispatches the data
	 * @param skt
	 * @param pieceNumber
	 * @param remotePID
	 */
	private void dispatchMessage(Socket skt, int pieceNumber, String remotePID) {

		byte[] byteFormatPiece = new byte[DefaultMessageConfiguration.DATA_PIECE_INDEX_LENGTH];

		for (int i = 0; i < DefaultMessageConfiguration.DATA_PIECE_INDEX_LENGTH; i++) {
			byteFormatPiece[i] = 0;
		}

		byte[] byFormatPieceIndex = DataTransformUtils.ToBytes(pieceNumber);
		System.arraycopy(byFormatPieceIndex, 0, byteFormatPiece, 0, byFormatPieceIndex.length);
		PayLoad pl = new PayLoad(REQUEST_DATA_MSG, byteFormatPiece);
		byte[] plBytes = PayLoad.toByteArray(pl);
		passPayload(skt, plBytes);

		byFormatPieceIndex = null;
		byteFormatPiece = null;
		pl = null;
		plBytes = null;
	}

	/**
	 * This method sends the not interested message
	 * @param skt
	 * @param remPId
	 */
	private void passNotInterestedMsg(Socket skt, String remPId)
	{
		peerProcess.writeLog(peerProcess.PID + " passing NOT INTERESTED message to the Peer " + remPId);
		PayLoad pl =  new PayLoad(NOTINTERESTED_DATA_MSG);
		byte[] bytesPl = PayLoad.toByteArray(pl);
		passPayload(skt,bytesPl);
	}

	/**
	 * This method send the bit field message
	 * @param skt
	 * @param remPId
	 */
	private void passBitFieldMsg(Socket skt, String remPId) {
		peerProcess.writeLog(peerProcess.PID + " passing BITFIELD message to the Peer " + remPId);
		byte[] convertedByteArr = peerProcess.myBFM.convertToBytes();
		PayLoad pl = new PayLoad(BITFIELD_DATA_MSG, convertedByteArr);
		passPayload(skt, PayLoad.toByteArray(pl));
		convertedByteArr = null;
	}

	/**
	 * This method checks if the peer is interested
	 * @param pl
	 * @param remPId
	 */
	private boolean checkIfInterested(PayLoad pl, String remPId) {
		BitFieldMessage bfm = BitFieldMessage.convertFromBytes(pl.getPayload());
		peerProcess.peerInfoMap.get(remPId).bitField = bfm;

		if (peerProcess.myBFM.compare(bfm))
		{
			return true;
		}
		return false;
	}

	/**
	 * This is a default constructor
	 * @param skt
	 * @param remPId
	 */
	private void passUnchokeMsg(Socket skt, String remPId) {
		peerProcess.writeLog(peerProcess.PID + " passing UNCHOKE message to the Peer " + remPId);
		PayLoad pl = new PayLoad(UNCHOKE_DATA_MSG);
		byte[] bytesPl = PayLoad.toByteArray(pl);
		passPayload(skt,bytesPl);
	}

	/**
	 * This method sends the payload message
	 * @param skt
	 * @param convertedByteArr
	 */
	private int passPayload(Socket skt, byte[] convertedByteArr) {
		try {
			OutputStream outputStream = skt.getOutputStream();
			outputStream.write(convertedByteArr);
		} catch (IOException e)
		{
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	/**
	 * This method sends the interested message
	 * @param skt
	 * @param remPId
	 */
	private void passInterestedMsg(Socket skt, String remPId) {
		peerProcess.writeLog(peerProcess.PID + " passing INTERESTED message to the Peer " + remPId);
		PayLoad pl =  new PayLoad(INTERESTED_DATA_MSG);
		byte[] bytedPl = PayLoad.toByteArray(pl);
		passPayload(skt,bytedPl);
	}

	/**
	 * This method sens the choked message
	 * @param skt
	 * @param remPId
	 */
	private void passChokeMsg(Socket skt, String remPId) {
		peerProcess.writeLog(peerProcess.PID + " passing CHOKE message to the Peer " + remPId);
		PayLoad pl = new PayLoad(CHOKE_DATA_MSG);
		byte[] bytesPl = PayLoad.toByteArray(pl);
		passPayload(skt,bytesPl);
	}

	/**
	 * This method send the have data messgae
	 * @param remPId
	 * @param skt
	 */
	private void passHaveMsg(Socket skt, String remPId) {
		peerProcess.writeLog(peerProcess.PID + " passing HAVE message to the Peer " + remPId);
		byte[] convertedByteArr = peerProcess.myBFM.convertToBytes();
		PayLoad pl = new PayLoad(HAVE_DATA_MSG, convertedByteArr);
		passPayload(skt, PayLoad.toByteArray(pl));
		convertedByteArr = null;
	}
}
