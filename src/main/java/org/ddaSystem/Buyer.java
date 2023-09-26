package org.ddaSystem;

public class Buyer {
    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getActive() {
        return Active;
    }

    public void setActive(String active) {
        Active = active;
    }

    public String getVenture() {
        return venture;
    }

    public void setVenture(String venture) {
        this.venture = venture;
    }

    private String Email;
    private String Password;
    private String Active;
    private String venture;

    public Buyer(String email, String password, String active, String venture) {
        Email = email;
        Password = password;
        Active = active;
        this.venture = venture;
    }

    @Override
    public String toString() {
        return "Buyer{" +
                "Email='" + Email + '\'' +
                ", Password='" + Password + '\'' +
                ", Active='" + Active + '\'' +
                ", venture='" + venture + '\'' +
                '}';
    }
}