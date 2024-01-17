

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * This class includes data transformation utility functions
 */
public class DataTransformUtils
{
    /**
     * Convert to bytes
     * @param input
     * @return byte array
     */
	public static byte[] ToBytes(int input)
	{
	    int cnst = 4;
        byte[] bytesArr = new byte[cnst];
        for (int i = 0; i < cnst; i++)
        {
            int os = (bytesArr.length - 1 - i) * 8;
            bytesArr[i] = (byte) ((input >>> os) & 0xFF);
        }
        return bytesArr;
    }

    /**
     * Converts to integer from byte array (Helper function)
     * @param byteArr
     * @return
     */
    public static int ToIntegerFromBytes(byte[] byteArr) {
	    int deflt = 0;
        return ToIntegerFromBytes(byteArr, deflt);
    }

    /**
     * Converts to integer from byte array
     * @param byteArr, offset/mask
     * @return
     */
    public static int ToIntegerFromBytes(byte[] bytesArr, int os)
    {
        int cnst = 4;
        int typesLen = 8;
        int output = 0;
        for (int i = 0; i < cnst; i++)
        {
            int mask = (cnst - 1 - i) * typesLen;
            output += (bytesArr[i + os] & 0x000000FF) << mask;
        }
        return output;
    }

    /**
     * Gets the current system time in string format
     * @return current system time
     */
    public static String getCurrentTime() {
        Calendar instance = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(instance.getTime());
    }
}
