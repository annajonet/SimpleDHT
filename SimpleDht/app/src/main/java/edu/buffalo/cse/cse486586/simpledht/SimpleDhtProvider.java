package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    static ArrayList<Integer> avds = new ArrayList<Integer>();
    static int predecessor = 0;
    static int successor = 0;
    static String queryRecords = "";

    private void callServer(Message message, int port){
        try {
            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    port*2);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            BufferedReader br3 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.writeObject(message);
            String ack = br3.readLine();
            out.close();
            br3.close();
            socket.close();
        } catch (UnknownHostException e) {
            Log.e("error", "ClientTask UnknownHostException");
        } catch (IOException e) {
            Log.e("error", "ClientTask socket IOException");
        }catch (Exception e) {
            Log.e("error","Exception : " + e);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        try{
            TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            String currentAvd = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            Message msgToSend = new Message(Constants.DELETE,selection,"",Integer.parseInt(currentAvd));
            if(selection.equals(Constants.LOCALQ) || (selection.equals(Constants.GLOBALQ) && avds.size() == 1)){
                localDelete();
            }else if(selection.equals(Constants.GLOBALQ)){
                callServer(msgToSend,successor);
            }else{
                int  coordinator = 0;
                try{
                    String hashKey = genHash(selection);
                    for(int i=0;i<avds.size();i++){

                        if(hashKey.compareTo(genHash(Integer.toString(avds.get(i)))) <= 0){
                            coordinator = avds.get(i);
                            break;
                        }
                    }
                    if(coordinator == 0){
                        coordinator = avds.get(0);
                    }
                }catch (NoSuchAlgorithmException ex){
                    Log.e("error", "no such algo exception from client for key"+selection);
                }
                if(coordinator == Integer.parseInt(currentAvd)){
                    deleteRecord(selection);
                }else{
                    msgToSend.setCooordinator(coordinator);
                    callServer(msgToSend,successor);
                }
            }

        }catch(Exception e){
            Log.e("error","Exception in delete()"+e);
        }
        return 0;
    }

    public int localDelete(){
        int count = 0;
        File file = new File(String.valueOf(getContext().getFilesDir()));
        File[] internalFiles = file.listFiles();
        for(int i=0;i < internalFiles.length;i++){
            deleteRecord(internalFiles[i].getName());
            count++;
        }
        return count;
    }
    public void deleteRecord(String key){
        try {
            File dir = getContext().getFilesDir();
            File file = new File(dir, key);
            boolean deleted = file.delete();
        }catch(Exception e){
            Log.e("error","Exception @ delete()"+e);
        }
    }
    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }
    private void sortPorts(String currentPort){
        Collections.sort(avds, new Comparator<Integer>(){
            @Override
            public  int compare(Integer one, Integer two){
                int returnVal = 0;
                try{
                    returnVal = genHash(Integer.toString(one)).compareTo(genHash(Integer.toString(two)));
                }
                catch(NoSuchAlgorithmException ex){
                    Log.e("error","no such algo exception in : findSuccessors()");
                }
                return returnVal;
            }
        });
        int currentIndex = avds.indexOf(Integer.parseInt(currentPort));
        successor = avds.get((currentIndex+1)%avds.size());
        predecessor = avds.get((currentIndex+(avds.size()-1))%avds.size());
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);


        String key = values.getAsString("key");
        String value = values.getAsString("value");
        try{
            int  coordinator = 0;
            try{
                String hashKey = genHash(key);
                for(int i=0;i<avds.size();i++){

                    if(hashKey.compareTo(genHash(Integer.toString(avds.get(i)))) <= 0){
                        coordinator = avds.get(i);
                        break;
                    }
                }
                if(coordinator == 0){
                    coordinator = avds.get(0);
                }
            }catch (NoSuchAlgorithmException ex){
                Log.e("error", "no such algo exception from client for key"+key);
            }
            Message msgToSend = new Message(Constants.INSERT, key, value, Integer.parseInt(portStr));
            msgToSend.setCooordinator(coordinator);
            callServer(msgToSend,Integer.parseInt(portStr));
        } catch(Exception ex){
            Log.e("error", "InsertTask Exception"+ex);
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub

        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        try{
        ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT);
        new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }catch (IOException e) {

            Log.e("error", e.getMessage());
        }
        joinRing(portStr);
        return false;
    }

    public void joinRing(String portStr){

        if(!portStr.equals(Constants.PORT_STRING)){
            try {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,portStr);
            }catch(Exception e){
                Log.e("error","Exception"+e);
            }
        }else{
            avds.add(Constants.PORT);

        }

    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        String[] colNames = {"key", "value"};
        MatrixCursor data = new MatrixCursor(colNames);
        try {
            TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            String currentAvd = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            Message msgToSend = new Message(Constants.QUERY,selection,"",Integer.parseInt(currentAvd));
            if (selection.equals(Constants.LOCALQ) || (selection.equals(Constants.GLOBALQ) && (avds.size() == 1))) {
                queryRecords = localQuery();
            } else if (selection.equals(Constants.GLOBALQ)) {
                queryRecords = null;
                callServer(msgToSend,successor);
                while (queryRecords == null){

                }
            } else {
                int  coordinator = 0;
                try{
                    String hashKey = genHash(selection);
                    for(int i=0;i<avds.size();i++){

                        if(hashKey.compareTo(genHash(Integer.toString(avds.get(i)))) <= 0){
                            coordinator = avds.get(i);
                            break;
                        }
                    }
                    if(coordinator == 0){
                        coordinator = avds.get(0);
                    }
                }catch (NoSuchAlgorithmException ex){
                    Log.e("error", "no such algo exception from client for key"+selection);
                }
                if(coordinator == Integer.parseInt(currentAvd)){
                   queryRecords = retrieveLocalRecord(selection);
                }else{
                    queryRecords = "";
                    msgToSend.setCooordinator(coordinator);
                    callServer(msgToSend,successor);
                    while (queryRecords.isEmpty()){

                    }
                }
            }
            if (!queryRecords.isEmpty()) {

                String[] records = queryRecords.split("\\*");
                for (String rec : records) {
                    String[] record = rec.split("\\>");
                    data.addRow(new Object[]{record[0], record[1]});
                }
            }
        }catch (Exception e){
            Log.e("error","Exception "+e);
        }

        return data;
    }
    public String localQuery(){
        String records = "";
        File file = new File(String.valueOf(getContext().getFilesDir()));
        File[] internalFiles = file.listFiles();
        for(int i=0;i < internalFiles.length;i++){
            if(internalFiles[i] != null) {
                String rec = retrieveLocalRecord(internalFiles[i].getName());
                records = (records.isEmpty()) ? rec : records + "*" + rec;
            }
        }
        return records;
    }

    public String retrieveLocalRecord(String key){
        String records = "";
        try {
            FileInputStream inputStream = getContext().openFileInput(key);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String value = "";

            if((value = reader.readLine()) != null){
                records = (records.isEmpty())?key+">"+value:records+"*"+key+">"+value;
            }
            inputStream.close();

        }

        catch (FileNotFoundException e) {
            Log.e("error","no such entry");
        } catch (IOException e) {
            Log.e("error","io exception");
        }
        return records;
    }
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while(true){
                try{
                    TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
                    String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
                    Socket clSocket = serverSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(clSocket.getInputStream());
                    PrintWriter pr = new PrintWriter(clSocket.getOutputStream());
                    Message message = (Message) in.readObject();
                    String reply = "";
                    if(message != null){
                        if(message.isRequest(Constants.JOIN)){
                            if(portStr.equals(Constants.PORT_STRING)) {
                                avds.add(message.getRequester());
                                message.setPorts(avds);
                            }else{
                                avds = message.getPorts();
                            }
                            sortPorts(portStr);
                            reply = "Gotcha!";
                            if(portStr.equals(Constants.PORT_STRING)) {
                                for (int avd : avds) {
                                    if (avd == Constants.PORT) {
                                        continue;
                                    }
                                    try {
                                        callNextServer(message,avd);
                                    } catch (Exception e) {
                                        Log.e("error", "Exception : " + e);
                                    }

                                }
                            }
                        }else if(message.isRequest(Constants.INSERT)){
                            reply = "Gotcha!";
                            if(message.getCooordinator() == Integer.parseInt(portStr)){
                                FileOutputStream outputStream;
                                try {
                                    outputStream = getContext().openFileOutput(message.getKey(), Context.MODE_PRIVATE);

                                    outputStream.write(message.getValue().getBytes());
                                    outputStream.close();
                                } catch (Exception e) {
                                    Log.e("error","error saving key : " + message.getKey());
                                }
                            }else{
                                //pass to succ
                                try {
                                    callNextServer(message,successor);
                                } catch (Exception e) {
                                    Log.e("error","error inserting key : " + message.getKey());
                                }

                            }

                        }else if(message.isRequest(Constants.QUERY)){       //Query, requestPort, key, receiver, foundValue
                            if(!message.getKey().equals("*")){
                                if(message.getCooordinator() == Integer.parseInt(portStr)){
                                    queryRecords = retrieveLocalRecord(message.getKey());
                                    message.setFoundValue(queryRecords);
                                }
                                reply = "Gotcha!";
                                if(message.getRequester() == Integer.parseInt(portStr)){     //reached back to the requester
                                    queryRecords = message.getFoundValue();

                                }else {
                                    callNextServer(message, successor);
                                }
                            }else{
                                if(message.getRequester() == Integer.parseInt(portStr)){     //reached back to the requester
                                    String localRec  = localQuery();
                                    if(localRec.isEmpty()){
                                        queryRecords = (message.getFoundValue() == null)?"":message.getFoundValue();
                                    }else {
                                        queryRecords = (message.getFoundValue() == null || message.getFoundValue().isEmpty()) ? localRec : message.getFoundValue() + "*" + localRec;
                                    }
                                    reply = "Gotcha!";
                                }else{
                                    if(message.getFoundValue() == null || message.getFoundValue().isEmpty()) {
                                        queryRecords = "";
                                    }else{
                                        queryRecords = message.getFoundValue();
                                    }
                                    String localRec = localQuery();
                                    if(!localRec.isEmpty()){
                                        queryRecords = (queryRecords.isEmpty())? localRec:queryRecords+ "*" +localRec ;
                                    }
                                    message.setFoundValue(queryRecords);
                                    callNextServer(message,successor);
                                    reply = "Gotcha!";
                                }
                            }


                        }else if(message.isRequest(Constants.DELETE)){
                            if(message.getKey().equals("*")) {
                                localDelete();
                                reply = "Gotcha!";
                                if (!(message.getRequester() == Integer.parseInt(portStr))) {
                                    callNextServer(message, successor);
                                }
                            }else{
                                if(message.getCooordinator() == Integer.parseInt(portStr)){
                                    deleteRecord(message.getKey());
                                }
                                reply = "Gotcha!";
                                if (!(message.getRequester() == Integer.parseInt(portStr))) {
                                    callNextServer(message, successor);
                                }
                            }
                        }
                    }


                   pr.println(reply);
                   pr.flush();
                   pr.close();
                    in.close();
                   clSocket.close();

                }
                catch (Exception e){
                    Log.e("error","Exception : "+e);
                }

            }

        }
        private void callNextServer(Message message, int port){
            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        port*2);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                BufferedReader br3 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.writeObject(message);
                String ack = br3.readLine();
                out.close();
                br3.close();
                socket.close();
            } catch (UnknownHostException e) {
                Log.e("error", "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e("error", "ClientTask socket IOException");
            }catch (Exception e) {
                Log.e("error","Exception : " + e);
            }
        }

        protected void onProgressUpdate(String...msgs) {

           return;
        }
    }
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Constants.PORT * 2);
                Message msgToSend = new Message(Constants.JOIN,"","",Integer.parseInt(msgs[0]));
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.writeObject(msgToSend);
                String ack = br.readLine();
                out.close();
                br.close();
                socket.close();

            } catch (UnknownHostException e) {
                Log.e("error", "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e("error", "ClientTask socket IOException");
            }

            return null;
        }
    }
    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
