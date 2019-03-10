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
import java.util.HashMap;
import java.util.Map;

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
    Uri uri;

    Map<Integer, ClientHelper> socketMap = new HashMap<Integer, ClientHelper>();

    private int getProcessId(){
        final int selfProcessIdLen = 4;
        String telephoneNumber =
                ((TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
        int length = telephoneNumber.length();
        telephoneNumber = telephoneNumber.substring(length - selfProcessIdLen);
        int id = Integer.parseInt(telephoneNumber);
        return id;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

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

                /* Don't send empty messages */
                if(message.equals("\n") || message.equals(" \n"))
                    return;
                editText.setText("");

                //, message, messageId
                /* Update the message id here as it is threadsafe.
                * Last 4 digits are process id and message/1000 is the message number
                * */

                new ClientThread().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Message(messageId, message));
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
                case 5558:  color = Color.DKGRAY;    break;
                case 5560:  color = Color.BLUE;     break;
                case 5562:  color = Color.RED;      break;
                default:    color = Color.BLACK;   break;
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

    private class ClientHelper{
        Socket socket;
        int remoteProcessId;
        ObjectOutputStream oos;
        ObjectInputStream ois;
        final String TAG = "CLIENT_HELPER";

        ClientHelper(int remoteProcessId, Socket socket){
            this.socket = socket;
            this.remoteProcessId = remoteProcessId;
            try {
                /*
                 * https://stackoverflow.com/questions/5680259/using-sockets-to-send-and-receive-data
                 * https://stackoverflow.com/questions/49654735/send-objects-and-strings-over-socket
                 */
                /* Streams */
                this.oos = new ObjectOutputStream(socket.getOutputStream());
                this.ois = new ObjectInputStream(socket.getInputStream());
                /* One-time operation. Send server the client's process id*/
                this.oos.writeInt(selfProcessId);
                Log.d(TAG, "to id " + remoteProcessId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void unicastToRemote(Message message){
            try {
                oos.writeUTF(message.toString());
                oos.flush();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientThread UnknownHostException port=" + remoteProcessId);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ClientThread socket IOException");
            }
        }
    }

    private class ClientThread extends AsyncTask<Message, Void, Void> {
        final String TAG = "CLIENT_THREAD";
        final int[] remoteProcessIds = new int[]{11108, 11112, 11116, 11120, 11124};


        protected void establishConnection(){
                /* Establish the connection to server and store it in a Hashmap*/
                for (int remoteProcessId : remoteProcessIds) {
                    Socket socket = null;
                    try {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                remoteProcessId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    socketMap.put(remoteProcessId, new ClientHelper(remoteProcessId, socket));
                }
            }

            @Override
            protected Void doInBackground(Message... msgs) {
                Message message = msgs[0];

                //For the first time attempt to establish connection
                if(socketMap.size() == 0)
                    establishConnection();

                for(int remoteProcessId : socketMap.keySet())
                    socketMap.get(remoteProcessId).unicastToRemote(message);
                return null;
        }
    }
}
