package mcc.mcc18.Objects;

public class UserItem {
    private String name;
    private String mail;
    private String id;
    private String photoUrl;

    public UserItem(){

    }

    public UserItem(String name, String mail, String id){
        this.name = name;
        this.mail = mail;
        this.id = id;
        this.photoUrl = null;
    }

    public UserItem(String name, String mail, String id, String photoUrl){
        this.name = name;
        this.mail = mail;
        this.id = id;
        this.photoUrl = photoUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
