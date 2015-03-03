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
    public static final int DISCOVERY_PORT = 55988;

    //The port for packet discovery
    public static final int DISCOVERY_PASSIVE_PORT = 55987;

    public static final int RELIABILITY_PORT = 55989;

    //The packet ID for streaming data packets
    public static final byte FRAME_INFO_PACKET_ID= 0;

    //The packet ID for starting a new song
    public static final byte SONG_START_PACKET_ID = 1;

    //The packet ID for a master discovery packet
    public static final byte MASTER_START_PACKET = 2;

    //The packet ID for a response packet to a discovery request
    public static final byte MASTER_RESPONSE_PACKET = 3;

    //The packet ID for streaming data packets
    public static final byte FRAME_DATA_PACKET_ID= 4;

}
