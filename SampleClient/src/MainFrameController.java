import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainFrameController implements ActionListener, USBMessageListener,
		ADBConnectionListener {
	private static final int INIT_WINDOW_WIDTH = 300;
	private static final int INIT_WINDOW_HEIGHT = 200;
	private static final int INIT_WINDOW_X = 200;
	private static final int INIT_WINDOW_Y = 200;

	private ADBConnector mADBConnector;
	private USBConnector mUSBConnector;

	public MainFrameController(String title, int targetPort) {
		this.mUSBConnector = USBConnector.client(targetPort);
		this.initFrame(title);
		this.mADBConnector = ADBConnector.get(targetPort);
		this.mADBConnector.addListener(this);
		this.mADBConnector.start();
	}

	private String mTitle;
	private JFrame mFrame;
	private JPanel mMainPanel;
	private JButton mSendMsgButton;

	private void initFrame(String title) {
		this.mTitle = title;
		this.mFrame = new JFrame();
		this.mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.mFrame.setTitle(this.mTitle);
		this.mFrame.setSize(INIT_WINDOW_WIDTH, INIT_WINDOW_HEIGHT);
		this.mFrame.setLocation(INIT_WINDOW_X, INIT_WINDOW_Y);
		this.initMainPanel();
		this.mFrame.setVisible(true);
	}

	private void initMainPanel() {
		this.mMainPanel = new JPanel();
		this.mMainPanel.setLayout(new BoxLayout(mMainPanel, BoxLayout.Y_AXIS));
		this.mFrame.getContentPane().add(this.mMainPanel);

		this.mSendMsgButton = new JButton("Send a message to target");
		this.mSendMsgButton.addActionListener(this);
		this.mMainPanel.add(this.mSendMsgButton);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(this.mSendMsgButton)) {
			String msg = "THIS IS MESSAGE FROM HOST PC";
			System.out.println("Send Message: " + msg);
			this.mUSBConnector.sendMessage(msg);
		}
	}

	@Override
	public void onUSBMessage(String messages) {
		System.out.println("Received Messages: (FROM THIS)\n" + messages
				+ "\n(TO THIS)\n");
	}

	@Override
	public void onADBConnect() {
		this.mUSBConnector.start();
	}

	@Override
	public void onADBDisconnect() {
		this.mUSBConnector.stop();
	}
}
