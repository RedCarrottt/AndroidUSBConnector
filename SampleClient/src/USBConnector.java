import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

// It abstracts USB communication behaviors between Host PC and Android target device.
public class USBConnector {
	private static final String TAG = "USBConnector";

	private Worker mWorker;
	private boolean mIsServer;
	private int mPort;

	private MessageBuffer mSendBuffer;
	private ArrayList<USBMessageListener> mListeners;

	private USBConnector(boolean isServer, int port) {
		this.mIsServer = isServer;
		this.mPort = port;

		this.mListeners = new ArrayList<USBMessageListener>();
		this.mSendBuffer = new MessageBuffer();
	}

	public static USBConnector server(int selfPort) {
		return new USBConnector(true, selfPort);
	}

	public static USBConnector client(int targetPort) {
		return new USBConnector(false, targetPort);
	}

	public void start() {
		this.stop();
		this.mWorker = new Worker(this.mIsServer, this.mPort);
		this.mWorker.start();
	}

	public void stop() {
		if (this.mWorker != null) {
			this.mWorker.kill();
			try {
				this.mWorker.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendMessage(String message) {
		if (this.mWorker != null && this.mWorker.isRunning() == true) {
			this.mSendBuffer.add(message);
		}
	}

	public int getSendBufferLength() {
		return this.mSendBuffer.getLength();
	}

	public void addListener(USBMessageListener listener) {
		this.mListeners.add(listener);
	}

	public void removeListener(USBMessageListener listener) {
		this.mListeners.remove(listener);
	}

	// Listen messages incoming from target and send messages of buffer to
	// target periodically
	class Worker extends Thread {
		private static final String THREAD_NAME = "USBConnectorThread";
		private static final int SLEEP_MILLISECONDS = 2000;
		private int mPort;
		private boolean mIsRunning;
		private boolean mIsServer;

		public Worker(boolean isServer, int port) {
			super(THREAD_NAME);
			this.mIsServer = isServer;
			this.mPort = port;
			this.mIsRunning = false;
		}

		public boolean isRunning() {
			return this.mIsRunning;
		}

		public void run() {
			if (this.mIsServer) {
				this.runServer();
			} else {
				this.runClient();
			}
		}

		private void runServer() {
			this.mIsRunning = true;
			Log.i(TAG, "USBServerThread is started");

			ObjectInputStream inStream = null;
			ObjectOutputStream outStream = null;
			ServerSocket self = null;
			Socket target = null;

			try {
				// Initialize this server(self)
				int selfPort = this.mPort;
				self = new ServerSocket(selfPort);
				self.setSoTimeout(SLEEP_MILLISECONDS);

				while (this.mIsRunning == true) {
					try {
						// establish connection from target
						target = self.accept();
						if (target.getInputStream() == null) {
							Log.e(TAG, "Void input stream!");
							break;
						}
						inStream = new ObjectInputStream(
								target.getInputStream());

						// listen a message from target and broadcast it to
						// listeners
						String inMsg = (String) inStream.readObject();
						for (USBMessageListener listener : mListeners) {
							listener.onUSBMessage(inMsg);
						}
						Log.d(TAG, "LISTEN: " + inMsg);

						// pop all messages from send buffer and send them if
						// possible
						if (target.getOutputStream() == null) {
							Log.e(TAG, "Void output stream!");
							break;
						}
						outStream = new ObjectOutputStream(
								target.getOutputStream());
						final String sendMsg = mSendBuffer.popAll();
						if (sendMsg != null) {
							outStream.writeObject(sendMsg);
							outStream.flush();

							Log.d(TAG, "RESPOND: " + sendMsg);
						}
					} catch (SocketTimeoutException e) {
						// Connection timeout: no need for exception process
					} catch (ClassNotFoundException e) {
						// Class not found
						Log.e(TAG,
								"Read message error: ClassNotFoundException "
										+ e);
					} finally {
						// Close connection with target
						try {
							if (outStream != null)
								outStream.close();
							if (inStream != null)
								inStream.close();
							if (target != null)
								target.close();
						} catch (IOException e) {
							Log.e(TAG, "Close failure: " + e);
						}
					}
				}

				// Close self
				if (self != null)
					self.close();
			} catch (IOException e) {
				Log.e(TAG, "" + e);
			}

			Log.i(TAG, "USBServerThread is finished");
		}

		private void runClient() {
			this.mIsRunning = true;
			Log.i(TAG, "USBClientThread is started");

			ObjectInputStream inStream = null;
			ObjectOutputStream outStream = null;
			Socket target = null;

			while (this.mIsRunning == true) {
				// Check if there is any message to be sent in the buffer
				final String sendMsg = mSendBuffer.popAll();

				// If there is any message, send it and receive messages from
				// target
				if (sendMsg != null) {
					try {
						// Establish connection with target
						int targetPort = this.mPort;
						target = new Socket("localhost", targetPort);
						if (target.getOutputStream() == null) {
							Log.e(TAG, "Void output stream!");
							break;
						}
						outStream = new ObjectOutputStream(
								target.getOutputStream());

						// Send the messages
						outStream.writeObject(sendMsg);
						outStream.flush();
						Log.d(TAG, "SEND: " + sendMsg);

						// Listen messages from target
						if (target.getInputStream() == null) {
							Log.e(TAG, "Void input stream!");
							break;
						}
						inStream = new ObjectInputStream(
								target.getInputStream());
						String inMsg = (String) inStream.readObject();
						for (USBMessageListener listener : mListeners) {
							listener.onUSBMessage(inMsg);
						}
						Log.d(TAG, "RECEIVE: " + inMsg);

						// Close connection with target
						if (outStream != null)
							outStream.close();
						if (inStream != null)
							inStream.close();
						if (target != null)
							target.close();
					} catch (ClassNotFoundException e) {
						// Class not found
						Log.e(TAG,
								"Read message error: ClassNotFoundException "
										+ e);
					} catch (IOException e) {
						Log.e(TAG, "" + e);
					} finally {
						// Close connection with target
						try {
							if (outStream != null)
								outStream.close();
							if (inStream != null)
								inStream.close();
							if (target != null)
								target.close();
						} catch (IOException e) {
							Log.e(TAG, "Close failure: " + e);
						}
					}
				}
			}
		}

		public void kill() {
			this.mIsRunning = false;
		}
	}

	class MessageBuffer {
		private Boolean mLock = false;
		private ArrayList<String> mMessages;

		public MessageBuffer() {
			this.mMessages = new ArrayList<String>();
		}

		public void add(String message) {
			while (this.getLock() == false)
				;
			this.mMessages.add(message);
			this.putLock();
		}

		public String popAll() {
			String result = "";

			while (this.getLock() == false)
				;
			for (String msg : this.mMessages) {
				result = result + "\n" + msg;
			}
			this.mMessages.clear();
			this.putLock();

			if (result.length() == 0) {
				result = null;
			}
			return result;
		}

		public int getLength() {
			int length;

			while (this.getLock() == false)
				;
			length = this.mMessages.size();
			this.putLock();

			return length;
		}

		private boolean getLock() {
			synchronized (this.mLock) {
				if (this.mLock == false) {
					this.mLock = true;
					return true;
				} else {
					return false;
				}
			}
		}

		private void putLock() {
			synchronized (this.mLock) {
				this.mLock = false;
			}
		}
	}
}

interface USBMessageListener {
	// 'messages' string includes multiple messages divided by '\n'
	public void onUSBMessage(String messages);
}

class Log {
	public static void d(String tag, String message) {
		System.out.println("[DEBUG] " + tag + ": " + message);
	}

	public static void e(String tag, String message) {
		System.out.println("[ERROR] " + tag + ": " + message);
	}

	public static void i(String tag, String message) {
		System.out.println("[INFO] " + tag + ": " + message);
	}
}

