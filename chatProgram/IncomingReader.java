package chatProgram;

import java.io.IOException;
import java.util.ArrayList;

import javafx.application.Platform;

class IncomingReader implements Runnable {
	public void run() {
		String message;
		try {
			while ((message = ClientMain.reader.readLine()) != null && !ClientMain.stop.get()) {
				String[] message_parse = message.split(" ");
				if(message_parse[0].equals("GET_ALL_CLIENTS"))
				{
					Platform.runLater(new Runnable(){
						@Override
						public void run() {
							//want to be picking up our broadcast of clients only
							if(message_parse[1].equals(ClientMain.target))
							{
								//need to compare, no point updating if all elements are the same
								boolean isSame = true;
								ArrayList<String> received_clients = new ArrayList<String>();
								
								for(int i = 2; i < message_parse.length; i++)
								{
									received_clients.add(message_parse[i]);
								}
								
								if(received_clients.size() != ClientMain.items_chatters.size()) 
								{
									isSame = false;
								}
								else
								{
									//they are the same size
									for(String client : received_clients)
									{
										if(!ClientMain.items_chatters.contains(client))
										{
											isSame = false;
										}
									}
								}
								
								if(!isSame)
								{
									ClientMain.items_chatters.clear();
									for(int i = 2; i < message_parse.length; i++)
									{
										ClientMain.items_chatters.add(message_parse[i]);
									}	
								}
							}
						}
					});
				}
				else if(message_parse[0].equals("GET_ALL_CHATS"))
				{
					Platform.runLater(new Runnable(){
						@Override
						public void run() {
							String chat_name = "";
							for(int i = 1; i < message_parse.length; i++)
							{
								chat_name += message_parse[i] + " ";
							}
							
							for(int i = 1; i < message_parse.length; i++)
							{
								//we want to add this chat
								if(ClientMain.nickname != null && message_parse[i].equals(ClientMain.nickname))
								{
									if(!ClientMain.items_chats.contains(chat_name))
									{
										ClientMain.items_chats.add(chat_name);
									}
								}
							}
						}
					});
				}
				//server sends out chat logs for all, only want to grab the one for us
				else if (message_parse[0].equals(ClientMain.target))
				{
					message = message.replaceAll("NEW_LINE", "\n");
					message = message.substring(ClientMain.target.length() + 1);
					if(message.length() > 0)
					{
						ClientMain.chat.setText(message + "\n");
					}
					else {
						ClientMain.chat.setText(message);
					}
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
