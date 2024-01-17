
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * This class implements the functionality of generating logs
 */
public class Logger {
	static FileOutputStream fileOutputStream;
	static OutputStreamWriter streamWriter;

	/**
	 * This method starts the log generator by initializing output and input stream
	 * @param input
	 */
	public static void InitLogging(String input) throws IOException {
		fileOutputStream = new FileOutputStream(input);
		streamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
	}

	/**
	 * This method terminates the log generation and restores back to original setting
	 */
	public static void terminateLogging() {
		try {
			streamWriter.flush();
			fileOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method writes the logs to the file
	 * @param logmsg
	 */
	public static void saveLog(String logmsg) {
		try {
			streamWriter.write(logmsg + '\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
