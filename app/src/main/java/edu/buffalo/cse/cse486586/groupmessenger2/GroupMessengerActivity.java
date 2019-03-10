package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
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
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    TextView textView;
    EditText editText;
    int selfProcessId, messageId=0;
    int[] remoteProcessIds = new int[]{11108, 11112, 11116, 11120, 11124};
    Uri uri;
    boolean spawnedClients = false;

    ArrayList<LinkedBlockingQueue<Message>> blockingQueueList;

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
        int i = 0;
        for(int remoteProcessId: remoteProcessIds) {
            Thread t = new ClientThread(selfProcessId, remoteProcessId, blockingQueueList.get(i));
            t.start();
            i++;
        }
        spawnedClients = true;
    }

    private void updateBlockingList(Message message){
        for(LinkedBlockingQueue linkedBlockingQueue: blockingQueueList ) {
            try {
                linkedBlockingQueue.put(message);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /* Initialize the shared queue*/
        blockingQueueList = new ArrayList<LinkedBlockingQueue<Message>>(remoteProcessIds.length);
        for(int i=0; i<remoteProcessIds.length; i++)
            blockingQueueList.add(new LinkedBlockingQueue<Message>());

        /* Get the process ID using telephone number*/
        selfProcessId = getProcessId();

        /* Uri for the content provider */
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
        uriBuilder.scheme("content");
        uri = uriBuilder.build();

        /* Starts the Server Interactor Spawner */
        Log.d("UI", "Server Started");
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

                updateBlockingList(new Message(messageId, message));
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

        /* https://stackoverflow.com/questions/8105545/can-i-select-a-color-for-each-text-line-i-append-to-a-textview*/
        public void appendColoredText(TextView tv, String text, int color) {
            int start = tv.getText().length();
            tv.append(text);
            int end = tv.getText().length();

            Spannable spannableText = (Spannable) tv.getText();
            spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
        }

        @Override
        public synchronized void run() {
            int color = 0;
            switch (from){
                case 5554:  color = Color.MAGENTA;  break;
                case 5556:  color = Color.BLACK;    break;
                case 5558:  color = Color.GREEN;    break;
                case 5560:  color = Color.BLUE;     break;
                case 5562:  color = Color.RED;      break;
                default:    color = Color.DKGRAY;   break;
            }
            String line = "("+from + ") : " + message;
            appendColoredText(textView, line, color);
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
                    Log.d(TAG, "Connection ?");
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
                Log.d(TAG, "Incoming");
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
                while (true) {
                    String message = ois.readUTF();
                    Log.d(TAG, message);
                    /* https://stackoverflow.com/questions/12716850/android-update-textview-in-thread-and-runnable
                     * Update the UI thread */
                    runOnUiThread(new UpdateTextView(message, clientProcessId));
                    //                writeToContentProvider(key, message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }


    private class ClientThread extends Thread {
        final String TAG = "CLIENT_THREAD";
        int remoteProcessId, selfProcessId;
        ObjectOutputStream oos;
        ObjectInputStream ois;
        LinkedBlockingQueue<Message> queue;

        public ClientThread(int selfProcessId, int remoteProcessId, LinkedBlockingQueue<Message> queue){
            this.remoteProcessId = remoteProcessId;
            this.selfProcessId = selfProcessId;
            this.queue = queue;
        }

        @Override
        public void run() {
            Message message;

            try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            remoteProcessId);

                    /*
                     * https://stackoverflow.com/questions/5680259/using-sockets-to-send-and-receive-data
                     * https://stackoverflow.com/questions/49654735/send-objects-and-strings-over-socket
                     */
                    /* Streams */
                    this.oos = new ObjectOutputStream(socket.getOutputStream());
                    this.ois = new ObjectInputStream(socket.getInputStream());

                    /* One-time operation. Send server the client's process id*/
                    oos.writeInt(this.selfProcessId);

                    while(true){
                        /* Retrieve the message from blocking queue */
                        Log.d(TAG, "to id " + remoteProcessId);
                        message = queue.take();
                        oos.writeUTF(message.toString());
                        oos.flush();
                    }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientThread UnknownHostException port=" + remoteProcessId);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ClientThread socket IOException");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
