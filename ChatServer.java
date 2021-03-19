import java.io.*;
import java.net.*;
import java.util.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;


 
public class ChatServer extends Application {

   
    Label lbLog = new Label("Log");
    Label lbUserList = new Label("Active User");

   
    private ArrayList<String> logList = new ArrayList<>();
    private ArrayList<String> userList = new ArrayList<>();

   
    ListView<String> logListView = new ListView<String>();
    ListView<String> userListView = new ListView<String>();

    
    ObservableList<String> logItems =
            FXCollections.observableArrayList (logList);
    ObservableList<String> userItems =
            FXCollections.observableArrayList (userList);

    
    private Hashtable outputStreams = new Hashtable();

   
    private ArrayList<Socket> socketList = new ArrayList<>();

    
    private ServerSocket serverSocket;

    @Override 
    public void start(Stage primaryStage) {

        
        userListView.setItems(userItems);
        logListView.setItems(logItems);
        logListView.setMinWidth(430);

        
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));

        
        gridPane.add(lbLog,0,0);
        gridPane.add(logListView,0,1);
        gridPane.add(lbUserList,0,2);
        gridPane.add(userListView,0,3);
        
        Scene scene = new Scene(gridPane, 450, 400);
        primaryStage.setTitle("Server"); 
        primaryStage.setScene(scene); 
        primaryStage.show(); 
       
        primaryStage.setOnCloseRequest(t -> closeSocketExit());

       
        new Thread(() -> listen()).start();
    }


    
    private void closeSocketExit() {
        try {
            for(Socket socket:socketList){
                
                if(socket!=null){
                    socket.close();
                }
            }
            Platform.exit();    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    
    private void listen() {
        try {
            
            serverSocket = new ServerSocket(8000);
            Platform.runLater(() ->
                    logItems.add("MultiThreadServer started at " + new Date()));

            while (true) {
                Socket socket = serverSocket.accept();

                
                socketList.add(socket);

                
                Platform.runLater(() ->
                        logItems.add("Connection from " + socket + " at " + new Date()));

                
                DataOutputStream dataOutputStream =
                        new DataOutputStream(socket.getOutputStream());

                
                outputStreams.put(socket, dataOutputStream);

                
                new ServerThread(this, socket);
            }
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
    }


    
    private void dispatchUserList() {
        this.sendToAll(userList.toString());
    }


    
    Enumeration getOutputStreams(){
        return outputStreams.elements();
    }


    
    void sendToAll(String message){
       
        for (Enumeration e = getOutputStreams(); e.hasMoreElements();){
            DataOutputStream dout = (DataOutputStream)e.nextElement();
            try {
                
                dout.writeUTF(message);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    
    void sendOnlineStatus(Socket socket,String message){
        for (Enumeration e = getOutputStreams(); e.hasMoreElements();){
            DataOutputStream dataOutputStream = (DataOutputStream)e.nextElement();
            try {
                
                if(!(outputStreams.get(socket) == dataOutputStream)){
                   
                    dataOutputStream.writeUTF(message);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


   
    class ServerThread extends Thread {
        private ChatServer server;
        private Socket socket;
        String userName;    // Default null;
        boolean userJoined; // Default false;

        
        public ServerThread(ChatServer server, Socket socket) {
            this.socket = socket;
            this.server = server;
            start();
        }

        
        public void run() {
            try {
                
                DataInputStream dataInputStream =
                        new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream =
                        new DataOutputStream(socket.getOutputStream());

                
                while (true) {
                    
                    if(!userJoined){
                        userName = dataInputStream.readUTF();
                        if(userList.contains(userName)){
                            dataOutputStream.writeUTF(userName);
                            System.out.println(userName + " already exist.");
                        }
                        else{
                            userList.add(userName);
                            dataOutputStream.writeUTF("Accepted");
                            server.dispatchUserList();
                            System.out.println(userName +" joined the chat room");
                            userJoined = true;
                            String userNotification = userName + " joined the chat room.";
                            Platform.runLater(() ->
                                    logItems.add(userName + " joined the chat room."));
                            server.sendOnlineStatus(socket,userNotification);
                            userItems.clear();
                            userItems.addAll(userList);
                        }
                    }
                  
                    else if(userJoined){
                       
                        String string = dataInputStream.readUTF();

                        server.sendToAll(string);
                        server.dispatchUserList();

                       
                        Platform.runLater(() ->logItems.add(string));
                    }
                }
            }


           
            catch(IOException ex) {
                System.out.println("Connection Closed for " + userName);
                Platform.runLater(() ->
                        logItems.add("Connection Closed for " + userName));

                if(!userName.equals(null)){
                    userList.remove(userName);
                }
                outputStreams.remove(socket);
                server.dispatchUserList();
                if (!userName.equals(null)){
                    server.sendToAll(userName + " has left the chat room.");
                }
                Platform.runLater(() ->{
                    userItems.clear();
                    userItems.addAll(userList);
                });
            }
        }
    }
}
