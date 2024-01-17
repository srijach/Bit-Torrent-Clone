

/**
 * This class includes payload wrapper which contains payload data and sender peer ID
 * It also manages setters and getters
 */
public class PayLoadContainer
{
	PayLoad messageData;
	String senderPID;

	/**
	 * Default constructor
	 */
	public PayLoadContainer()
	{
		messageData = new PayLoad();
		senderPID = null;
	}

	/**
	 * Setter for sender peer id
	 * @param senderPID
	 */
    public void setSenderPID(String senderPID) {
        this.senderPID = senderPID;
    }

	/**
	 * Setter for payload data
	 * @param messageData
	 */
	public void setMessageData(PayLoad messageData) {
        this.messageData = messageData;
    }

	/**
	 * Getter for payload data
	 * @return
	 */
	public PayLoad getMessageData() {
		return messageData;
	}

	/**
	 * Getter for sender peer id
	 * @return
	 */
	public String getSenderPID() {
		return senderPID;
	}

}
