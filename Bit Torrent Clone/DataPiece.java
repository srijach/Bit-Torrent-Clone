

/**
 * This class includes and handles the PIECE related information
 */
public class DataPiece
{
	public int doesPieceExist;
	public String senderPID;
	public byte [] dataPiece;
	public int dataPieceIndex;

	/**
	 * Getter for Piece existance
	 * @return
	 */
	public int getDoesPieceExist() {
		return doesPieceExist;
	}

	/**
	 * Setter for Piece existance
	 * @param doesPieceExist
	 */
	public void setDoesPieceExist(int doesPieceExist) {
		this.doesPieceExist = doesPieceExist;
	}

	/**
	 * Getter for sender peer Id
	 * @return
	 */
	public String getSenderPID() {
		return senderPID;
	}

	/**
	 * Setter for sender peer Id
	 * @param senderPID
	 */
	public void setSenderPID(String senderPID) {
		this.senderPID = senderPID;
	}

	/**
	 * Default constructor
	 */
	public DataPiece()
	{
		dataPiece = new byte[CommonConfiguration.PieceSize];
		dataPieceIndex = -1;
		doesPieceExist = 0;
		senderPID = null;
	}
	/**
	 * Gets the payload bytes and returns the Piece
	 * @param payloadData
	 * @return piece index
	 */
	public static DataPiece extractDataPieceFromPayload(byte[] payloadData) {
		byte[] bi = new byte[DefaultMessageConfiguration.DATA_PIECE_INDEX_LENGTH];
		DataPiece piece = new DataPiece();
		System.arraycopy(payloadData, 0, bi, 0, DefaultMessageConfiguration.DATA_PIECE_INDEX_LENGTH);
		piece.dataPieceIndex = DataTransformUtils.ToIntegerFromBytes(bi);
		piece.dataPiece = new byte[payloadData.length- DefaultMessageConfiguration.DATA_PIECE_INDEX_LENGTH];
		System.arraycopy(payloadData, DefaultMessageConfiguration.DATA_PIECE_INDEX_LENGTH, piece.dataPiece, 0, payloadData.length- DefaultMessageConfiguration.DATA_PIECE_INDEX_LENGTH);
		return piece;
	}
}
