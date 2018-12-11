package com.mattymatty;

import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("WeakerAccess")
public class ServerController implements Runnable{

    private static AtomicInteger ctn = new AtomicInteger();

    static private Map<String,BOT> botMap = new HashMap<>();

    static private Thread runner;
    static private ServerSocket serverSocket;

    private Set<String> allowedKeys;

    private int port;

    public void run(){
        runner = Thread.currentThread();
        runner.setName("Server Listener");
        try {
            serverSocket = new ServerSocket(port);
            while (!Thread.interrupted()) {
                try {
                    Socket connectionSocket = serverSocket.accept();
                    DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
                    String message = inFromClient.readUTF();
                    if (allowedKeys.contains(message)) {
                        System.out.println("Logged: "+message);
                        BOT old = botMap.get(message);
                        if (old != null) {
                            old.close();
                            System.out.println("Resetted: "+message);
                        }
                        BOT bot = new BOT(message,connectionSocket);
                        botMap.put(message, bot);
                        ExecutorService executor = bot.executorService;
                        executor.execute(new CommandRunnable(message, () -> {
                            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
                            outToClient.writeUTF(new JSONObject().put("ReqID",getUnsignedID()).put("REQUEST", "ping").toString() + "\n"); //ask for ping
                            outToClient.flush();
                        }));

                    } else {
                        connectionSocket.close();
                    }
                }catch (IOException ignored){}
            }
        }catch (IOException ex){
            new Thread(this).start();
        }
    }

    public ServerController(int port,String ... allowedKeys) {
        this.allowedKeys = new HashSet<>(Arrays.asList(allowedKeys));
        this.port = port;
    }

    public JSONObject request(String id, JSONObject req){
        BOT bot = botMap.get(id);
        Socket serverSocket = bot.socket;
        ExecutorService executor = bot.executorService;
        if(serverSocket==null || executor==null)
            return null;
        int reqId = getUnsignedID();
        req.put("ReqID",reqId);
        executor.execute(new CommandRunnable(id,()->{
            DataOutputStream outToClient = new DataOutputStream(serverSocket.getOutputStream());
            outToClient.writeUTF(req.toString());
        }));
        MsgWaiter waiter = new MsgWaiter(reqId);
        bot.addObserver(waiter);

        JSONObject rep;
        try {
            rep = waiter.get();
        } catch (InterruptedException e) {
            return new JSONObject().put("STATUS",500);
        }

        bot.deleteObserver(waiter);

        return rep;
    }


    private int getUnsignedID(){
        return ctn.getAndUpdate(i -> {
            i++;
            return i==Integer.MIN_VALUE?0:i;
        });
    }

    public class CommandRunnable implements Runnable{

        ExeptionRunnable cmd;
        String id;

        @Override
        public void run() {
            try {
                cmd.run();
            }catch (IOException ex){
                BOT bot = botMap.get(id);
                bot.close();
                System.out.println("Died: "+id);
            }
        }

         CommandRunnable(String id, ExeptionRunnable cmd) {
            this.cmd = cmd;
            this.id = id;
        }
    }


    @Override
    public void finalize(){
        close();
    }

    public static void close(){
        if(runner!=null)
            runner.interrupt();
        if(serverSocket!=null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        botMap.values().forEach(BOT::close);
    }

    private static class BOT extends Observable{
        String id;
        Socket socket;
        ExecutorService executorService;
        Thread listener;

         BOT(String id,Socket socket) {
            this.socket = socket;
            this.executorService = Executors.newSingleThreadExecutor((r)->{
                Thread t = new Thread(r);
                t.setName("ServerToBot<"+id+">");
                return t;
            });
            this.listener = new Thread(()->{
                try {
                    while (!Thread.interrupted()) {
                        DataInputStream inFromClient = new DataInputStream(socket.getInputStream());
                        setChanged();
                        notifyObservers(new JSONObject(inFromClient.readUTF()));
                    }
                }catch (IOException ex){
                    close();
                }
            },"Listener<"+id+">");
            listener.start();
        }

         void close(){
            try {
                socket.close();
            }catch (IOException ignored){}
            executorService.shutdownNow();
            listener.interrupt();
            botMap.remove(id);
        }
    }

    private static class MsgWaiter implements Observer{
        private Semaphore sem = new Semaphore(0);
        private Queue<JSONObject> queue = new LinkedList<>();
        final int id;

        @Override
        public void update(Observable o, Object arg) {
            if(arg instanceof JSONObject) {
                queue.add((JSONObject) arg);
                sem.release();
            }
        }

        JSONObject get() throws InterruptedException{
            while (!Thread.interrupted()) {
                sem.acquire();
                JSONObject rep = queue.poll();
                assert rep!=null;
                if(rep.has("ReqID"))
                    if(rep.getInt("ReqID")==id)
                        return rep;
            }
            throw new InterruptedException();
        }

        MsgWaiter(int id) {
            this.id = id;
        }
    }

    private interface ExeptionRunnable{
        public void run() throws IOException;
    }
}
