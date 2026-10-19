package com.slms.services;

/** Current in-memory login. The server token is cleared on logout. */
public final class AuthSession {
    private static final java.util.prefs.Preferences PREFS=java.util.prefs.Preferences.userNodeForPackage(AuthSession.class);
    private static String role, token, code, name, email;
    private AuthSession() {}
    public static void admin(String name, String email, String token) { AuthSession.role="ADMIN"; AuthSession.name=name; AuthSession.email=email; AuthSession.token=token; code=null; persist(); }
    public static void member(String name, String code) { AuthSession.role="MEMBER"; AuthSession.name=name; AuthSession.code=code; token=null; email=null; persist(); }
    public static String role() { return role; }
    public static String token() { return token; }
    public static String code() { return code; }
    public static String name() { return name; }
    public static String email() { return email; }
    public static boolean isAdmin() { return "ADMIN".equals(role); }
    public static boolean isAuthenticated() { return role != null; }
    public static void restore(){role=PREFS.get("role",null);token=PREFS.get("token",null);code=PREFS.get("code",null);name=PREFS.get("name",null);email=PREFS.get("email",null);if(role==null||("ADMIN".equals(role)&&(token==null||token.isBlank()))||("MEMBER".equals(role)&&(code==null||code.isBlank())))clear();}
    private static void persist(){PREFS.put("role",role);if(token==null)PREFS.remove("token");else PREFS.put("token",token);if(code==null)PREFS.remove("code");else PREFS.put("code",code);PREFS.put("name",name==null?"":name);if(email==null)PREFS.remove("email");else PREFS.put("email",email);}
    public static void clear() { role=null; token=null; code=null; name=null; email=null;try{PREFS.clear();}catch(java.util.prefs.BackingStoreException ignored){} }
}
