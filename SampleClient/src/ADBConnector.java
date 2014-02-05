import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ADBConnector {
	private Worker mWorker;

	private int mTargetPort;
	private ArrayList<ADBConnectionListener> mListeners;

	private ADBConnector(int targetPort) {
		this.mTargetPort = targetPort;
		this.mListeners = new ArrayList<ADBConnectionListener>();
	}

	public static ADBConnector get(int targetPort) {
		return new ADBConnector(targetPort);
	}

	public void start() {
		this.mWorker = new Worker(this.mTargetPort);
		this.mWorker.start();
	}

	public void kill() {
		this.mWorker.kill();
		try {
			this.mWorker.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void addListener(ADBConnectionListener listener) {
		this.mListeners.add(listener);
	}

	public void removeListener(ADBConnectionListener listener) {
		this.mListeners.remove(listener);
	}

	private void openPort(int portNum) throws IOException {
		new ProcessBuilder("adb", "forward", ("tcp:" + portNum),
				("tcp:" + portNum)).start();
	}

	private boolean isUSBConnected() {
		return (getNumUSBConnected() > 0);
	}

	private int getNumUSBConnected() {
		try {
			// Check if target devices is connected to host
			Process devicesProcess;

			devicesProcess = new ProcessBuilder("adb", "devices").start();

			BufferedReader stdOut = new BufferedReader(new InputStreamReader(
					devicesProcess.getInputStream()));
			String outString;
			int connectedDevices = -1;
			while ((outString = stdOut.readLine()) != null) {
				// System.out.println(outString);
				if (outString.contains("device") == true) {
					connectedDevices++;
				}
			}
			if (outString != null || connectedDevices <= 0) {
				// System.err.println("Device is not connected!");
				return -1;
			}
			return connectedDevices;
		} catch (IOException e) {
			// System.err.println("I/O exception!");
			return -1;
		}
	}

	class Worker extends Thread {
		private static final String THREAD_NAME = "USBConnectorThread";
		private static final int SLEEP_MILLISECONDS = 1000;
		private int mTargetPort;

		private boolean mIsRunning;
		private boolean mLastConnected;

		public Worker(int targetPort) {
			super(THREAD_NAME);
			this.mTargetPort = targetPort;
			this.mLastConnected = false;
		}

		@Override
		public void run() {
			this.mIsRunning = true;
			this.mLastConnected = false;
			while (this.mIsRunning) {
				try {
					if (isUSBConnected() == true
							&& this.mLastConnected == false) {
						// Connected
						openPort(this.mTargetPort);
						this.mLastConnected = true;
						for (ADBConnectionListener listener : mListeners) {
							listener.onADBConnect();
						}
					} else if (isUSBConnected() == false
							&& this.mLastConnected == true) {
						// Disconnected
						this.mLastConnected = false;
						for (ADBConnectionListener listener : mListeners) {
							listener.onADBDisconnect();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(SLEEP_MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		public void kill() {
			this.mIsRunning = false;
		}
	}
}

interface ADBConnectionListener {
	public void onADBConnect();

	public void onADBDisconnect();
}
