
/**
 * This file contains the pre-defined system-wide constants in an interface
 * as mentioned in the project description.
 */
public interface DefaultMessageConfiguration {
	public static final String MESSAGE_CHARSET = "UTF8";

	public static final String CHOKE_DATA_MSG = "0";
	public static final String UNCHOKE_DATA_MSG = "1";
	public static final String INTERESTED_DATA_MSG = "2";
	public static final String NOTINTERESTED_DATA_MSG = "3";
	public static final String HAVE_DATA_MSG = "4";
	public static final String BITFIELD_DATA_MSG = "5";
	public static final String REQUEST_DATA_MSG = "6";
	public static final String PIECE_DATA_MSG = "7";
	public static final String HEADER_HANDSHAKE = "P2PFILESHARINGPROJ";
	public static final int LENGTH_DATA_MSG = 4;
	public static final int TYPE_DATA_MSG = 1;
	public static final int DATA_PIECE_INDEX_LENGTH = 4;

	public static final int MESSAGE_LENGTH_HANDSHAKE = 32;
	public static final int HEADER_LENGTH_HANDSHAKE = 18;
	public static final int ZEROBITS_LENGTH_HANDSHAKE = 10;
	public static final int PEERID_LENGTH_HANDSHAKE = 4;
}