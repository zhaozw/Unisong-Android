package com.ezturner.speakersync.network;

/**
 * Created by ezturner on 3/2/2015.
 */
public class CONSTANTS {


    //The starting port for the streaming sessions
    public static final int STREAM_PORT_BASE = 55989;

    //The number that will be used to add to the base port to randomize the port that the stream will be on
    public static final int PORT_RANGE = 50;

    //The port for active discovery
    public static final int DISCOVERY_SLAVE_PORT = 55988;

    //The port for active discovery
    public static final int DISCOVERY_MASTER_PORT = 55987;

    public static final int RELIABILITY_PORT = 55990;

    //The packet ID for starting a new song
    public static final byte SONG_START_PACKET_ID = 1;

    //The packet ID for a master discovery packet
    public static final byte MASTER_START_PACKET = 2;

    //The packet ID for a response packet to a discovery request
    public static final byte MASTER_RESPONSE_PACKET = 3;

    //The packet ID for streaming data packets
    public static final byte FRAME_INFO_PACKET_ID= 4;

    //The packet ID for streaming data packets
    public static final byte FRAME_DATA_PACKET_ID= 5;

    public static final byte FRAME_PACKET_ID = 6;

    public static final byte MIME_PACKET_ID = 7;

    public static final byte AUDIO_DATA_PACKET_ID = 8;

    public static final int AUDIO_CHUNK_SIZE = 1024;

    public static final int MAX_PACKET_SIZE = 1030;

    public static final int DELAY = 500;

    public static final byte TCP_ACK_ID = 1;

    public static final byte TCP_REQUEST_ID = 2;

    public static final byte TCP_COMMAND_RETRANSMIT = 3;

    public static final byte TCP_SONG_START = 4;

    public static final byte TCP_SONG_IN_PROGRESS = 5;


}
