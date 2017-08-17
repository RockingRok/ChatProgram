package chatProgram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

public class Server extends Observable {
	private static Set<String> clients = new HashSet<String>();
	private static Map<String, ArrayList<String>> chats = new HashMap<String, ArrayList<String>>();
	private static Map<String, ArrayList<String>> chat_clients = new HashMap<String, ArrayList<String>>();

	private int socket;

	Server(int socket) {
		this.socket = socket;
	}

	public void setUpNetworking() throws Exception {
		@SuppressWarnings("resource")
		ServerSocket serverSock = new ServerSocket(socket);
		while (true) {
			Socket clientSocket = serverSock.accept();
			ClientObserver writer = new ClientObserver(clientSocket.getOutputStream());
			Thread t = new Thread(new ClientHandler(clientSocket));
			t.start();
			this.addObserver(writer);
			System.out.println("A client has connected to the server.");
		}
	}

	class ClientHandler implements Runnable {
		private BufferedReader reader;
		String clientName = null;

		public ClientHandler(Socket clientSocket) {
			Socket sock = clientSocket;
			try {
				reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			String message;
			try {
				while ((message = reader.readLine()) != null) {
					String[] message_list = message.split(" ");
					if (message_list[0].equals("GET_ALL_CLIENTS")) {
						setChanged();
						notifyObservers("GET_ALL_CLIENTS " + message_list[1] + " " + convertToString(message_list[1]));
					} else if (message_list[0].equals("GET_ALL_CHATS")) {
						String chat_name = "";
						String internal_chat_name = "";
						// the first string in this sequence is the user
						for (int i = 1; i < message_list.length; i++) {
							chat_name += message_list[i] + " ";
							internal_chat_name += message_list[i];
						}

						if (chat_clients.get(internal_chat_name) == null) {
							chat_clients.put(internal_chat_name, new ArrayList<String>());
						}

						for (int i = 1; i < message_list.length; i++) {
							if (!chat_clients.get(internal_chat_name).contains(message_list[i])) {
								chat_clients.get(internal_chat_name).add(message_list[i]);
							}
						}

						setChanged();
						notifyObservers("GET_ALL_CHATS " + chat_name);
					} else if (message_list[0].equals("GIVING_INFO")) {
						clients.add(message_list[1]);
						clientName = message_list[1];
						if (chat_clients.get(message_list[2]) == null) {
							chat_clients.put(message_list[2], new ArrayList<String>());
						}

						if (!chat_clients.get(message_list[2]).contains(message_list[1])) {
							chat_clients.get(message_list[2]).add(message_list[1]);
						}
					} else if (message_list[0].equals("UPDATE_CHAT")) {
						String target_channel = message_list[1];

						if (chats.get(target_channel) == null) {
							chats.put(target_channel, new ArrayList<String>());
						}

						ArrayList<String> channel_chat = chats.get(target_channel);

						String chat_log = "";
						for (String user_message : channel_chat) {
							chat_log += user_message + "NEW_LINE";
						}

						setChanged();
						notifyObservers(target_channel + " " + chat_log);
					}
					// being down here means that a user is sending a chat
					// message
					else {
						String sender = message_list[0];
						String target_channel = message_list[1];

						if (chats.get(target_channel) == null) {
							chats.put(target_channel, new ArrayList<String>());
						}

						ArrayList<String> channel_chat = chats.get(target_channel);

						String constructed_message = sender;
						for (int i = 2; i < message_list.length; i++) {
							constructed_message += message_list[i] + " ";
						}

						channel_chat.add(constructed_message);

						String chat_log = "";
						for (int i = 0; i < channel_chat.size() - 1; i++) {
							chat_log += channel_chat.get(i) + "NEW_LINE";
						}
						chat_log += channel_chat.get(channel_chat.size() - 1);

						setChanged();
						notifyObservers(target_channel + " " + chat_log);
					}
				}
			} catch (IOException e) {
				cleanAll();
				e.printStackTrace();
			}
		}

		public String convertToString(String selected_channel) {
			String users = "";
			ArrayList<String> client_list = chat_clients.get(selected_channel);
			if (client_list != null) {
				for (int i = 0; i < client_list.size(); i++) {
					users += client_list.get(i) + " ";
				}
			}
			return users;
		}

		private synchronized void cleanAll() {
			clients.remove(clientName);
			HashMap<String, ArrayList<String>> temp = new HashMap<>();
			for (Map.Entry<String, ArrayList<String>> chat : chats.entrySet()) {
				clean(chat, temp);
			}
			chats = temp;
			temp = new HashMap<>();
			for (Map.Entry<String, ArrayList<String>> chat : chat_clients.entrySet()) {
				clean(chat, temp);
			}
			chat_clients = temp;
		}

		private void clean(Map.Entry<String, ArrayList<String>> chat, HashMap<String, ArrayList<String>> temp) {
			ArrayList<String> l = chat.getValue();
			l.remove(clientName);
			String key = chat.getKey();
			if (l.size() > 0 || key.equals("Main")) {
				temp.put(key, l);
			}
		}
	}
}
