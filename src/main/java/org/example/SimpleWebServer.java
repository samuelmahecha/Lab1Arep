package org.example;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.*;

public class SimpleWebServer {
    private static final int PORT = 8080;
    public static final String WEB_ROOT = "src/main/resources";

    public static void main(String[] args) throws IOException {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            threadPool.submit(new ClientHandler(clientSocket));
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedOutputStream dataOut = new BufferedOutputStream(clientSocket.getOutputStream())) {

            String requestLine = in.readLine();
            if (requestLine == null) return;
            String[] tokens = requestLine.split(" ");
            String method = tokens[0];
            String fileRequested = tokens[1];

            if (method.equals("GET")) {
                handleGetRequest(fileRequested, out, dataOut);
            } else if (method.equals("POST")) {
                handlePostRequest(fileRequested, in, out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePostRequest(String fileRequested, BufferedReader in, PrintWriter out) throws IOException {
        StringBuilder payload = new StringBuilder();
        String line;

        while (!(line = in.readLine()).isEmpty()) {
        }

        while (in.ready() && (line = in.readLine()) != null) {
            payload.append(line);
        }
        String body = payload.toString();

        String fileName = "received_data.txt";
        try (FileWriter fileWriter = new FileWriter(new File(SimpleWebServer.WEB_ROOT, fileName))) {
            fileWriter.write(body);
        }

        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/plain");
        out.println();
        out.println("File created: " + fileName);
        out.flush();
    }


    private void handleGetRequest(String fileRequested, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        File file = new File(SimpleWebServer.WEB_ROOT, fileRequested);
        int fileLength = (int) file.length();
        String content = getContentType(fileRequested);
        String fileReadable = file.toString();
        if (file.exists()) {
            if (fileReadable.endsWith(".png") || fileReadable.endsWith(".jpg") || fileReadable.endsWith(".jpeg")) {
                byte[] imageData = getImageContent(fileReadable);
                String base64Image = Base64.getEncoder().encodeToString(imageData);

                String htmlResponse = "<!DOCTYPE html>\r\n"
                        + "<html>\r\n"
                        + "    <head>\r\n"
                        + "        <title>Imagen</title>\r\n"
                        + "    </head>\r\n"
                        + "    <body>\r\n"
                        + "         <center><img src=\"data:image/jpeg;base64," + base64Image + "\" alt=\"image\"></center>\r\n"
                        + "    </body>\r\n"
                        + "</html>";

                // Send HTML response with embedded base64 image
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html");
                out.println();
                out.println(htmlResponse);
            }else {

                byte[] fileData = readFileData(file, fileLength);
                out.println("HTTP/1.1 200 OK");
                out.println("Content-type: " + content);
                out.println("Content-length: " + fileLength);
                out.println();
                out.flush();
                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
            }
        } else {
            out.println("HTTP/1.1 404 Not Found");
            out.println("Content-type: text/html");
            out.println();
            out.flush();
            out.println("<html><body><h1>File Not Found</h1></body></html>");
            out.flush();
        }
    }

    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".html")) return "text/html";
        else if (fileRequested.endsWith(".css")) return "text/css";
        else if (fileRequested.endsWith(".js")) return "application/javascript";
        else if (fileRequested.endsWith(".png")) return "image/png";
        else if (fileRequested.endsWith(".jpg")) return "image/jpeg";
        return "text/plain";
    }
    private static byte[] getImageContent(String file) throws IOException {
        Path filePath = Paths.get(file);
        return Files.readAllBytes(filePath);
    }
    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];
        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null) fileIn.close();
        }
        return fileData;
    }

    private void printRequestHeader (String requestLine, BufferedReader in) throws IOException {
        System.out.println("Request Line: " + requestLine);
        String inputLine = "";
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.isEmpty()) break;
            System.out.println("Header: " + inputLine);
        }
    }
}