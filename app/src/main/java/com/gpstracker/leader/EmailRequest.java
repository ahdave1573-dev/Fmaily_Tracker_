package com.gpstracker.leader;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

public class EmailRequest {
    @SerializedName("sender")
    public Sender sender;

    @SerializedName("to")
    public List<To> to;

    @SerializedName("subject")
    public String subject;

    @SerializedName("htmlContent")
    public String htmlContent;

    // Constructor
    public EmailRequest(String senderEmail, String receiverEmail, String subject, String content) {
        // "Family Tracker App" e sender nu naam dekhase
        this.sender = new Sender("Family Tracker App", senderEmail);
        this.to = Collections.singletonList(new To(receiverEmail));
        this.subject = subject;
        this.htmlContent = content;
    }

    public static class Sender {
        @SerializedName("name")
        public String name;
        @SerializedName("email")
        public String email;

        public Sender(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }

    public static class To {
        @SerializedName("email")
        public String email;

        public To(String email) {
            this.email = email;
        }
    }
}