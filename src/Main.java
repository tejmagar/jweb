public class Main {
    public static void main(String[] args) {
        System.out.println("Server listening at http://127.0.0.1:8080");
        Server server = new Server(8080);
        server.run();
    }
}
