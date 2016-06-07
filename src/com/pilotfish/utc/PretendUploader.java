package com.pilotfish.utc;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.*;
import java.util.Date;
import java.util.Scanner;

public class PretendUploader {

    private static final String PILOTFISH_HTTP_SERVER = "http://localhost:9000";
    private static String servletContext = "";
    private static final String LISTENER_CONTEXT = "http-post";
    private static final String REQUEST_PATH = "utc-audit";

    private static String opcode;
    private static String teamLeaderEmail;
    private static String fileName;

    private static final int TEST_MODE = 1;
    private static final int EMULATOR = 2;
    private static final int EIP = 3;

    private static final String EI_CONSOLE_CONTEXT = "eiConsole";
    private static final String EIP_CONTEXT = "eip";

    static String PRETEND_UPLOAD_QUEUE =
            "<DATA>\n" +
                    "<PilotFish_Upload_Queue>\n" +
                    "  <PILOTFISH_UPLOAD_QUEUE_ROW>\n" +
                    "    \t<FILE_UNIQUE_KEY>0000000000</FILE_UNIQUE_KEY>\n" +
                    "    \t<TEAM_LEADER>Bernie Pozzi</TEAM_LEADER>\n" +
                    "    \t<TEAM_LEADER_EMAIL>%s</TEAM_LEADER_EMAIL>\n" +
                    "    \t<UPLOAD_FILE_NAME>%s</UPLOAD_FILE_NAME>\n" +
                    "    \t<UPLOAD_TIMESTAMP>1984-10-01 13:16:50</UPLOAD_TIMESTAMP>\n" +
                    "    \t<OPCODE>%s</OPCODE>\n" +
                    "    \t<SITEID>999999</SITEID>\n" +
                    "    \t<EVENTINFOKEY>222222222222222222222</EVENTINFOKEY>\n" +
                    "  </PILOTFISH_UPLOAD_QUEUE_ROW>\n" +
                    "</PilotFish_Upload_Queue>\n" +
                    "</DATA>\n";

    private static String getRequestUrl(){
        return String.format("%s/%s/%s/%s", PILOTFISH_HTTP_SERVER,servletContext,LISTENER_CONTEXT,REQUEST_PATH);
    }

    private static int send(PostMethod postMethod) {
        HttpClient httpClient = new HttpClient();
        try {
            postMethod.setURI(new URI(getRequestUrl(), true));
            return httpClient.executeMethod(postMethod);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static void sendAsList() {
        PostMethod postMethod = new PostMethod();
        postMethod.setRequestEntity(new StringRequestEntity(String.format(PRETEND_UPLOAD_QUEUE, teamLeaderEmail, fileName, opcode)));
        send(postMethod);
    }

    private static File getPretendExcelFile() {
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

    private static int sendAsFile() {
        PostMethod postMethod = new PostMethod();
        postMethod.addRequestHeader("team.leader.email", teamLeaderEmail);
        postMethod.addRequestHeader("upload.file.name", fileName);
        postMethod.addRequestHeader("opcode", opcode);
        postMethod.addRequestHeader("team.leader", "Bernie");
        postMethod.addRequestHeader("file.unique.key", "123");
        postMethod.addRequestHeader("upload.time", (new Date()).toString());
        postMethod.addRequestHeader("eventinfokey", "456");
        postMethod.addRequestHeader("siteid", "987");
        File f = getPretendExcelFile();
        try {
            postMethod.setRequestEntity(new InputStreamRequestEntity(new FileInputStream(f)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return send(postMethod);
    }

    public static void main(String[] args) throws Exception{
        Scanner in = new Scanner(System.in);

        System.out.println("Which environment?");
        System.out.println("1. eiConsole test mode");
        System.out.println("2. eiConsole emulator");
        System.out.println("3. eiPlatform");
        System.out.print("Choice: ");
        int env = Integer.parseInt(in.nextLine());
        switch(env){
            case TEST_MODE:
                servletContext = EI_CONSOLE_CONTEXT;
                break;
            case EMULATOR:
            case EIP:
                servletContext = EIP_CONTEXT;
                break;
            default:
                throw new IllegalArgumentException("Illegal selection for environment: " + env);
        }

        System.out.print("Enter a test opcode: ");
        opcode = in.nextLine();
        System.out.print("Enter a test email: ");
        teamLeaderEmail = in.nextLine();
        System.out.print("Enter a test filename: ");
        fileName = in.nextLine();

        System.out.println("\nPreparing Http Post");
        System.out.println("Request URL: " + getRequestUrl());
        System.out.println("OpCode: " + opcode);
        System.out.println("Team Leader Email: " + teamLeaderEmail);
        System.out.println("File Name: " + fileName);
        System.out.print("\nPress 'enter' to continue");
        in.nextLine();

        System.out.println("\nSending Http Post");
        int status = sendAsFile();
        if(status >= 0){
            System.out.println("Status Code: " + status);
        }
    }
}