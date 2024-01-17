
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * This class contains payload properties and functions
 */
public class PayLoad implements DefaultMessageConfiguration
{
    private String payloadType;
    private String payloadLen;
	private byte[] payload = null;
	private int messageLenght = TYPE_DATA_MSG;
	private byte[] msgCategory = null;
	private byte[] size = null;

	/**
	 * Default constructor
	 */
	public PayLoad() { }

	/**
	 * Parametrized constructor
	 * @param msgBit
	 */
    public PayLoad(String msgBit) {
        try {
            if (Arrays.asList(CHOKE_DATA_MSG, UNCHOKE_DATA_MSG, INTERESTED_DATA_MSG, NOTINTERESTED_DATA_MSG).contains(msgBit))
            {
                this.setPayloadLen(1);
                this.setPayloadType(msgBit);
                this.payload = null;
            }
            else
                throw new Exception("ERROR in constructor::PayLoad");
        } catch (Exception exp) {
            peerProcess.writeLog(exp.toString());
        }
    }

	/**
	 * Parametrized constructor
	 * @param msgBit
	 * @param byteData
	 */
	public PayLoad(String msgBit, byte[] byteData)
	{
		try 
		{
			if (byteData != null)
			{
                this.setPayloadLen(byteData.length + 1);
                if (this.size.length > LENGTH_DATA_MSG)
                    throw new Exception("PayLoad Size Too Large...");
                this.setPayload(byteData);
			} 
			else
			{
                if (Arrays.asList(CHOKE_DATA_MSG, UNCHOKE_DATA_MSG, INTERESTED_DATA_MSG, NOTINTERESTED_DATA_MSG).contains(msgBit))
                {
                    this.setPayloadLen(1);
                    this.payload = null;
                }
                else
                    throw new Exception("Payload is NULL...");
			}
			this.setPayloadType(msgBit);
			if (this.getPayloadType().length > TYPE_DATA_MSG)
				throw new Exception("Payload Size too large...");
		} catch (Exception e) {
			peerProcess.writeLog(e.toString());
		}
	}

	/**
	 * This method sets the payload message length
	 * @param payloadLen
	 */
	public void setPayloadLen(int payloadLen) {
        this.messageLenght = payloadLen;
        this.payloadLen = ((Integer) payloadLen).toString();
        this.size = DataTransformUtils.ToBytes(payloadLen);
    }

	/**
	 * This method sets the payload message length
	 * @param bytes
	 */
	public void setMessageLength(byte[] bytes) {
		Integer temp = DataTransformUtils.ToIntegerFromBytes(bytes);
		this.payloadLen = temp.toString();
		this.size = bytes;
		this.messageLenght = temp;
	}

	/**
	 * This method returns the length/size of payload message
	 */
	public byte[] getPayloadLen() {
		return size;
	}

	/**
	 * This method return the message length
	 */
	public int getMessageLengthInt() {
		return this.messageLenght;
	}

	/**
	 * This method sets the payload message type
	 */
	public void setPayloadType(byte[] byteArr) {
		try {
			this.payloadType = new String(byteArr, MESSAGE_CHARSET);
			this.msgCategory = byteArr;
		} catch (UnsupportedEncodingException e) {
			peerProcess.writeLog(e.toString());
		}
	}

	/**
	 * This method sets the payload message type
	 */
	public void setPayloadType(String msgBit) {
		try {
			this.payloadType = msgBit.trim();
			this.msgCategory = this.payloadType.getBytes(MESSAGE_CHARSET);
		} catch (UnsupportedEncodingException e) {
			peerProcess.writeLog(e.toString());
		}
	}

	/**
	 * getter method for payload type
	 */
	public byte[] getPayloadType() {
		return msgCategory;
	}

	/**
	 * setter method for payload message
	 */
	public void setPayload(byte[] message) {
		this.payload = message;
	}

	/**
	 * getter message for payload
	 */
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Setter method for payload message type
	 */
	public String getPayloadTypeString() {
		return payloadType;
	}

	/**
	 * This method generates a payload message
	 */
	public String toString() {
		String tempString = null;
		try {
			tempString = "[Payload] : Payload Length - " + this.payloadLen + ", Payload Type - " + this.payloadType + ", Message - " + (new String(this.payload, MESSAGE_CHARSET)).toString().trim();
		} catch (UnsupportedEncodingException e) {
			peerProcess.writeLog(e.toString());
		}
		return tempString;
	}

	/**
	 * This method converts the message into byte array
	 * @param msg
	 */
    public static byte[] toByteArray(PayLoad msg)
    {
        byte[] result = null;
        int msgBit;
        try
        {
            msgBit = Integer.parseInt(msg.getPayloadTypeString());
            if (msg.getPayloadLen().length > LENGTH_DATA_MSG)
                throw new Exception("Payload length not valid");
			else if (msg.getPayloadLen() == null)
				throw new Exception("Payload length not valid");
            else if (msgBit < 0 || msgBit > 7)
                throw new Exception("Payload type not valid");
            else if (msg.getPayloadType() == null)
                throw new Exception("Payload type not valid");

            if (msg.getPayload() != null) {
                result = new byte[LENGTH_DATA_MSG + TYPE_DATA_MSG + msg.getPayload().length];
                System.arraycopy(msg.getPayloadLen(), 0, result, 0, msg.getPayloadLen().length);
                System.arraycopy(msg.getPayloadType(), 0, result, LENGTH_DATA_MSG, TYPE_DATA_MSG);
                System.arraycopy(msg.getPayload(), 0, result, LENGTH_DATA_MSG + TYPE_DATA_MSG, msg.getPayload().length);
            }
            else {
                result = new byte[LENGTH_DATA_MSG + TYPE_DATA_MSG];
                System.arraycopy(msg.getPayloadLen(), 0, result, 0, msg.getPayloadLen().length);
                System.arraycopy(msg.getPayloadType(), 0, result, LENGTH_DATA_MSG, TYPE_DATA_MSG);
            }
        }
        catch (Exception e)
        {
            peerProcess.writeLog(e.toString());
            result = null;
        }
        return result;
    }

	/**
	 * This method converts the byte array back to payload message
	 */
	public static PayLoad fromByteArray(byte[] Message) {
		PayLoad message = new PayLoad();
		byte[] payLoad = null;
		byte[] payloadLen = new byte[LENGTH_DATA_MSG];
		byte[] payloadType = new byte[TYPE_DATA_MSG];
		int l;
		try 
		{
			if (Message == null)
				throw new Exception("Message not valid");
			else if (Message.length < LENGTH_DATA_MSG + TYPE_DATA_MSG)
				throw new Exception("Too small payload");
			System.arraycopy(Message, 0, payloadLen, 0, LENGTH_DATA_MSG);
			System.arraycopy(Message, LENGTH_DATA_MSG, payloadType, 0, TYPE_DATA_MSG);
			message.setMessageLength(payloadLen);
			message.setPayloadType(payloadType);
			l = DataTransformUtils.ToIntegerFromBytes(payloadLen);
			if (l > 1)
			{
				payLoad = new byte[l-1];
				System.arraycopy(Message,
						LENGTH_DATA_MSG + TYPE_DATA_MSG,
						payLoad, 0,
						Message.length - LENGTH_DATA_MSG - TYPE_DATA_MSG);
				message.setPayload(payLoad);
			}
			payLoad = null;
		} 
		catch (Exception e) 
		{
			peerProcess.writeLog(e.toString());
			message = null;
		}
		return message;
	}
}
