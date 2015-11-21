package com.iha.group2.dronecontrol;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/*REFERENCE:
http://developer.android.com/training/basics/network-ops/connecting.html
http://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
http://developer.android.com/reference/java/net/DatagramSocket.html
 */

/*This class extends a Service
It sends a message to Arduino (or UDP server) and then it receives an answer from it
 */

public class UDP_Receiver extends Service {

    // Some initializations
    static final int UDP_port = 8888;
    static final int timeout = 10000;

    Drone drone;
    String ip;
    DatagramSocket socket;
    Thread t;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Start function from service
    public int onStartCommand(final Intent intent, int flags, int startId) {
        // Get drone instance
        drone = Drone.getInstance();
        // Get IP from drone class
        ip = drone.getIP();
        //Log.v("UDP_receiver", ip);
        // Get action
        final String action = intent.getStringExtra("action");
        Log.v("actionUDP", ""+action);
        // Do something depending on the action.
        switch (action) {
            // Handshake message from InitActivity
            case "connect":
                try {
                    get_msg(ip, action, UDP_port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            // Check internet connection
            case "Check":
                // Returns false if internet is not available
                if (!isNetworkAvailable()){
                    broadcast_toInit("NoConnection", 0);
                    broadcast_result("NoConnection", 2);
                }
                else{
                    broadcast_result("Connection", 3);
                    broadcast_toInit("Connection", 0);
                }
                break;
            // GPS data request from MapsActivity
            case "GPS":
                try {
                    get_msg(ip, action, UDP_port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            // Stop message from MapsActivity
            case "Stop":
                try {
                    get_msg(ip, action, UDP_port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket.close();
                drone.setStatus(false);
                stopSelf();
            // Weather data request from MapsActivity
            case "Weather":
                try {
                    get_msg(ip, action, UDP_port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            // Default response
            default:
                Log.v("Service Receiver:", "Unknown Action " + action);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        socket.close();
        stopSelf();
    }



    //It sends data and processes the received message
    public void get_msg (final String ip, final String msg, final int port) throws IOException{
        /*
        Variables declaration
         */
        // Received message will be written here

        // Get message length
        int msg_length = msg.length();
        // Get bytes from message
        byte[] message = msg.getBytes();
        // Get IP address
        InetAddress IPAddress = InetAddress.getByName(ip);

        // Create new socket
        socket = new DatagramSocket();

        Log.v("Service Receiver:", "Sending connection packet");

        // Sending msg to server, the msg will tell which data do we want
        DatagramPacket p = new DatagramPacket(message, msg_length,IPAddress, port);
        socket.send(p);
        Log.v("Service Receiver:","Connection data sent");

        Log.v("Service Receiver:", "Receiving packet");
        // Preparing packet to receive data


        // Timeout
        socket.setSoTimeout(timeout);

        // We wait until we receive a packet or timeout happens
        /*if (t != null) try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        t = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] receive_data = new byte[64];
                    DatagramPacket receive_pkt = new DatagramPacket(receive_data,receive_data.length);
                    String rec_msg;
                    Log.v("Service Receiver:", "Waiting for data");
                    try {
                        socket.receive(receive_pkt);
                        Log.v("Service Receiver:", "Data received");
                    }
                    // In case of timeout
                    catch (IOException e) {
                        Log.v("Service Receiver:", "Timeout");
                        // Set status to not connected
                        drone.setStatus(false);
                        // Close connection
                        socket.close();
                        if (msg.equals("connect")) broadcast_toInit("error",0);
                    }
                    rec_msg = new String(receive_pkt.getData());
                    Log.v("Service Receiver", "Data received: " + rec_msg.split("\n")[0]);
                    //this variable splits the messages received by \n because the buffer can contain others undesired characters
                    String ms = rec_msg.split("\n")[0];
                    Log.v("MESSAGEService", ""+ms);
                    String act = ms.split("!")[0];
                    Log.v("act = ",act);
                    // Check which message we've received and react accordingly
                    switch (act) {
                        // If the message is stop, we stop the service and send a message.
                        case "Stop":
                            broadcast_result(act, 1);
//                            Toast.makeText(getApplicationContext(),"Stop",Toast.LENGTH_LONG).show();
                            break;
                        // Hand Shake message
                        case "alive":
                            broadcast_toInit(act, 0);
                            break;
                        case "Weather":
                            String weather_value = ms.split("!")[1];
                            broadcast_result(weather_value, 4);
                            break;
                        case "GPS":
                            String GPS_value = ms.split("!")[1];
                            Log.v("GPS result: ", GPS_value);
                        broadcast_result(GPS_value, 0);
                        break;
                    default:
                        broadcast_result(rec_msg, 5);
                }
            }
        });
        t.start();
        Log.v("Client:", "Out of loop");
    }

    // Broadcast the result to MapsActivity
    public void broadcast_result(String msg,int act){
        Intent broadcast = new Intent();
        broadcast.setAction("broadcast");
        broadcast.putExtra("action",act);
        broadcast.putExtra("result", msg);
        sendBroadcast(broadcast);
    }

    // Broadcast the result to InitActivity
    public void broadcast_toInit(String msg,int act){
        Intent broadcast = new Intent();
        broadcast.setAction("init");
        broadcast.putExtra("action",act);
        broadcast.putExtra("result", msg);
        sendBroadcast(broadcast);
    }

    // Check network availability
    public boolean isNetworkAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }



    // Old TCP client. Deprecated
    /*public String tcp_client(String ip, String msg, int port) throws  IOException{
        InetAddress IP = InetAddress.getByName(ip);
        if (first) {
            Log.v("TCP_connection:", ""+first);
            socket_tcp = new Socket(IP, port);
            first=false;
        }
        out = socket_tcp.getOutputStream();
        socket_tcp.setSoTimeout(10000);
        output = new PrintWriter(out);
        BufferedReader in;
        String response;

        output.println(msg);
        output.flush();
        Log.v("TCP_connection:", "Sent msg");
        in = new BufferedReader(new InputStreamReader(socket_tcp.getInputStream()));

        response = in.readLine();
        Log.v("TCP_connection:", "Message received");
        Log.v("TCP_connection:", response);

        if (response.equals("Stop")){
            broadcast_toInit(response,0);
        }
        else if (response.equals("alive")){
            broadcast_toInit(response, 0);
        }

        return response;
    }*/
}
