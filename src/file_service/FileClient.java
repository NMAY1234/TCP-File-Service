package file_service;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class FileClient {
    private final static int STATUS_CODE_LENGTH = 1;
    private final static int BYTEBUFFER_MAX_LENGTH = 3000;
    private static final String CLIENT_FILES = "ClientFiles/";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Syntax: FileClient <ServerIP> <ServerPort>");
            return;
        }
        int serverPort = Integer.parseInt(args[1]);
        String hostname = args[0];
        String command;

        do {
            Scanner keyboard = new Scanner(System.in);
            System.out.println("""
                    Please type a command.\s
                    Commands:
                    D: Delete
                    U: Upload
                    G: Download
                    L: List
                    R: Rename
                    Q: Quit""");

            command = keyboard.nextLine().toUpperCase();
            switch (command) {
                case "D" -> { // Delete
                    System.out.println("Please enter the name of the file to be deleted:");
                    String fileName = keyboard.nextLine();
                    ByteBuffer code;
                    ByteBuffer request = ByteBuffer.wrap(
                            (command + fileName).getBytes());
                    SocketChannel deleteChannel = openConnection();
                    connectChannel(deleteChannel, hostname, serverPort);
                    deleteChannel.write(request);
                    deleteChannel.shutdownOutput();

                    code = ByteBuffer.allocate(STATUS_CODE_LENGTH);
                    deleteChannel.read(code);
                    System.out.println(parseStatusCode(code));
                }

                case "L" -> { // List
                    ByteBuffer code;
                    ByteBuffer request = ByteBuffer.wrap((command).getBytes());
                    SocketChannel listChannel = openConnection();
                    connectChannel(listChannel, hostname, serverPort);
                    listChannel.write(request);
                    listChannel.shutdownOutput();

                    code = ByteBuffer.allocate(BYTEBUFFER_MAX_LENGTH);
                    listChannel.read(code);
                    String statusCode = parseStatusCode(code);
                    if(code.remaining() >= 0) {
                        System.out.println(statusCode);
                        byte[] a = new byte[code.remaining()];
                        code.get(a);
                        System.out.println(new String(a));
                    } else {
                        System.out.println(
                                "Error. List not available. \n" + statusCode);
                    }
                }

                case "R" -> { //Rename
                    System.out.println("Please enter the name of the file to be renamed: ");
                    String oldName = keyboard.nextLine();
                    System.out.println("Please enter the new name for the file: ");
                    String newName = keyboard.nextLine();
                    ByteBuffer code;
                    ByteBuffer request = ByteBuffer.wrap(
                            (command + oldName + ":" + newName).getBytes());
                    SocketChannel renameChannel = openConnection();
                    connectChannel(renameChannel, hostname, serverPort);
                    renameChannel.write(request);
                    renameChannel.shutdownOutput();

                    code = ByteBuffer.allocate(STATUS_CODE_LENGTH);
                    renameChannel.read(code);
                    System.out.println(parseStatusCode(code));
                }

                case "U" -> { // Upload
                    System.out.println("Please enter the name of the file to be uploaded:");
                    String fileName = keyboard.nextLine();

                    if (new File(CLIENT_FILES + fileName).exists()) {
                        ByteBuffer code;
                        ByteBuffer request = ByteBuffer.wrap(
                                (command + fileName).getBytes());
                        SocketChannel uploadChannel = openConnection();
                        connectChannel(uploadChannel, hostname, serverPort);
                        uploadChannel.write(request);
                        uploadChannel.shutdownOutput();

                        code = ByteBuffer.allocate(STATUS_CODE_LENGTH);
                        uploadChannel.read(code);
                        System.out.println(parseStatusCode(code));
                    } else {
                        System.out.println("No file with that name.");
                    }
                }

                case "G" -> { // Download
                    System.out.println("Please enter the name of the file to be downloaded:");
                    String fileName = keyboard.nextLine();
                    if(!new File(CLIENT_FILES + fileName).exists()) {
                        ByteBuffer code;
                        ByteBuffer request = ByteBuffer.wrap(
                                (command + fileName).getBytes());
                        SocketChannel downloadChannel = openConnection();
                        connectChannel(downloadChannel, hostname, serverPort);
                        downloadChannel.write(request);
                        downloadChannel.shutdownOutput();

                        code = ByteBuffer.allocate(STATUS_CODE_LENGTH);
                        downloadChannel.read(code);
                        System.out.println(parseStatusCode(code));
                    }
                    else{
                        System.out.println("File already exists.");
                    }
                }

                default -> {
                    if (!command.equals("Q")) {
                        System.out.println("Unknown command");
                    }
                }
            }
        }
        while (!command.equals("Q"));
        }

    public static SocketChannel openConnection() {
        SocketChannel channel;
        try {
            channel = SocketChannel.open();
        } catch (IOException e) {
            System.out.println("Failed to open connection. " + e.getMessage());
            throw new RuntimeException(e);
        }
        return channel;
    }

    public static void connectChannel(SocketChannel channel,
                                      String hostname,
                                      int serverPort){
        try {
            channel.connect(new InetSocketAddress(hostname, serverPort));
        } catch (IOException e) {
            System.out.println("Failed to connect to server. " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static String parseStatusCode(ByteBuffer sentBuffer) {
        sentBuffer.flip();
        String statusCode = "F";
        try {
            byte[] a = new byte[STATUS_CODE_LENGTH];
            sentBuffer.get(a);
            statusCode = (new String(a));
        } catch (BufferUnderflowException e) {
            System.out.println("Status code error: " + e.getMessage());
        }
        return statusCode;

    }
}
