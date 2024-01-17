

import java.util.Comparator;

/**
 * This class implements a comparator and orders the data by its rate
 */
public class OrderByDataRate implements Comparator<PeerStateInfo> {

	private boolean toggleOrder;

	/**
	 * This is a default constructor
	 */
	public OrderByDataRate() {
		this.toggleOrder = true;
	}

	/**
	 * This is a parametrized constructor
	 * @param toggleOrder
	 */
	public OrderByDataRate(boolean toggleOrder) {
		this.toggleOrder = toggleOrder;
	}

	/**
	 * This method compares the remote peers by its data rate
	 */
	public int compare(PeerStateInfo state1, PeerStateInfo state2) {
		if (state1 == null && state2 == null)
			return 0;

		if (state1 == null)
			return 1;

		if (state2 == null)
			return -1;

		if (state1 instanceof Comparable) {
			if (toggleOrder)
				return state1.compareTo(state2);
			else
				return state2.compareTo(state1);
		} 
		else {
			if (toggleOrder)
				return state1.toString().compareTo(state2.toString());
			else
				return state2.toString().compareTo(state1.toString());
		}
	}
}
