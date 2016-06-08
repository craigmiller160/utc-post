package com.pilotfish.utc;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.*;
import java.util.Date;
import java.util.Scanner;

public class PretendUploader {

    private static final String PILOTFISH_HTTP_HOST = "http://localhost";
    private static final String LISTENER_CONTEXT = "http-post";
    private static final String REQUEST_PATH = "utc-audit";
    private static final int DEFAULT_PORT = 9000;
    private static final int ALT_PORT = 8080;
    private static final int SSL_PORT = 8443;

    private static final int TEST_MODE_CHOICE = 1;
    private static final int EMULATOR_CHOICE = 2;
    private static final int EIP_CHOICE = 3;

    private static final int DEFAULT_PORT_CHOICE = 1;
    private static final int ALT_PORT_CHOICE = 2;
    private static final int SSL_PORT_CHOICE = 3;

    private static final int ACTUAL_FILE_CHOICE = 1;
    private static final int DUMMY_FILE_CHOICE = 2;

    private static final String EI_CONSOLE_CONTEXT = "eiConsole";
    private static final String EIP_CONTEXT = "eip";

    private String servletContext = "";
    private String opcode;
    private String teamLeaderEmail;
    private String fileName;
    private int port = 9000;
    private boolean useDummyFile;
    private File excelFile;

    public static void main(String[] args) throws Exception{
        new PretendUploader().start();
    }

    public PretendUploader(){}

    public void start() throws Exception{
        Scanner in = new Scanner(System.in);

        System.out.println("Which environment?");
        System.out.println("1. eiConsole test mode");
        System.out.println("2. eiConsole emulator");
        System.out.println("3. eiPlatform");
        System.out.print("Choice: ");
        int envChoice = Integer.parseInt(in.nextLine());
        switch(envChoice){
            case TEST_MODE_CHOICE:
                servletContext = EI_CONSOLE_CONTEXT;
                break;
            case EMULATOR_CHOICE:
            case EIP_CHOICE:
                servletContext = EIP_CONTEXT;
                break;
            default:
                throw new IllegalArgumentException("Illegal selection for environment: " + envChoice);
        }

        System.out.println("\nWhat port is the PilotFish application running on?");
        System.out.println("1. 9000 (default)");
        System.out.println("2. 8080");
        System.out.println("3. 8443");
        System.out.print("Choice: ");
        int portChoice = Integer.parseInt(in.nextLine());
        switch(portChoice){
            case DEFAULT_PORT_CHOICE:
                port = DEFAULT_PORT;
                break;
            case ALT_PORT_CHOICE:
                port = ALT_PORT;
                break;
            case SSL_PORT_CHOICE:
                port = SSL_PORT;
                break;
            default:
                throw new IllegalArgumentException("Illegal selection for port: " + portChoice);
        }

        System.out.println("\nDo you want to use an actual file, or generate a dummy file?");
        System.out.println("1. Actual file");
        System.out.println("2. Dummy file");
        System.out.print("Choice: ");
        int fileChoice = Integer.parseInt(in.nextLine());
        switch(fileChoice){
            case ACTUAL_FILE_CHOICE:
                useDummyFile = false;
                break;
            case DUMMY_FILE_CHOICE:
                useDummyFile = true;
                break;
            default:
                throw new IllegalArgumentException("Illegal selection for file: " + fileChoice);
        }

        if(!useDummyFile){
            System.out.println("Enter the path to the file you want to use.");
            System.out.println("If the file is located in the same directory as the jar, just enter the file name.");
            System.out.print("Path: ");
            String path = in.nextLine();
            excelFile = getFile(path);
            fileName = excelFile.getName();
        }
        else{
            System.out.print("Enter a dummy filename: ");
            fileName = in.nextLine();
        }

        System.out.print("\nEnter a test opcode: ");
        opcode = in.nextLine();
        System.out.print("Enter a test email: ");
        teamLeaderEmail = in.nextLine();

        System.out.println("\nPreparing Http Post");
        System.out.println("Request URL: " + getRequestUrl());
        System.out.println("OpCode: " + opcode);
        System.out.println("Team Leader Email: " + teamLeaderEmail);
        System.out.println("File Name: " + fileName);
        System.out.print("\nPress 'enter' to continue");
        in.nextLine();

        System.out.println("\nSending Http Post");
        int status = sendAsFile();
        if(status >= 0) {
            System.out.println("Status Code: " + status);
        }
    }

    private File getFile(String path) throws IOException{
        File f = new File(path);
        if(!f.exists()){
            f = new File(System.getProperty("user.dir") + File.pathSeparator + path);
            if(!f.exists()){
                throw new IOException("File doesn't exist: " + path);
            }
        }

        return f;
    }

    private String getRequestUrl(){
        return String.format("%s:%d/%s/%s/%s", PILOTFISH_HTTP_HOST, port, servletContext, LISTENER_CONTEXT, REQUEST_PATH);
    }

    private int send(PostMethod postMethod) {
        HttpClient httpClient = new HttpClient();
        try {
            postMethod.setURI(new URI(getRequestUrl(), true));
            return httpClient.executeMethod(postMethod);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private File getPretendExcelFile() {
        File f = null;
        try {
            f = File.createTempFile("test", "xls");
            FileOutputStream outF = new FileOutputStream(f);
            outF.write("CONTENT OF THE UPLOADED EXCEL FILE".getBytes());
            outF.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }

    private int sendAsFile() {
        PostMethod postMethod = new PostMethod();
        postMethod.addRequestHeader("team.leader.email", teamLeaderEmail);
        postMethod.addRequestHeader("upload.file.name", fileName);
        postMethod.addRequestHeader("opcode", opcode);
        postMethod.addRequestHeader("team.leader", "Bernie");
        postMethod.addRequestHeader("file.unique.key", "123");
        postMethod.addRequestHeader("upload.time", (new Date()).toString());
        postMethod.addRequestHeader("eventinfokey", "456");
        postMethod.addRequestHeader("siteid", "987");

        File f = useDummyFile ? getPretendExcelFile() : excelFile;
        try {
            postMethod.setRequestEntity(new InputStreamRequestEntity(new FileInputStream(f)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return send(postMethod);
    }
}