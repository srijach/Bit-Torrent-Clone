
import java.util.Date;

/**
 * This class maintains the peer's state information
 */
public class PeerStateInfo implements Comparable<PeerStateInfo>
{
	public String PID;
	public String port;
	public String address;
	public BitFieldMessage bitField;
	public Date start;
	public Date finish;
	public int isInvokedFirst;
	public int isInterested = 1;
	public int isChoked = 1;
	public int isOptimisticUnchoked = 0;
	public int isFavorableNbr = 0;
	public int isCompleted = 0;
	public int Index;
	public int state = -1;
	public int isHandShakeDone = 0;
	public double rate = 0;

	/**
	 * Parameterized constructor without isInvokedFirst param
	 * @param id
	 * @param addr
	 * @param portNum
	 * @param ind
	 */
	public PeerStateInfo(String id, String addr, String portNum, int ind)
	{
		PID = id;
		address = addr;
		port = portNum;
		bitField = new BitFieldMessage();
		Index = ind;
	}

	/**
	 * Parameterized constructor with isInvokedFirst param
	 * @param id
	 * @param addr
	 * @param portNum
	 * @param isInvokedFirst
	 * @param ind
	 */
	public PeerStateInfo(String id, String addr, String portNum, int isInvokedFirst, int ind)
	{
		PID = id;
		address = addr;
		port = portNum;
		this.isInvokedFirst = isInvokedFirst;
		bitField = new BitFieldMessage();
		Index = ind;
	}

	/**
	 * Getter for peer id
	 * @return peerId
	 */
	public String getPID() {
		return PID;
	}

	/**
	 * Setter for peer id
	 * @param PeerID
	 */
	public void setPID(String PID) {
		this.PID = PID;
	}

	/**
	 * Getter for peer IP address
	 * @return IP
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * Setter for peer IP address
	 * @param IPaddress
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * Getter for peer port number
	 * @return portnumber
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Setter for peer port number
	 * @param port number
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * Getter for first invoked peer
	 * @return
	 */
	public int getIsInvokedFirst() {
		return isInvokedFirst;
	}

	/**
	 * Setter for first invoked peer
	 * @param isInvokedFirst
	 */
	public void setIsInvokedFirst(int isInvokedFirst) {
		this.isInvokedFirst = isInvokedFirst;
	}

	/**
	 * Compare method in the comparable class;
	 * used for comparing data rates
	 * @param o1
	 * @return
	 */
	public int compareTo(PeerStateInfo o1) {
		if (this.rate > o1.rate) return 1;
		else if (this.rate == o1.rate) return 0;
		else return -1;
	}
}
