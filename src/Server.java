import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Server {
    public static class HeaderResult {
        public String startHeader;
        public HashMap<String, String> headers;
        public byte[] extraBody;
    }

    private final int port;

    public Server(int port) {
        this.port = port;
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(this.port)){
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        handleClient(socket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles each client
     * @param socket Socket
     * @throws Exception exception
     */
    private void handleClient(Socket socket) throws Exception {
        HeaderResult headerResult = readRequestHeader(socket);
        String content = "Hello World";
        String response = "HTTP/1.1 200 OK\r\nConnection: close\r\nContent-Type: text/html\r\nContent-Length: "
                + content.length() + "\r\n\r\n" + content;
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(bytes);
        socket.close();
    }

    private int searchHeaderEnd(List<Byte> requestBuffer)  {
        // GET / HTTP/1.1\r\n\r\nHello World
        for(int i=0; i<requestBuffer.size(); i++) {
            int searchUpto = i + 4;

            if (searchUpto > requestBuffer.size()) {
                System.out.println("Search upto is larger than the request buffer size.");
                return -1;
            }

            if (requestBuffer.get(i) == 13 && requestBuffer.get(i + 1) == 10
                    && requestBuffer.get(i + 2) == 13 && requestBuffer.get(i + 3) == 10) {
                return i;
            }
        }

        return -1;
    }

    private HeaderResult readRequestHeader(Socket socket) throws Exception {
        List<Byte> requestBuffer = new ArrayList<>();

        int searchHeaderIndex;

        while (true) {
            searchHeaderIndex = searchHeaderEnd(requestBuffer);
            if (searchHeaderIndex == -1) {
                byte[] buffer = new byte[1024];
                InputStream inputStream = socket.getInputStream();
                int bytesRead = inputStream.read(buffer);

                for (int i=0; i<bytesRead; i++) {
                    requestBuffer.add(buffer[i]);
                }
            } else {
                break;
            }
        }

        // Extract headers
        byte[] headerBytes = new byte[searchHeaderIndex + 1];
        for (int i=0; i<searchHeaderIndex + 1; i++) {
            headerBytes[i] = requestBuffer.get(i);
        }

        int extraReadLength = requestBuffer.size() - headerBytes.length - 4;
        byte[] extraRead;

        if(extraReadLength > 0) {
            extraRead = new byte[extraReadLength];
            for(int i=0; i<extraReadLength; i++) {
                int readFrom = headerBytes.length + i;
                extraRead[i] = requestBuffer.get(readFrom);
            }
        } else {
            extraRead = new byte[0];
        }

        String headers = new String(headerBytes);
        return parseHeaders(headers, extraRead);
    }

    private HeaderResult parseHeaders(String header, byte[] extraRead) {
        HashMap<String, String> headers = new HashMap<>();

        HeaderResult headerResult = new HeaderResult();
        String[] headerLines = header.split("\r\n");
        headerResult.startHeader = headerLines[0];
        headerResult.extraBody = extraRead;

        for(int i=1; i<headerLines.length; i++) {
            String headerLine = headerLines[0];
            String[] values = headerLine.split(":");
            String headerName = values[0].trim();

            if (values.length >= 2) {
                String headerValue = values[1].trim();
                headers.put(headerName, headerValue);
            }
        }

        headerResult.headers = headers;
        return headerResult;
    }
}
