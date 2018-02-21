package com.android.internal.telephony.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;

import com.android.internal.telephony.utils.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

//Todo (1) register broadcast receiver to receive commands from other app parts - done
//Todo (2) Initiate call server or call instance as required
//Todo (3) At end of call send report to server

public class CallService extends Service {

    private boolean mRunCallingServer = false;
    private boolean mRunCallingInstance = false;
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int minBufSize = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
    private static String mTag;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();

        mTag = this.getClass().getSimpleName();

        Log.i(mTag, "CAll Service Created");
        IntentFilter callServiceIntentFilter = new IntentFilter();
        callServiceIntentFilter.addAction(Constants.Calling.CALL_SERVICE_ACTION);
        registerReceiver(mCallServiceReceiver, callServiceIntentFilter);
    }

    private void startCallServer() {
        Thread thread = new Thread(() -> {
            try {
                Log.i(mTag, "Call Server started");
                //Servers to receive from client
                DatagramSocket serverSocket = new DatagramSocket(Constants.Calling.CALLING_SERVER_PORT);
                serverSocket.setReuseAddress(true);
                //buffer to receive from microphone
                byte[] buffer = new byte[minBufSize];
                //Instance for microphone
                //AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,Constants.Calling.SAMPLING_RATE,channelConfig,audioFormat,minBufSize*10);

                AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, Constants.Calling.SAMPLING_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        AudioRecord.getMinBufferSize(Constants.Calling.SAMPLING_RATE,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10);
                //Let microphone start recording sound
                recorder.startRecording();
                //Instance for speaker
                //AudioTrack atrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, Constants.Calling.SAMPLING_RATE, channelConfig, audioFormat, minBufSize, AudioTrack.MODE_STREAM);

                AudioTrack atrack = new AudioTrack(AudioManager.STREAM_MUSIC, Constants.Calling.SAMPLING_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, minBufSize, AudioTrack.MODE_STREAM);

                //set speaker sapling rate
                atrack.setPlaybackRate(Constants.Calling.SAMPLING_RATE);
                //start speaker
                atrack.play();
                //While call is running
                while (mRunCallingServer) {
                    //buffer to hold incoming sampled sound
                    byte[] receiveData = new byte[minBufSize];
                    //new instance for datagram recived packet
                    DatagramPacket receivePacket = new DatagramPacket(receiveData,
                            receiveData.length);
                    //receive data from server socket and hold it in receivePacket
                    serverSocket.receive(receivePacket);
                    Log.i(mTag, "Call server recived packet its size is: " + receivePacket.getData().length);
                    //write recived data to speaker
                    atrack.write(receivePacket.getData(), 0, receivePacket.getLength());

                    recorder.read(buffer, 0, buffer.length);
                    //putting buffer in the packet
                    DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, receivePacket.getAddress(), receivePacket.getPort());
                    //Send voice packet to destination
                    serverSocket.send(sendPacket);
                }
                serverSocket.disconnect();
                serverSocket.close();
                recorder.stop();
                recorder.release();
                atrack.stop();
                atrack.flush();
                atrack.release();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    private void startCallInstance(String serverIP) {
        Thread thread = new Thread(() -> {
            try {

                //Address of destination call server
                final InetAddress destination = InetAddress.getByName(serverIP);
                //Servers to receive from client
                DatagramSocket socket = new DatagramSocket();
                //Instance for microphone
                //AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,Constants.Calling.SAMPLING_RATE,channelConfig,audioFormat,minBufSize*10);

                AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, Constants.Calling.SAMPLING_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        AudioRecord.getMinBufferSize(Constants.Calling.SAMPLING_RATE,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10);

                //Let microphone start recording sound
                recorder.startRecording();
                //Instance for speaker
                //AudioTrack atrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, Constants.Calling.SAMPLING_RATE, channelConfig, audioFormat, minBufSize, AudioTrack.MODE_STREAM);

                AudioTrack atrack = new AudioTrack(AudioManager.STREAM_MUSIC, Constants.Calling.SAMPLING_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, minBufSize, AudioTrack.MODE_STREAM);

                //set speaker sapling rate
                atrack.setPlaybackRate(Constants.Calling.SAMPLING_RATE);
                //start speaker
                atrack.play();
                //While call is running
                while (mRunCallingInstance) {
                    //buffer to receive from microphone
                    byte[] buffer = new byte[minBufSize];
                    recorder.read(buffer, 0, buffer.length);
                    //putting buffer in the packet
                    DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, destination, Constants.Calling.CALLING_SERVER_PORT);
                    //Send voice packet to destination
                    socket.send(sendPacket);
                    Log.i(Constants.TAG, "call instance sent data its length is: " + sendPacket.getData().length);
                    //buffer to hold incoming sampled sound
                    byte[] receiveData = new byte[minBufSize];
                    //new instance for datagram recived packet
                    DatagramPacket receivePacket = new DatagramPacket(receiveData,
                            receiveData.length);
                    //receive data from server socket and hold it in receivePacket
                    socket.receive(receivePacket);
                    //write recived data to speaker
                    atrack.write(receivePacket.getData(), 0, receivePacket.getLength());
                }
                socket.disconnect();
                socket.close();
                recorder.stop();
                recorder.release();
                atrack.stop();
                atrack.flush();
                atrack.release();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private BroadcastReceiver mCallServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Todo Implement open call server action
            String ip = intent.getStringExtra(Constants.Calling.MAKE_CALL_ACTION_PARAM);
            Log.i(mTag, String.format("broadcast recieved and ip is: %s", ip));
            if (intent.getStringExtra("END") != null && intent.getStringExtra("END").equals("END")) {
                mRunCallingInstance = false;
                mRunCallingServer = false;
                mDisposable.dispose();
                return;
            }
            if (ip == null) {
                mRunCallingServer = true;
                mRunCallingInstance = true;
                startCallServer();
            } else {
                mRunCallingInstance = true;
                startCallInstance(ip);
            }
            //Todo Implement open call instance action
        }
    };

}

