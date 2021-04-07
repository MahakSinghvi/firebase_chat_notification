package com.ps.agrostand.fcm_chat;

public class FCM_UserDTO {

    private String user_id = "";
    private String email = "";
    private String username = "";
    private String agroUserId = "";
    private String timeZone = "";
    private String languageId = "";
    private String image = "";
    private String message_count="";
    private String user_type = "";
    private String created_date="";
    private String device_type="";
    private String device_token="";

    public FCM_UserDTO() {
    }

    public FCM_UserDTO(String user_id, String email, String username,
                       String agroId, String timeZone, String languageId, String image, String message_count,
                       String user_type, String created_date, String device_type, String device_token) {
        this.user_id = user_id;
        this.email = email;
        this.username = username;
        this.agroUserId = agroId;
        this.timeZone = timeZone;
        this.languageId = languageId;
        this.image = image;
        this.message_count = message_count;
        this.user_type = user_type;
        this.created_date = created_date;
        this.device_type = device_type;
        this.device_token = device_token;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAgroUserId() {
        return agroUserId;
    }

    public void setAgroUserId(String agroUserId) {
        this.agroUserId = agroUserId;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getMessage_count() {
        return message_count;
    }

    public void setMessage_count(String message_count) {
        this.message_count = message_count;
    }

    public String getUser_type() {
        return user_type;
    }

    public void setUser_type(String user_type) {
        this.user_type = user_type;
    }

    public String getCreated_date() {
        return created_date;
    }

    public void setCreated_date(String created_date) {
        this.created_date = created_date;
    }

    public String getDevice_type() {
        return device_type;
    }

    public void setDevice_type(String device_type) {
        this.device_type = device_type;
    }

    public String getDevice_token() {
        return device_token;
    }

    public void setDevice_token(String device_token) {
        this.device_token = device_token;
    }

}
