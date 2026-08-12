package rmi;

import java.io.Serializable;

public class UserSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private long userId;
    private String fullName;
    private String email;

    public UserSession(long userId, String fullName, String email) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
    }

    public long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return "UserSession{id=" + userId + ", name='" + fullName + "', email='" + email + "'}";
    }
}
