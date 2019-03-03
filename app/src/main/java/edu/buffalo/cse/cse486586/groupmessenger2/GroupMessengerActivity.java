package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static TextView textView;
    static EditText editText;
    static int selfProcessId, messageId=0;
    static int[] remoteProcessIds = new int[]{11108, 11112, 11116, 11120, 11124};
    static Uri uri;
    static boolean spawnedClients = false;
    static SharedQueue sharedQueue;

    private int getProcessId(){
        final int selfProcessIdLen = 4;
        String telephoneNumber =
                ((TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
        int length = telephoneNumber.length();
        telephoneNumber = telephoneNumber.substring(length - selfProcessIdLen);
        int id = Integer.parseInt(telephoneNumber);
        return id;
    }

    private void spawnClientThreads(){
        /* Spawn new thread to connect to every peer */
        for(int remoteProcessId: remoteProcessIds)
            AsyncTask.execute(new ClientThread(selfProcessId, remoteProcessId));
        spawnedClients = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /* Initialize the shared queue*/
        sharedQueue = new SharedQueue(remoteProcessIds.length);

        /* Get the process ID using telephone number*/
        selfProcessId = getProcessId();

        /* Uri for the content provider */
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
        uriBuilder.scheme("content");
        uri = uriBuilder.build();

        /* Starts the Server Interactor Spawner */
        new ServerThreadSpawner(selfProcessId).start();


        /* Process Id generator */
        messageId = selfProcessId;

        /* Get the textView and the editText of the activity */
        textView = (TextView) findViewById(R.id.textView1);
        textView.setMovementMethod(new ScrollingMovementMethod());
        editText = (EditText) findViewById(R.id.editText1);
        textView = (TextView) findViewById(R.id.textView1);

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(textView, getContentResolver()));

        /* https://developer.android.com/reference/android/widget/Button */
        Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editText.getText().toString() + "\n";
                editText.setText("");

                //, message, messageId
                /* Update the message id here as it is threadsafe.
                * Last 4 digits are process id and message/1000 is the message number
                * */
                if(!spawnedClients)
                    spawnClientThreads();
                sharedQueue.addToQueue(new Message(messageId, message));
                messageId+=10000;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    //https://stackoverflow.com/questions/5853167/runnable-with-a-parameter/5853198
    private class UpdateTextView implements Runnable{
        String message;
        int from;

        UpdateTextView(String message, int from){
            this.message = message;
            this.from = from;
        }

        @Override
        public void run() {
            textView.append(from + " : " + message);
        }
    }

    //https://stackoverflow.com/questions/10131377/socket-programming-multiple-client-to-one-server
    private class ServerThreadSpawner extends Thread {
        static final int SERVER_PORT = 10000;
        static final String TAG = "SERVER_TASK";
        int selfProcessId;


        public ServerThreadSpawner(int selfProcessId){
            this.selfProcessId = selfProcessId;
            Log.d(TAG, "Got selfProcessId = " + selfProcessId);
        }

        public void run() {
            /* Open a socket at SERVER_PORT */
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }

            /* Accept a client connection and spawn a thread to respond */
            while (true)
                try {
                    Socket socket = serverSocket.accept();
                    new ServerThread(socket, this.selfProcessId).start();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "ServerThreadSpawner socket IOException");
                }
        }
    }


    /* https://stackoverflow.com/questions/10131377/socket-programming-multiple-client-to-one-server*/
    private class ServerThread extends Thread {
        ObjectOutputStream oos;
        ObjectInputStream ois;
        int selfId, clientProcessId;

        static final String TAG = "SERVER_THREAD";


        public  ServerThread(Socket socket, int selfId) {
            try {
                this.ois = new ObjectInputStream(socket.getInputStream());
                this.oos = new ObjectOutputStream(socket.getOutputStream());
                this.clientProcessId = ois.readInt();
                this.selfId = selfId;
            } catch (IOException e) {
                Log.e(TAG, "Unable to connect");
                e.printStackTrace();
            }
        }

        private void writeToContentProvider(int key, String message){
            ContentValues contentValues = new ContentValues();
            contentValues.put("key", new Integer(key).toString());
            contentValues.put("value", message);
            key++;
            getContentResolver().insert(uri, contentValues);

        }

        @Override
        public void run() {
            //Read from the socket
            try {
                String message = ois.readUTF();

                /* https://stackoverflow.com/questions/12716850/android-update-textview-in-thread-and-runnable
                 * Update the UI thread */
                runOnUiThread(new UpdateTextView(message, clientProcessId));
//                writeToContentProvider(key, message);


                //Acknowledgement
                oos.writeByte(255);
                oos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    private class ClientThread implements Runnable {
        final String TAG = "CLIENT_THREAD";
        int remoteProcessId, selfProcessId;
        ObjectOutputStream oos;
        ObjectInputStream ois;

        public ClientThread(int selfProcessId, int remoteProcessId){
            this.remoteProcessId = remoteProcessId;
            this.selfProcessId = selfProcessId;
        }

        @Override
        public void run() {
            Message message;
            try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            remoteProcessId);

                    //https://stackoverflow.com/questions/5680259/using-sockets-to-send-and-receive-data
                    //https://stackoverflow.com/questions/49654735/send-objects-and-strings-over-socket
                    /* Streams */
                    this.oos = new ObjectOutputStream(socket.getOutputStream());
                    this.ois = new ObjectInputStream(socket.getInputStream());
                    /* Retrieve the message from blocking queue */
                    message = sharedQueue.popFront();
                    oos.writeInt(this.selfProcessId);
                    oos.writeUTF(message.toString());
                    oos.flush();

                    /* Get ACK */
                    ois.readByte();
                    oos.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientThread UnknownHostException port=" + remoteProcessId);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ClientThread socket IOException");
            } finally {
            }
        }
    }
}
