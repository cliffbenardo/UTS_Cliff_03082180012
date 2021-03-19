import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class ChatClient extends Application {

  
    Label labelName = new Label("Name");
    Label labelMessages = new Label("Compose ");
    Label labelReceived = new Label("Messages");
    Label labelTitle = new Label();
    Label labelActiveUser = new Label("Active User");
    Label errorLabel = new Label("");

  
    ArrayList<String> userList = new ArrayList<>();
    ArrayList<String> chatMessages = new ArrayList<>();

   
    ListView<String> userListView = new ListView<String>();
    ListView<String> messageListView = new ListView<String>();

   
    ObservableList<String> userItems =
            FXCollections.observableArrayList (userList);

    ObservableList<String> messageItem =
            FXCollections.observableArrayList (chatMessages);


  
    TextField tfName = new TextField();
    TextArea taComposeMessage = new TextArea();

    
    Button btJoin = new Button("Join");
    Button btSend = new Button("Send");
    Button btDisconnect = new Button("Exit");

   
    DataOutputStream dataOutputStream;
    DataInputStream dataInputStream;

   
    boolean joined = false;

   
    private Socket socket;

    
    private String userName;

    private boolean connection = true;


    @Override  
    public void start(Stage primaryStage)  {

        
        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(10));

       
        Font titleFont = new Font("Times New Roman",20);
        labelTitle.setText("Welcome to Anonymous Chat Application");
        labelTitle.setFont(titleFont);
        Color titleColor = new Color(0.1, 0, 0.5,1);
        labelTitle.setTextFill(titleColor);


       
        tfName.setPromptText("Enter User Name");
        taComposeMessage.setPromptText("Enter your Message");

       
        taComposeMessage.setPrefHeight(2*(tfName.getHeight()));
        taComposeMessage.setPrefWidth(250);

        
        GridPane centreGridPane = new GridPane();
        centreGridPane.setPadding(new Insets(10));
        centreGridPane.setHgap(20);
        centreGridPane.setVgap(10);

       
        centreGridPane.add(labelName,0,0);
        centreGridPane.add(tfName,1,0);
        centreGridPane.add(btJoin,2,0);
        centreGridPane.add(labelReceived,0,2);
        centreGridPane.add(errorLabel,1,1,2,1);
        centreGridPane.add(messageListView,1,2,2,1);

       
        messageListView.setItems(messageItem);
        userListView.setItems(userItems);

        
        userListView.setEditable(false);
        messageListView.setEditable(false);

        
        userListView.setMaxWidth(180);
        userListView.setMaxHeight(250);


        
        VBox rightVBox = new VBox();
        rightVBox.setPadding(new Insets(20,0,10,0));
        rightVBox.setSpacing(10);
        rightVBox.getChildren().addAll(labelActiveUser,userListView);
        borderPane.setRight(rightVBox);


        
        GridPane bottomGridPane = new GridPane();
        bottomGridPane.add(labelMessages,0,0);
        bottomGridPane.add(taComposeMessage,1,0);
        bottomGridPane.add(btSend,4,0);
        bottomGridPane.add(btDisconnect,7,0);
        bottomGridPane.setHgap(20);
        bottomGridPane.setPadding(new Insets(10,0,10,10));
        btSend.setAlignment(Pos.BASELINE_RIGHT);

        
        borderPane.setTop(labelTitle);
        borderPane.setAlignment(labelTitle,Pos.CENTER);

        
        borderPane.setCenter(centreGridPane);

        
        borderPane.setBottom(bottomGridPane);

       
        Scene scene = new Scene(borderPane,580,400);
        primaryStage.setScene(scene); 
        primaryStage.setTitle("Anonymous Chat"); 
        primaryStage.show();    

       
        primaryStage.setOnCloseRequest(t -> closeSocketExit());
       
        btSend.setDisable(true);

        
        btJoin.setOnAction(event -> joinChat());
        btSend.setOnAction(e -> process());
        btDisconnect.setOnAction(event -> closeSocketExit());

        try {
            
            socket = new Socket("localhost", 8000);

            
            dataInputStream =
                    new DataInputStream(socket.getInputStream());

            
            dataOutputStream =
                    new DataOutputStream(socket.getOutputStream());

            
            new Thread(() -> receiveMessages()).start();
        }
       
        catch (IOException ex) {
            errorLabel.setTextFill(Color.RED);
            errorLabel.setText("Unable to establish connection.");
            System.err.println("Connection refused.");
        }
    }


   
    private void closeSocketExit() {
        try {
           
            if(socket!=null){
                socket.close();
            }
            Platform.exit();   
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }


   
    public void receiveMessages(){
        try{
            while(connection){
                String message;
               

                if(!joined){
                    addUserName();
                }
              
                else{
                 
                    message = dataInputStream.readUTF();
                    if(message.startsWith("[")){
                        addMessageToUserListView(message);
                    }
                    else{
                   
                        Platform.runLater(() -> {
                            messageItem.add(message);
                        });
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Socket is closed.receive");
            Platform.runLater(() -> {
                errorLabel.setTextFill(Color.RED);
                errorLabel.setText("Unable to establish connection.");
            });
            connection = false;
        }
    }


  
    private void joinChat(){
        userName = tfName.getText();
        if(userName.contains(",")){
            Platform.runLater(() -> {
               
                errorLabel.setTextFill(Color.RED);
                errorLabel.setText("Cannot contain ','.");
            });
        }
        else{
            try {
                dataOutputStream.writeUTF(userName);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


   
    private void addMessageToUserListView(String s) {
        List<String> list =
                Arrays.asList(
                        s.substring(1, s.length() - 1).split(", ")
        );
        Platform.runLater(() -> {
           
            userItems.clear();
            for(int i = 0; i < list.size(); i++){
                if(!(list.get(i).equals(userName))){
                    userItems.add(list.get(i));
                }
            }
        });
    }


 
    private void addUserName()  {
        String response;
        try {
            response = dataInputStream.readUTF();
            if (response.startsWith("Accepted")){
                joined = true;
                Platform.runLater(() -> {
                    System.out.println("User Connected as "+ userName);
                    btSend.setDisable(false);
                    btJoin.setDisable(true);
                    tfName.setEditable(false);
                    errorLabel.setTextFill(Color.GREEN);
                    errorLabel.setText("Joined as " + userName);
                });
            }
            else if(response.equals(userName)){
                Platform.runLater(() -> {
                    
                    tfName.clear();
                    errorLabel.setTextFill(Color.RED);
                    errorLabel.setText("User with same name exist.");
                });
            }
        } catch (IOException e) {
            System.out.println("Socket is closed.add");
            Platform.runLater(() -> {
                errorLabel.setTextFill(Color.RED);
                errorLabel.setText("Unable to establish connection.");
                connection = false;
            });
        }
    }



    private void process() {
        try {
            
            String string = tfName.getText().trim() + ":\n " +
                     taComposeMessage.getText().trim();

            
            dataOutputStream.writeUTF(string);

            
            taComposeMessage.setText("");
        }
        catch (IOException ex) {
            System.err.println(ex);
        }
    }
}


