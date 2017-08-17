package chatProgram;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class ServerMain extends Application implements Runnable {

	private static Server server;
	private static int socket;
	private static String ip;
	private Thread serverThread;

	public static void main(String[] args) {
		socket = Params.socket;
		if (args.length > 0) {
			try {
				socket = Integer.parseInt(args[0]);
			} catch (Exception ignore) {
			}
		}
		ip = getMyIP();
		server = new Server(socket);
		launch(args);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void start(Stage primaryStage) throws Exception {
		serverThread = new Thread(this);
		serverThread.start();

		primaryStage.setTitle("Server Running on " + ip + ":" + socket);
		BorderPane pane = new BorderPane();
		GridPane main_pane = new GridPane();
		main_pane.setCenterShape(true);

		Button closeButton = new Button();
		closeButton.setText("Stop server");
		closeButton.setMaxWidth(100);
		main_pane.add(closeButton, 1, 0);

		closeButton.setOnAction(e -> {
			serverThread.stop();
			Platform.exit();
			System.exit(0);
		});

		pane.setCenter(closeButton);
		primaryStage.setScene(new Scene(pane, 400, 60));
		primaryStage.setResizable(false);
		primaryStage.setOnCloseRequest(e -> {
			serverThread.stop();
			Platform.exit();
			System.exit(0);
		});
		primaryStage.show();
	}

	@Override
	public void run() {
		try {
			server.setUpNetworking();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String getMyIP() {
		String ip = "";
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				if (iface.isLoopback() || !iface.isUp() || iface.getDisplayName().contains("Virtual"))
					continue;

				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					ip = addr.getHostAddress();
					System.out.println(iface.getDisplayName() + " " + ip);
				}
			}
		} catch (SocketException ignore) {
		}
		return ip;
	}
}
