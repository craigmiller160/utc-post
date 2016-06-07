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

    static String PILOTFISH_HTTP_SERVER = "http://localhost:8443";
    static String SERVLET_CONTEXT = "";
    static String LISTENER_CONTEXT = "http-post";
    static String REQUEST_PATH = "testPost";

    static String OPCODE;
    static String TEAM_LEADER_EMAIL;
    static String FILENAME;

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

    private static void send(PostMethod postMethod) {
        HttpClient httpClient = new HttpClient();
        try {
            postMethod.setURI(new URI(String.format("%s/%s/%s/%s", PILOTFISH_HTTP_SERVER,SERVLET_CONTEXT,LISTENER_CONTEXT,REQUEST_PATH), true));
            httpClient.executeMethod(postMethod);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendAsList() {
        PostMethod postMethod = new PostMethod();
        postMethod.setRequestEntity(new StringRequestEntity(String.format(PRETEND_UPLOAD_QUEUE, TEAM_LEADER_EMAIL, FILENAME, OPCODE)));
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

    private static void sendAsFile() {
        PostMethod postMethod = new PostMethod();
        postMethod.addRequestHeader("team.leader.email", TEAM_LEADER_EMAIL);
        postMethod.addRequestHeader("upload.file.name", FILENAME);
        postMethod.addRequestHeader("opcode", OPCODE);
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
        send(postMethod);
    }

    public static void main(String[] args) {
        Scanner keysIn = new Scanner(System.in);
        System.out.println("Which environment?");
        System.out.println("1. test mode");
        System.out.println("2. emulator");
        System.out.println("3. EIP");
        String env = keysIn.nextLine();
        if (env.equals("1")) {
            SERVLET_CONTEXT = "eiConsole";
        }
        if (env.equals("2")) {
            SERVLET_CONTEXT = "eip";
        }
        System.out.println("Which method to use?");
        System.out.println("1. upload list item");
        System.out.println("2. upload file");
        String methodChoice = keysIn.nextLine();
        System.out.println("Pretend to upload from remote server?");
        REQUEST_PATH += keysIn.nextLine().matches("(?i)y.*") ? "Remote" : "";
        System.out.println("Enter a test opcode:");
        OPCODE = keysIn.nextLine();
        System.out.println("Enter a test email:");
        TEAM_LEADER_EMAIL = keysIn.nextLine();
        System.out.println("Filename to pretend to upload:");
        FILENAME = keysIn.nextLine();
        if (methodChoice.equals("1")) {
            sendAsList();
        }
        if (methodChoice.equals("2")) {
            sendAsFile();
        }
        System.out.println("\nSent.  Hopefully.");
    }
}