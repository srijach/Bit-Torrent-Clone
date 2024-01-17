
import java.io.*;

/**
 * This class defines the variables and methods required for handshake functionality
 */
public class HandShake implements DefaultMessageConfiguration
{
	private byte[] handshakeHeader = new byte[HEADER_LENGTH_HANDSHAKE];
	private byte[] zeroBits = new byte[ZEROBITS_LENGTH_HANDSHAKE];
	private byte[] PID = new byte[PEERID_LENGTH_HANDSHAKE];
	private String msgHead;
	private String msgPID;

	/**
	 * default constructor
	 */
	public HandShake(){ }

	/**
	 * default parametrized constructor
	 * @param String header, String pid
	 */
	public HandShake(String header, String pid) {
		try {
			this.msgHead = header;
			this.handshakeHeader = header.getBytes(MESSAGE_CHARSET);
			if (this.handshakeHeader.length > HEADER_LENGTH_HANDSHAKE)
				throw new Exception("Too Large Header");
			this.msgPID = pid;
			this.PID = pid.getBytes(MESSAGE_CHARSET);
			if (this.PID.length > HEADER_LENGTH_HANDSHAKE)
				throw new Exception("Too Large Peer ID");
			this.zeroBits = "0000000000".getBytes(MESSAGE_CHARSET);
		} catch (Exception e) {
			peerProcess.writeLog(e.toString());
		}
	}

	/**
	 * This method sets the handshake header
	 * @param handShakeHeader
	 */
	public void setHandshakeHeader(byte[] handShakeHeader) {
		try {
			this.msgHead = (new String(handShakeHeader, MESSAGE_CHARSET)).toString().trim();
			this.handshakeHeader = this.msgHead.getBytes();
		} catch (UnsupportedEncodingException e) {
			peerProcess.writeLog(e.toString());
		}
	}

	/**
	 * This method sets the peer Id.
	 * @param PID
	 */
	public void setPID(byte[] PID) {
		try {
			this.msgPID = (new String(PID, MESSAGE_CHARSET)).toString().trim();
			this.PID = this.msgPID.getBytes();

		} catch (UnsupportedEncodingException e) {
			peerProcess.writeLog(e.toString());
		}
	}

	/**
	 * This method sets the peer Id for a message
	 * @param pid
	 */
	public void setPID(String pid) {
		try {
			this.msgPID = pid;
			this.PID = pid.getBytes(MESSAGE_CHARSET);
		} catch (UnsupportedEncodingException e) {
			peerProcess.writeLog(e.toString());
		}
	}

	/**
	 * This method sets the message header
	 * @param mh
	 */
	public void setHeader(String mh) {
		try {
			this.msgHead = mh;
			this.handshakeHeader = mh.getBytes(MESSAGE_CHARSET);
		} catch (UnsupportedEncodingException e) {
			peerProcess.writeLog(e.toString());
		}
	}
	
	/**
	 * This method returns the handshake Header
	 */
	public byte[] getHandshakeHeader() {
		return handshakeHeader;
	}

	/**
	 * This method returns the peer Id
	 */
	public byte[] getPID() {
		return PID;
	}

	/**
	 * 	This method sets the zero bits
	 */
	public void setZeroBits(byte[] zeroBits) {
		this.zeroBits = zeroBits;
	}

	/**
	 * This method returns the zero Bits
	 */
	public byte[] getZeroBits() {
		return zeroBits;
	}

	/**
	 * This method returns the message Header
	 */
	public String getHeaderString() {
		return msgHead;
	}

	/**
	 * This method returns the message Peer ID
	 */
	public String getPeerIDString() {
		return msgPID;
	}

	/**
	 * This method returns the handshaking status in string format
	 */
	public String toString() {
		return ("[HandShaking Status] : Peer Id - " + this.msgPID + ", with header - " + this.msgHead);
	}

	/**
	 * This method converts handshake Message byte array to simple handshake Message
	 * @param input
	 */
	public static HandShake ToHandShakeType(byte[] input) {
		HandShake handShake = null;
		byte[] PIDmsg = null;
		byte[] headerMsg = null;

		try {
			if (input.length != MESSAGE_LENGTH_HANDSHAKE)
				throw new Exception("LENGTH MISMATCH -> ByteArray");

			handShake = new HandShake();
			headerMsg = new byte[HEADER_LENGTH_HANDSHAKE];
			PIDmsg = new byte[PEERID_LENGTH_HANDSHAKE];
			System.arraycopy(input, 0, headerMsg, 0, HEADER_LENGTH_HANDSHAKE);
			System.arraycopy(input, HEADER_LENGTH_HANDSHAKE + ZEROBITS_LENGTH_HANDSHAKE, PIDmsg, 0, PEERID_LENGTH_HANDSHAKE);

			handShake.setHandshakeHeader(headerMsg);
			handShake.setPID(PIDmsg);

		} catch (Exception e) {
			peerProcess.writeLog(e.toString());
			handShake = null;
		}
		return handShake;
	}

	/**
	 * This method converts a message into handshake message format (byte array format)
	 * @param hsMsg
	 */
	public static byte[] ToByteArray(HandShake hsMsg) {

		byte[] result = new byte[MESSAGE_LENGTH_HANDSHAKE];

		try {
			if (hsMsg.getHandshakeHeader() == null) {
				throw new Exception("Handshake header not valid");
			}
			if (hsMsg.getHandshakeHeader().length > HEADER_LENGTH_HANDSHAKE || hsMsg.getHandshakeHeader().length == 0)
			{
				throw new Exception("Handshake header not valid");
			} else {
				System.arraycopy(hsMsg.getHandshakeHeader(), 0, result, 0, hsMsg.getHandshakeHeader().length);
			}

			if (hsMsg.getZeroBits() == null) {
				throw new Exception("Zero Bits not valid");
			} 
			if (hsMsg.getZeroBits().length > ZEROBITS_LENGTH_HANDSHAKE || hsMsg.getZeroBits().length == 0) {
				throw new Exception("Zero Bits not valid");
			} else {
				System.arraycopy(hsMsg.getZeroBits(), 0, result, HEADER_LENGTH_HANDSHAKE, ZEROBITS_LENGTH_HANDSHAKE - 1);
			}

			if (hsMsg.getPID() == null)
			{
				throw new Exception("PEER ID not valid");
			} 
			else if (hsMsg.getPID().length > PEERID_LENGTH_HANDSHAKE || hsMsg.getPID().length == 0)
			{
				throw new Exception("PEER ID not valid");
			} 
			else 
			{
				System.arraycopy(hsMsg.getPID(), 0, result, HEADER_LENGTH_HANDSHAKE + ZEROBITS_LENGTH_HANDSHAKE, hsMsg.getPID().length);
			}
		} 
		catch (Exception e) 
		{
			peerProcess.writeLog(e.toString());
			result = null;
		}
		return result;
	}
}
