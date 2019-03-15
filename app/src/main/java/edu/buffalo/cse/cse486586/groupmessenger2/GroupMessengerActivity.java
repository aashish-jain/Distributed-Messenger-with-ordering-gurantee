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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    TextView textView;
    EditText editText;
    int selfProcessId=0, messageId=0, idIncrementValue = 0;
    static final int selfProcessIdLen = 4, initCapacity = 100, socketTimeout = 5000;
    long proposalNumber;
    Uri uri;
    AtomicInteger contentProviderKey;
    PriorityQueue<Message> deliveryQueue;
    LinkedBlockingQueue<Message> DeliveryManagerQueue;
    Map<Integer, Client> clientMap;

    private int getProcessId(){
        String telephoneNumber =
                ((TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
        int length = telephoneNumber.length();
        telephoneNumber = telephoneNumber.substring(length - selfProcessIdLen);
        int id = Integer.parseInt(telephoneNumber);
        return id;
    }

    private void writeToContentProvider(int key, String message){
        ContentValues contentValues = new ContentValues();
        contentValues.put("key", new Integer(key).toString());
        contentValues.put("value", message);
        getContentResolver().insert(uri, contentValues);
    }

    public void notifyFailure(int id){
        Message failureMessage = new Message(id,"");
        failureMessage.setFailure();
        try {
            DeliveryManagerQueue.put(failureMessage);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    public String showPriorityQueue(PriorityQueue<Message> priorityQueue) {
        StringBuilder stringBuilder = new StringBuilder();
        PriorityQueue<Message> pq = new PriorityQueue<Message>(priorityQueue);
        int i = pq.size();
        for (; i!=0; i--){
            stringBuilder.append( pq.poll().allData());
        }
        return stringBuilder.toString();
    }

    private synchronized void updateProposalNumber(long proposed){
        /* Update Proposed to maximum */
        long selfId = proposalNumber % idIncrementValue;
        proposed -= proposed % idIncrementValue;
        if (proposalNumber - selfId < proposed)
            proposalNumber = proposed + selfId;
    }

    private synchronized long getNewProposalNumber(long by){
        /* Send a proposal with last 'n' digits with process ID*/
        proposalNumber += by;
        return proposalNumber;
    }

    public Message findInPriorityQueue(PriorityQueue<Message> priorityQueue, int id) {
        for (Message priorityQueueIterator : priorityQueue)
            if (priorityQueueIterator.getId() == id)
                return priorityQueueIterator;
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /* Get the process ID using telephone number*/
        selfProcessId = getProcessId();
        /* Client Map*/
        clientMap = new HashMap<Integer, Client>();
        /* Content Provider Key*/
        contentProviderKey = new AtomicInteger(0);
        /* Proposal Number*/
        proposalNumber = selfProcessId;
        /* Id increment value*/
        idIncrementValue = (int) Math.pow(10, selfProcessIdLen);
        /* delivery Queue */
        deliveryQueue = new PriorityQueue<Message>(initCapacity, new Comparator<Message>() {
            @Override
            public int compare(Message lhs, Message rhs) {
                if(lhs.getPriority() > rhs.getPriority())
                    return 1;
                else
                    return -1;
            }
        });
        /* Proposal Queue */
        DeliveryManagerQueue = new LinkedBlockingQueue<Message>(initCapacity);
        /* Process Id generator */
        messageId = selfProcessId;
        /* Get the textView and the editText of the activity */
        textView = (TextView) findViewById(R.id.textView1);
        textView.setMovementMethod(new ScrollingMovementMethod());
        editText = (EditText) findViewById(R.id.editText1);
        textView = (TextView) findViewById(R.id.textView1);
        /* Print my ID */
        textView.append("My ID is "+selfProcessId+"\n");

        /* Uri for the content provider */
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
        uriBuilder.scheme("content");
        uri = uriBuilder.build();

        /* Starts the Server Thread Spawner */
        new ServerThreadSpawner(selfProcessId).start();
        /* Starts the Proposal Handler */
        new DeliveryQueueManager().start();

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
                * Last 4 digits are process id and message/10000 is the message number
                * */

                new ClientThreadSpawner().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Message(messageId, message));
                //Update the Id
                messageId+= idIncrementValue;
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
                case 5558:  color = Color.DKGRAY;   break;
                case 5560:  color = Color.BLUE;     break;
                case 5562:  color = Color.RED;      break;
                default:    color = Color.BLACK;    break;
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
                    socket.setSoTimeout(socketTimeout);
                    new ServerThread(socket, this.selfProcessId).start();
                } catch (IOException e) {
                    e.printStackTrace();
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
                this.selfId = selfId;
                this.ois = new ObjectInputStream(socket.getInputStream());
                this.oos = new ObjectOutputStream(socket.getOutputStream());
                this.clientProcessId = ois.readInt();
            } catch (Exception e) {
                Log.e(TAG, "Failure");
                notifyFailure(this.clientProcessId);
            }
        }

        private void respondToProposal(Message message) throws IOException, InterruptedException {

            long proposal = getNewProposalNumber(idIncrementValue);
            message.setPriority(proposal);
            oos.writeLong(proposal);
            oos.flush();
            DeliveryManagerQueue.put(message);
        }

        private void processAgreement(Message message) throws InterruptedException {
            message.setToDeliverable();
            DeliveryManagerQueue.put(message);
            updateProposalNumber(message.getPriority());
        }

        @Override
        public void run() {
            //Read from the socket
            try {
                while (true) {
                    Message message = new Message(ois.readUTF());
                    if(message.isProposal())
                        respondToProposal(message);
                    else if(message.isAgreement())
                        processAgreement(message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failure");
                notifyFailure(this.clientProcessId);
            }
        }
    }

    private class ClientThreadSpawner extends AsyncTask<Message, Message, Void> {
        final String TAG = "CLIENT_TASK";
        final int[] remoteProcessIds = new int[]{11108, 11112, 11116, 11120, 11124};


        protected void establishConnection(){

            /* Establish the connection to server and store it in a Hashmap*/
            for (int remoteProcessId : remoteProcessIds) {
                Socket socket = null;
                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            remoteProcessId);
                    socket.setSoTimeout(socketTimeout);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to establish connection with the server");
                }
                Client client = new Client(remoteProcessId, socket);
                clientMap.put(remoteProcessId, client);
            }
        }

        @Override
        protected Void doInBackground(Message... msgs) {
            Message message = msgs[0];

            //For the first time attempt to establish connection
            if(clientMap.size() == 0) {
                establishConnection();
            }

            LinkedList<Integer> toRemove = new LinkedList<Integer>();
            if(message.isProposal()) {
                for (int remoteProcessId : clientMap.keySet())
                    try {
                        clientMap.get(remoteProcessId).requestProposal(message);
                    } catch (Exception e) {
                        Log.e(TAG, "Failure Detected in Proposal");
                        toRemove.add(remoteProcessId);
                        notifyFailure(remoteProcessId);
                    }
                message.agree();
                /* https://stackoverflow.com/questions/5779894/calling-an-asynctask-from-another-asynctask*/
                publishProgress(message);
            }
            else if(message.isAgreement())
                for(int remoteProcessId : clientMap.keySet())
                    try {
                        clientMap.get(remoteProcessId).sendAgreement(message);
                    } catch (Exception e) {
                        Log.e(TAG, "Failure detected in agreement");
                        toRemove.add(remoteProcessId);
                        notifyFailure(remoteProcessId);
                    }

            for(int remoteProcessId : toRemove)
                clientMap.remove(remoteProcessId);

            return null;
        }

        protected void onProgressUpdate(Message...messages){
            new ClientThreadSpawner().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, messages[0]);
        }
    }

    private class Client {
        Socket socket;
        int remoteProcessId;
        ObjectOutputStream oos;
        ObjectInputStream ois;
        final String TAG = "CLIENTOBJ";
        Client(int remoteProcessId, Socket socket) {
            this.socket = socket;
            this.remoteProcessId = remoteProcessId;
            try {
                /* Streams */
                this.oos = new ObjectOutputStream(socket.getOutputStream());
                this.ois = new ObjectInputStream(socket.getInputStream());
                /* One-time operation. Send server the client's process id*/
                this.oos.writeInt(selfProcessId);
                oos.flush();
            } catch (Exception e) {
                Log.e(TAG, "Failure detected in connecting with server");
                notifyFailure(this.remoteProcessId);
            }
        }

        public void requestProposal(Message message) throws Exception {
            oos.writeUTF(message.encodeMessage());
            oos.flush();
            long proposed = ois.readLong();
            message.setPriority(proposed);
        }

        public void sendAgreement(Message message) throws Exception {
            oos.writeUTF(message.encodeMessage());
            oos.flush();
        }
    }


    private class DeliveryQueueManager extends Thread{
        final String TAG = "PROPOSAL_HANDLER";

        /** Gets the first message from the delivery queue and check if it is deliverable
         * Then delivers all the deliverable messages at the beginning of the queue
         */
        private void deliverAllDeliverable(){
            Message message = deliveryQueue.peek();
            while (message!=null && message.isDeliverable()) {
                /* https://stackoverflow.com/questions/12716850/android-update-textview-in-thread-and-runnable
                 * Update the UI thread */
                runOnUiThread(new UpdateTextView(message.getMessage(), message.getId() % idIncrementValue));
                deliveryQueue.poll();
                writeToContentProvider(contentProviderKey.getAndIncrement(), message.getMessage());
                message = deliveryQueue.peek();
            }
        }

        private void reUpdateMessages(Message message) {
            /* Find the message with given Id, update it and re-insert to the queue*/
            Message queueMessage = findInPriorityQueue(deliveryQueue, message.getId());

            if (queueMessage != null) {
                deliveryQueue.remove(queueMessage);
                message.setPriority(queueMessage.getPriority());
            }
            deliveryQueue.offer(message);

        }

        @Override
        public void run(){
            try {
                while(true) {
                    /* Take the message from the proposal queue */
                    Message message = DeliveryManagerQueue.take();

                    if(message.isFailure())
                        removeFailures(message);
                    else
                        reUpdateMessages(message);
                    deliverAllDeliverable();
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "DeliveryQueueManager Interrupted");
            }
        }

        private void removeFailures(Message message) {
            if(!message.isFailure())
                return;
            LinkedList<Message> removeList = new LinkedList<Message>();
            for (Message priorityQueueIterator : deliveryQueue)
                if (priorityQueueIterator.getSender(idIncrementValue) == message.getSender(idIncrementValue))
                    removeList.add(priorityQueueIterator);

            for(Message message1: removeList)
                deliveryQueue.remove(message1);
        }
    }
}
