import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;

/**
 * This class implements message configurations and handles Bit Field messages among peers.
 */
public class BitFieldMessage implements DefaultMessageConfiguration
{

	public DataPiece[] pieceList;
	public int pieceSize;

	/**
	 * Default constructor
	 */
	public BitFieldMessage()
	{
		pieceSize = (int) Math.ceil(((double) CommonConfiguration.FileSize / (double) CommonConfiguration.PieceSize));
		this.pieceList = new DataPiece[pieceSize];

		for (int i = 0; i < this.pieceSize; i++)
			this.pieceList[i] = new DataPiece();

	}
	/**
	 * Getter method for pieceSize.
	 */
	public int getPieceSize() {
		return pieceSize;
	}

	/**
	 * Setter method for pieceSize.
	 */
	public void setPieceSize(int pieceSize) {
		this.pieceSize = pieceSize;
	}

	/**
	 * Getter method for pieceList.
	 */
	public DataPiece[] getPieceList() {
		return pieceList;
	}

	/**
	 * Setter method for pieceList.
	 */
	public void setPieceList(DataPiece[] pieceList) {
		this.pieceList = pieceList;
	}
	/**
	 * Wrapper for fetchDataBytes method.
	 */
	public byte[] convertToBytes()
	{
		return this.fetchDataBytes();
	}

	/**
	 * This method converts the incoming byte list to the BitFieldMessage object.
	 * @param byteList
	 * @return BitFieldMessage
	 */
	public static BitFieldMessage convertFromBytes(byte[] byteList)
	{
		BitFieldMessage bitFieldMsg = new BitFieldMessage();
		for(int i = 0 ; i < byteList.length; i ++)
		{
			int numMsgTypes = 7;
			while(numMsgTypes >= 0)
			{
				int leftShift = 1 << numMsgTypes;
				if(i * 8 + (8-numMsgTypes-1) < bitFieldMsg.pieceSize)
				{
					if((byteList[i] & (leftShift)) != 0)
						bitFieldMsg.pieceList[i * 8 + (8-numMsgTypes-1)].doesPieceExist = 1;
					else
						bitFieldMsg.pieceList[i * 8 + (8-numMsgTypes-1)].doesPieceExist = 0;
				}
				numMsgTypes--;
			}
		}
		
		return bitFieldMsg;
	}

	/**
	 * This method compares the incoming BitFieldMessage obj with this object.
	 * @param BitFieldMessage obj
	 * @return boolean
	 */
	public synchronized boolean compare(BitFieldMessage bFieldMsg) {
		int bFieldMsgPieceSize = bFieldMsg.getPieceSize();
		

		for (int i = 0; i < bFieldMsgPieceSize; i++) {
			if (bFieldMsg.getPieceList()[i].getDoesPieceExist() == 1
					&& this.getPieceList()[i].getDoesPieceExist() == 0) {
				return true;
			} else
				continue;
		}

		return false;
	}

	/**
	 * This method finds first bit number not present with this instance but is present with
	 * the incoming BitFieldMessage obj.
	 * @param BitFieldMessage obj
	 * @return int
	 */
	public synchronized int firstDifference(BitFieldMessage bFieldMsg)
	{
		int pieceSize1 = this.getPieceSize();
		int pieceSize2 = bFieldMsg.getPieceSize();

		if (pieceSize1 >= pieceSize2) {
			for (int i = 0; i < pieceSize2; i++) {
				if (bFieldMsg.getPieceList()[i].getDoesPieceExist() == 1
						&& this.getPieceList()[i].getDoesPieceExist() == 0) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < pieceSize1; i++) {
				if (bFieldMsg.getPieceList()[i].getDoesPieceExist() == 1
						&& this.getPieceList()[i].getDoesPieceExist() == 0) {
					return i;
				}
			}
		}
		
		return -1;
	}

	/**
	 * This method gathers the data to a byte list.
	 * @return byte[]
	 */
	public byte[] fetchDataBytes()
	{
		int subPieceSize = this.pieceSize / 8;
		if (pieceSize % 8 != 0)
			subPieceSize = subPieceSize + 1;
		int c = 0;
		byte[] bList = new byte[subPieceSize];
		int counter;
		int t1 = 0;
		for (counter = 1; counter <= this.pieceSize; counter++)
		{
			int tPiece = this.pieceList[counter-1].doesPieceExist;
			t1 = t1 << 1;
			if (tPiece == 1)
			{
				t1++;
			}

			if (counter % 8 == 0 && counter != 0) {
				bList[c] = (byte) t1;
				c++;
				t1 = 0;
			}
		}
		if ((counter-1) % 8 != 0)
		{
			int ts = ((pieceSize) - (pieceSize / 8) * 8);
			t1 = t1 << (8 - ts);
			bList[c] = (byte) t1;
		}
		return bList;
	}

	/**
	 * This method is responsible for updating the info about this
	 * objects bitfield information.
	 * @param myPID
	 * @param isFilePresent
	 * */
	public void createMyBitField(String myPID, int isFilePresent) {

		// File does not exist.
		if (isFilePresent != 1) {
			for (int i = 0; i < this.pieceSize; i++) {
				this.pieceList[i].setDoesPieceExist(0);
				this.pieceList[i].setSenderPID(myPID);
			}

		}
		// File present.
		else {
			for (int i = 0; i < this.pieceSize; i++) {
				this.pieceList[i].setDoesPieceExist(1);
				this.pieceList[i].setSenderPID(myPID);
			}

		}

	}

	/**
	 * This method modifies the bit field data and the piece information in this object based on the incoming piece and
	 * the sender info.
	 * @param PID
	 * @param pieceObj
	 * */
	public synchronized void modifyBitField(String PID, DataPiece pieceObj) {
		try 
		{
			if (peerProcess.myBFM.pieceList[pieceObj.dataPieceIndex].doesPieceExist == 1) {
				peerProcess.writeLog(PID + " Piece has been received before.");
			} 
			else 
			{
				String fn = CommonConfiguration.FileName;
				File props = new File(peerProcess.PID, fn);
				int checkpoint = pieceObj.dataPieceIndex * CommonConfiguration.PieceSize;
				RandomAccessFile rand = new RandomAccessFile(props, "rw");
				byte[] bw;
				bw = pieceObj.dataPiece;
				rand.seek(checkpoint);
				rand.write(bw);
				this.pieceList[pieceObj.dataPieceIndex].setDoesPieceExist(1);
				this.pieceList[pieceObj.dataPieceIndex].setSenderPID(PID);
				rand.close();
				
				peerProcess.writeLog(peerProcess.PID + " received the piece " + pieceObj.dataPieceIndex + " from Peer " + PID + ". Total pieces holding is " + peerProcess.myBFM.myCurrentPieces());

				if (peerProcess.myBFM.isFinished())
				{
					peerProcess.peerInfoMap.get(peerProcess.PID).isInterested = 0;
					peerProcess.peerInfoMap.get(peerProcess.PID).isCompleted = 1;
					peerProcess.peerInfoMap.get(peerProcess.PID).isChoked = 0;
					modifyInfoConfig(peerProcess.PID, 1);
					peerProcess.writeLog(peerProcess.PID + " RECEIVED the whole file.");
				}
			}
		} catch (Exception exp) {
			peerProcess.writeLog(peerProcess.PID + " ERROR while modifying the bitfield " + exp.getMessage());
		}
	}

	/**
	 * This method returns the number of existing pieces this object contains.
	 * */
    public int myCurrentPieces()
    {
        int ct = 0;
        for (int i = 0; i < this.pieceSize; i++) {
			if (this.pieceList[i].doesPieceExist == 1) {
				ct++;
			}
		}
        return ct;
    }

    /**
	 * This methods checks if the downloading has been finished for a piece.
	 * It does so by checking the existing status of all the pieces it has.
	 * */
    public boolean isFinished() {
        for (int i = 0; i < this.pieceSize; i++) {
            if (this.pieceList[i].doesPieceExist == 0) {
                return false;
            }
        }
        return true;
    }

    /**
	 * This method updates the information for this peer in the PeerInfo.cfg file.
	 * @param PID
	 * @param isFilePresent
	 * */
	public void modifyInfoConfig(String PID, int isFilePresent)
	{
		BufferedReader iBuffer = null;
		BufferedWriter oBuffer = null;
		try
		{
			String record;
			StringBuffer sb = new StringBuffer();
			FileReader fr = new FileReader("PeerInfo.cfg");
			iBuffer = new BufferedReader(fr);

			while((record = iBuffer.readLine()) != null)
			{
				if(record.trim().split("\\s+")[0].equals(PID))
					sb.append(record.trim().split("\\s+")[0]
							+ " " + record.trim().split("\\s+")[1]
							+ " " + record.trim().split("\\s+")[2]
							+ " " + isFilePresent);
				else
					sb.append(record);
				sb.append("\n");
			}
			
			iBuffer.close();
			FileWriter fw = new FileWriter("PeerInfo.cfg");
			oBuffer= new BufferedWriter(fw);
			oBuffer.write(sb.toString());
			oBuffer.close();
		} 
		catch (Exception exp)
		{
			peerProcess.writeLog(PID + "ERROR while modifying peer config file " +  exp.getMessage());
		}
	}
	
	
	
}
