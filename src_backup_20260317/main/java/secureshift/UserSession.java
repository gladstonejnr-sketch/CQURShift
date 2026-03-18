package secureshift;

public class UserSession {
    public enum Role { ADMIN, MANAGER }
    private static UserSession instance;
    private String username;
    private Role role;
    private UserSession() {}
    public static UserSession getInstance() { if (instance == null) instance = new UserSession(); return instance; }
    public void login(String username, String roleStr) { this.username = username; this.role = Role.valueOf(roleStr.toUpperCase()); }
    public void logout() { this.username = null; this.role = null; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isLoggedIn() { return username != null; }
}
