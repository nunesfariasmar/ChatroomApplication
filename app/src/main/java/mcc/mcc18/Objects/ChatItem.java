package mcc.mcc18.Objects;

public class ChatItem {

    private String id;
    private String lastMessage;
    private String username;
    private String timeStamp;
    private String chatName;

    public ChatItem() {
    }

    public ChatItem(String id, String lastMessage, String name, String timeStamp, String chatName) {
        this.id = id;
        this.lastMessage = lastMessage;
        this.username = name;
        this.timeStamp = timeStamp;
        this.chatName = chatName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    public String getLastMessage(){
        return this.lastMessage;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String name) {
        this.username = name;
    }

    public String getTimeStamp(){return this.timeStamp;}
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getChatName() {
        return this.chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }
}