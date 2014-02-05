import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class USBConnectorTestClient {
	private static final int DEFAULT_TARGET_PORT = 30012;

	public static void main(String[] args) {
		openMainFrame();
	}

	private static void openMainFrame() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		new MainFrameController("USBConnector Test Client", DEFAULT_TARGET_PORT);
	}
}
