package com.pilotfish.utc;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.*;
import java.util.Date;
import java.util.Scanner;

public class PretendUploader {

    private static final String HTTP_PREFIX = "http://";
    private static final String LISTENER_CONTEXT = "http-post";
    private static final String REQUEST_PATH = "utc-audit";
    private static final int DEFAULT_PORT = 9000;
    private static final int ALT_PORT = 8080;
    private static final int SSL_PORT = 8443;
    private static final String LOCAL_HOST = "localhost";

    private static final int TEST_MODE_CHOICE = 1;
    private static final int EMULATOR_CHOICE = 2;
    private static final int EIP_CHOICE = 3;

    private static final int DEFAULT_PORT_CHOICE = 1;
    private static final int ALT_PORT_CHOICE = 2;
    private static final int SSL_PORT_CHOICE = 3;

    private static final int ACTUAL_FILE_CHOICE = 1;
    private static final int DUMMY_FILE_CHOICE = 2;

    private static final int DEFAULT_SETTING_CHOICE = 1;
    private static final int CUSTOM_SETTING_CHOICE = 2;

    private static final int LOCAL_HOST_CHOICE = 1;
    private static final int CUSTOM_HOST_CHOICE = 2;

    private static final String EI_CONSOLE_CONTEXT = "eiConsole";
    private static final String EIP_CONTEXT = "eip";

    private String hostname = LOCAL_HOST;
    private String servletContext = "";
    private String opcode;
    private String teamLeaderEmail;
    private String fileName;
    private int port = DEFAULT_PORT;
    private boolean useDummyFile = true;
    private File excelFile;
    private String filePath;
    private boolean useDefaultSettings = true;

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

        System.out.println("\nDefault Settings");
        System.out.println("Hostname: " + LOCAL_HOST);
        System.out.println("Port: " + DEFAULT_PORT);
        System.out.println("File: (Dummy/Real): Dummy");
        System.out.println("\nUse these settings?");
        System.out.println("1. Use defaults");
        System.out.println("2. Change defaults");
        System.out.print("Choice: ");
        int settingChoice = Integer.parseInt(in.nextLine());
        switch(settingChoice){
            case DEFAULT_SETTING_CHOICE:
                useDefaultSettings = true;
                break;
            case CUSTOM_SETTING_CHOICE:
                useDefaultSettings = false;
                break;
            default:
                throw new IllegalArgumentException("Illegal selection for settings: " + envChoice);
        }

        if(!useDefaultSettings){
            configureSettings(in);
        }

        if(useDummyFile){
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
        System.out.println("File: " + (useDummyFile ? "Dummy" : "Real"));
        System.out.println("File Path: " + (useDummyFile ? "N/A" : filePath));
        System.out.println("File Name: " + fileName);
        System.out.print("\nPress 'enter' to continue");
        in.nextLine();

        System.out.println("\nSending Http Post");
        int status = sendAsFile();
        if(status >= 0) {
            System.out.println("Status Code: " + status);
        }
    }

    private void configureSettings(Scanner in) throws Exception{
        System.out.println("\nWhat is the hostname of the PilotFish applicaiton server?");
        System.out.println("1. Localhost (default)");
        System.out.println("2. Other (you will specify)");
        System.out.print("Choice: ");
        int hostChoice = Integer.parseInt(in.nextLine());
        switch(hostChoice){
            case LOCAL_HOST_CHOICE:
                hostname = LOCAL_HOST;
                break;
            case CUSTOM_HOST_CHOICE:
                System.out.print("Hostname: ");
                hostname = in.nextLine();
                break;
            default:
                throw new IllegalArgumentException("Illegal selection for host: " + hostChoice);
        }

        System.out.println("\nWhat is the port number of the PilotFish application server?");
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
            filePath = excelFile.getAbsolutePath();
            fileName = excelFile.getName();
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
        return String.format("%s%s:%d/%s/%s/%s", HTTP_PREFIX, hostname, port, servletContext, LISTENER_CONTEXT, REQUEST_PATH);
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
        FileOutputStream out = null;
        try {
            f = File.createTempFile("test", "xls");
            out = new FileOutputStream(f);
            out.write("CONTENT OF THE UPLOADED EXCEL FILE".getBytes());
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        finally{
            if(out != null){
                try{
                    out.close();
                }
                catch(IOException ex){
                    ex.printStackTrace();
                }
            }
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