package app;

public class TestHash {
    public static void main(String[] args) {
        String[] users = {"alice123", "bob123", "yash123"};
        for(String password : users){
            String hash = HashUtil.sha256Hex(password);
            System.out.println(password + " -> " + hash);
        }
    }
}
