package chatProgram;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientMain extends Application {
	//stop is responsible for telling the server when the client has closed the application
	public static AtomicBoolean stop = new AtomicBoolean(false);
	//reader and writer for the Java socket
	public static BufferedReader reader;
	public static PrintWriter writer;
	//list_chatters and list_chats are the viewable lists that the user can see
	//items_chatters and items_chats are the items that are within those lists
	public static ListView<String> list_chatters = new ListView<String>();
	public static ListView<String> list_chats = new ListView<String>();
	public static ObservableList<String> items_chatters = FXCollections.observableArrayList();
	public static ObservableList<String> items_chats = FXCollections.observableArrayList("Main");
	//selected_people is responsible for keeping track of people the user selected to create a private room
	public static Set<String> selected_people = new HashSet<String>();
	//nickname is the user's chosen name and target is their current selected channel
	public static String nickname = null;
	public static String target = "Main";
	//chat is where the chat between users is displayed
	public static TextArea chat = new TextArea();
	//private variables for socket and ip which are taken from parameters
	private static int socket;
	private static String ip;

	
	public static void main(String[] args) {
		socket = Params.socket;
		ip = Params.ip_address;
		//if parameters are present for custom ip and socket, otherwise it is run on localhost
		if (args.length > 1) {
			try {
				ip = args[0];
				socket = Integer.parseInt(args[1]);
				System.out.println("Client connecting to " + ip + ":" + socket);
			} catch (Exception ignore) {}
		}
		launch(args);
	}

	private void setUpNetworking() throws Exception {
		@SuppressWarnings("resource")
		Socket sock = new Socket(ip, socket);
		InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
		reader = new BufferedReader(streamReader);
		writer = new PrintWriter(sock.getOutputStream());
		System.out.println("Client successfully established a connection.");
		Thread readerThread = new Thread(new IncomingReader());
		readerThread.start();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			setUpNetworking();
		} catch (Exception e) {
			e.printStackTrace();
		}

		primaryStage.setTitle("Chat Client");
		BorderPane pane = new BorderPane();
		GridPane user_pane = new GridPane();
		user_pane.setCenterShape(true);

		list_chatters.setItems(items_chatters);
		list_chatters.setMaxSize(100, 300);
		list_chatters.setEditable(true);
		list_chatters.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		list_chats.setItems(items_chats);
		list_chats.setMaxSize(100, 300);
		list_chats.setEditable(true);

		Button sendButton = new Button();
		sendButton.setText("Set Nickname");
		sendButton.setMaxWidth(100);

		Button privateButton = new Button();
		privateButton.setText("Create Private Group");
		privateButton.setMaxWidth(200);

		TextField userText = new TextField();
		userText.setPromptText("Please input nickname. (No Spaces Allowed)");
		userText.setMaxWidth(383);
		userText.setMinWidth(383);

		user_pane.add(userText, 0, 0);
		user_pane.add(sendButton, 1, 0);
		user_pane.add(privateButton, 2, 0);

		chat.setMaxSize(600, 300);
		chat.setEditable(false);
		chat.setWrapText(true);

		sendButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				String text = userText.getText();
				String[] text_parse = text.split(" ");
				if (text_parse.length != 1 && nickname == null) {
					return;
				} else if (nickname == null) {
					nickname = text;
					primaryStage.setTitle("Chat Client - " + nickname);
					userText.setText("");
					userText.setPromptText("Type to send message.");
					sendButton.setText("Send Message");
				} else {
					writer.println(nickname + " " + target + " : " + text);
					writer.flush();
					userText.setText("");
				}
			}
		});

		privateButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				getSelectedChatters();
				if (selected_people.size() > 0) {
					list_chatters.getSelectionModel().clearSelection();
					String selected_people_string = nickname + " ";
					String[] arr = selected_people.toArray(new String[0]);
					for (int i = 0; i < arr.length; i++) {
						selected_people_string += arr[i] + " ";
					}
					writer.println("GET_ALL_CHATS " + selected_people_string);
					writer.flush();
				}
			}
		});

		pane.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
			if (ev.getCode() == KeyCode.ENTER) {
				String text = userText.getText();
				String[] text_parse = text.split(" ");
				if (text_parse.length != 1 && nickname == null) {
					return;
				} else if (nickname == null) {
					nickname = text;
					primaryStage.setTitle("Chat Client - " + nickname);
					userText.setText("");
					userText.setPromptText("Type to send message.");
					sendButton.setText("Send Message");
				} else {
					writer.println(nickname + " " + target + " : " + text);
					writer.flush();
					userText.setText("");
				}
			}
		});

		list_chats.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 1) {
					String x = list_chats.getSelectionModel().getSelectedItem();
					target = x.replaceAll(" ", "");
					writer.println("UPDATE_CHAT " + target);
					writer.flush();
				}
			}
		});

		// keeps up with the current online clients
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!stop.get()) {
					try {
						writer.println("GET_ALL_CLIENTS " + target);
						writer.flush();
						Thread.sleep(200);
					} catch (InterruptedException e) {
					}
				}
			}
		}).start();

		// to give info to server on nickname and current chats
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!stop.get()) {
					try {
						if (nickname != null) {
							writer.println("GIVING_INFO " + nickname + " " + target);
							writer.flush();
						}
						Thread.sleep(200);
					} catch (InterruptedException e) {
					}
				}
			}
		}).start();

		pane.setLeft(list_chats);
		pane.setCenter(chat);
		pane.setRight(list_chatters);
		pane.setBottom(user_pane);

		// want to update main chat for late users
		writer.println("UPDATE_CHAT " + target);
		writer.flush();

		primaryStage.setScene(new Scene(pane, 600, 300));
		primaryStage.setResizable(false);
		primaryStage.setOnCloseRequest(e -> {
			stop.set(true);
			Platform.exit();
			System.exit(0);
		});
		primaryStage.show();
	}

	private void getSelectedChatters() {
		selected_people = new HashSet<String>();
		List<String> chatters = list_chatters.getSelectionModel().getSelectedItems();
		if (chatters == null) {
			return;
		}
		if (chatters.size() == 0) {
			return;
		}
		for (String x : chatters) {
			if (x != null && nickname != null && !x.equals(nickname)) {
				if (selected_people.contains(x)) {
					selected_people.remove(x);
				} else {
					selected_people.add(x);
				}
			}
		}
		String selected_people_string = "";
		if (selected_people.size() == 0) {
			selected_people_string = "nobody.";
		} else {
			String[] arr = selected_people.toArray(new String[0]);
			for (int i = 0; i < arr.length - 1; i++) {
				selected_people_string += arr[i] + ", ";
			}
			selected_people_string += arr[arr.length - 1] + ".";
		}
		chat.appendText("Creating chat with: " + selected_people_string + "\n");
	}
}
