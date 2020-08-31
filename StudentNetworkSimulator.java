import java.util.*;
import java.io.*;



/**
 * @author - Darren Hoffmann-Marks
 * ID: U34821624
 * Collaborated with Jason Lu
 */
public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
    
    
 
    private int num_delivered;                      // counts number of messages delivered to layer 5                 
    public int num_retrans;                            // counts number of retransmissions
    public double averageRTT;
    public double averageCOM;
    private int originalT;                          // counts number of original transmissions
    private int numAcksB;                               //number of ACKS sent by B
    private double corruptedA;                         // number of corrupted packets A receives
    private double corruptedB;                         // number of corrupted packets B receives
    
    
    private ArrayList<Message> waitingBuffer;             //buffer of transmitted packets
    private List<Double> RTTtimes;                  // records RTT times
    private List<Double> COMtimes;                  // records communication times
    private int seqNumA;                            // this is the old variable
    private int nextSeq;                            // next sequence number to be used
    private int send_base;                          //keeps track of oldest unacknowledged packet
    private int send_end;                           //last available sequence number in window
    private Packet retransBuffer[];                 //holds all packets that may need retransmission
    private int lastSentSeq;                        //keeps track of the last sent sequence number
    private double numPacksSentA;                   //keeps track of how many packets A has sent
    private double sendTime;                        //records the time of sending last packet
    private boolean isRetrans = false;              //flag that ACKS coming are for retransmitted packets
    private double comTimes[];
    private double rttTimes[];
    
    
    
    private int maxSeq;                             // max sequence number 
    private int expSeq;                             // sequence number receiver expects 
    private Packet bufferB[];                       // buffer of recieved packets
    private int rcv_base;                           // base of receive window
    private int rcv_end;                            // end of receive window
    private double numPacksSentB;                   // keeps track of number of packets sent
    
    


    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        WindowSize = winsize;
        LimitSeqNo = winsize*2; // set appropriately; assumes SR here!
        RxmtInterval = delay;
    }

    
    
    
    
    /**
     * This will determine if a sequence number is within the receive or send window
     * 
     * @param seqNo - sequence number to be check if it's in the window
     * @param base - base sequence number of window
     * @param end - end sequence number of window
     * @return - returns true if it's in the window. False otherwise.
     */
    protected boolean inWindow(int seqNo, int base, int end)
    {
        boolean flag = false;
        
        // checks to see if window is in a wrap around state
        if (base > end)
        {
            if (seqNo >= base || seqNo <= end)
                flag = true;
            
        }
        else
        {
            if (seqNo <= end && seqNo >= base)
                flag = true;
            
        }    
        return flag;   
        
    }
    
    
    /**
     * Determines the index of where to insert the packet into
     * sender or receiver buffer
     * 
     * @param seqNo - sequence number of packet to be inserted into buffer
     * @param base - base sequence number of window
     * @param end - end sequence number of window
     * @return - index of buffer to insert packet 
     */
    protected int determineIndex(int seqNo, int base, int end)
    {
        int index = -1;
        //tchecks to see if window is in a wrap around state
        if (end > base)
        {
            index = seqNo - base;
        }
        else
        {
            if(seqNo >= base)
                index = seqNo - base;
            else
                index = seqNo + (LimitSeqNo - base);
        }
        
        
        return index;
    }
    
    
    
    
   
    
    
    
    /**
     * This routine will receive a message from layer 5 to send to the receiver. If
     * the next sequence number is within the send window it will send the message if there are
     * no other messages waiting to be sent. Otherwise it will add the message to the end of the
     * buffer of waiting messages.
     * 
     * @param message - message passed down from Layer 5 to be sent to the receiver
     */
    protected void aOutput(Message message)
    {
        System.out.println("aOutput()_______________________________");
        
        
        System.out.println("Send_Base: " + send_base + " Send_end: " + send_end);
        System.out.println("NextSeqNo: " + nextSeq);
                
        //checks to see if the next sequence number is withing the send window
        boolean in_window = inWindow(nextSeq, send_base, send_end);
        
        //if the next sequence number is in the window
        if(in_window)
        {
            System.out.println("NextSeqNo is in send window");
            //if there aren't messages waiting in the buffer to be sent
            if (waitingBuffer.isEmpty())
            {
                /*
                 * This will extract the data and send the packet into layer3
                 * with the next sequence number
                 * 
                 */
                String msg = message.getData();     //gets payload
                Packet pkt = new Packet(nextSeq,0,0, msg); //creates the packet
                pkt.setChecksum(generateChecksum(pkt));     //creates and inserts checksum 
                toLayer3(0, pkt);     //send the packet into the network
                
                //makes sure that there isn't already a timer running
                if(nextSeq != send_base)
                    stopTimer(0);
                startTimer(0, RxmtInterval);            //starts the timer for last transmitted packet
                sendTime = getTime();                   //records time it was sent
                
                System.out.println("Packet: " + nextSeq + " With Payload: " + msg  + " sent to B");
                System.out.printf("Timer started at: %.2f\n", getTime());
                
                
                //adds the packet to the transmission buffer
                int index = determineIndex(nextSeq, send_base, send_end);
                retransBuffer[index] = pkt;
                comTimes[index] = getTime();            //will track communication times
                rttTimes[index] = getTime();
                
                System.out.println("Packet added to retransmission buffer at index [" + index + "]");
                
                lastSentSeq = nextSeq;          //keeps track of last sent packet's sequence number
                
                
                
                originalT++;
                //numTmitted++;
                numPacksSentA++;
                
                //updates the next sequence number
                nextSeq++;
                if(nextSeq == LimitSeqNo)
                    nextSeq = 0;
                System.out.println("Next sequence number: " + nextSeq);    
                
            }       //if there are messages already waiting to be sent, append message to end of queue
            else {
                waitingBuffer.add(message);
                if(waitingBuffer.size() == 50)
                {
                    System.out.println("Buffer Overflow. System Exit");
                    System.exit(1); 
                }
                System.out.println("Message " + message.getData() + " added to waiting buffer");
                System.out.println("There are " + waitingBuffer.size() + " messages waiting in buffer to be sent");
            }
        }
        else
        {   //if the next sequence number is not within the send window, add it to queue of waiting messages to be sent
            waitingBuffer.add(message);
            if(waitingBuffer.size() == 50){
                System.out.println("Buffer Overflow. System Exit");
                System.exit(1);
            }
            System.out.println("NextSeqNo is not in send window");
            System.out.println("Message " + message.getData() + " added to waiting buffer");
            System.out.println("There are " + waitingBuffer.size() + " messages waiting in buffer to be sent");
            
        }
        
        
        System.out.println("_______________________________\n");     

    }
    
    
    
    
    
    
    /**
     * Generates the checksum for the passed in packet by adding up
     * the value in the sequence number and ack field and by adding up
     * the ascii values of each character in the payload and then taking
     * the ones complement of sum.
     * 
     * @param pkt - packet that function is generating a checksum for
     * @return - checksum of packet to be inserted into checksum field
     */
    public int generateChecksum(Packet pkt)
    {
        int seq = pkt.getSeqnum();
        int ack = pkt.getAcknum();
        String data = pkt.getPayload();
        
        int sum = seq + ack;
        
        for (int i = 0; i < data.length(); i++)
        {
            sum += (int)data.charAt(i);         
        }
        
        
        sum = ~sum;
        
        return sum;
        
        
        
    }
    
    
    
    /**
     * Checks to see if received packet is corrupt by using the checksum and adding
     * up the values in the sequence number, ACK number, and payload field to see if 
     * they add up to a value of 0xFFFFFFFF
     * 
     * @param pkt - packet being checked for corruption
     * @return - returns true if packet is corrup, false otherwise
     */
    public boolean isCorrupt(Packet pkt)
    {
        int seq = pkt.getSeqnum();      
        int ack = pkt.getAcknum();
        String data = pkt.getPayload();
        
        int sum = seq + ack;        //adds sequence number and ack number of packet
        
        // adds up the ascii values of each character in the payload
        for (int i = 0; i < data.length(); i++)
        {
            sum += (int)data.charAt(i);         
        }
              
        int check = sum + pkt.getChecksum();
        
        if (check == 0xFFFFFFFF)
            return false;
        else
            return true; 
    }
    
    
    
    
    
    
    
    /**
     * Called when A side receives an ACK packet from B side. Checks to see if the packet is corrupt.
     * If it is then it ignores it, otherwise it checks to see if the ACK number is within the send window.
     * If it is within the send window, then it will slide the send window and update the retransmission buffer. 
     * After sliding the window it checks to see if there are messages waiting to be sent, if there are then it will
     * send them as long as the next sequence number is within the send window. If the ACK number received wasn't within
     * the send window, it sees it as a duplicate ACK and retransmits the first unACK'd packet.
     * 
     * @param packet - ACK packet received from B
     */
    protected void aInput(Packet packet)
    {
        
        
        System.out.println("aInput()_______________________________");
        int ack = packet.getAcknum();           
        
        System.out.println("Current Send_Base: " + send_base + "   Send_end: " + send_end);
        // if the ACK received is not corrupt
        if (!isCorrupt(packet))
        {
            //gets the index of the last packet to be removed from  
            int index = determineIndex(ack, send_base, send_end);
            boolean in_window = inWindow(ack, send_base, send_end);
            
            
            
            //if the ACK received is in the window
            if(in_window)
            {
                //checks to see if ACK received is expected or cumulative
                if ((index + 1) == 1)
                    System.out.println("Expected ACK " + ack + " received");
                else 
                    System.out.println("Cumulative ACK " + ack + " received");
                    
                
                //stops the timer if the ACK is equal to the last sent packet
                if(ack == lastSentSeq){
                    stopTimer(0);
                    double insert = getTime()-sendTime;
                    if (isRetrans){
                        isRetrans = false;
                    }
                    else{
                       RTTtimes.add(insert);
                    }
                    System.out.println("Recieved ACK for last sent packet. Stopping Timer at " + getTime());
                }
                
                //updates the retransBuffer
                int k = 0;
                for (int x = (index + 1); x < WindowSize; x++)
                {
                    retransBuffer[k] = retransBuffer[x];
                    retransBuffer[x] = null;   
                    k++;
                }
                
                getComTimes((index+1));
                
                
                System.out.println("Sliding window " + (index + 1) + " times");
                //updates the sliding window
                for (int i = 0; i < (index + 1); i++)
                {
                    send_base++;
                    if (send_base == LimitSeqNo)
                        send_base = 0;
                    send_end++;
                    if (send_end == LimitSeqNo)
                        send_end = 0;    
                }
                System.out.println("New send_base: " + send_base + " send_end: " + send_end);
                
                
                //If there are messages waiting in the buffer to be sent
                System.out.println("Number of messages waiting in buffer after ACK: " + waitingBuffer.size());
                
                //while the next sequence number is within the send window, send the next message
                while(inWindow(nextSeq, send_base, send_end))
                {
                    /*
                    * This block of code will get the next message, create the packet,
                    * and send it into layer 3
                    */
                    if(!waitingBuffer.isEmpty()){
                        Message message = waitingBuffer.remove(0);
                        String msg = message.getData();     //gets payload
                        Packet pkt = new Packet(nextSeq,0,0, msg); //creates the packet
                        pkt.setChecksum(generateChecksum(pkt));
                        toLayer3(0, pkt);     //send the packet into the network
                        startTimer(0, RxmtInterval);            //starts the timer for last transmitted packet
                        sendTime = getTime();
                        
                        //adds the packet to the transmission buffer
                        int buffer_index = determineIndex(nextSeq, send_base, send_end);
                        retransBuffer[buffer_index] = pkt;
                        comTimes[buffer_index] = getTime();
                        rttTimes[buffer_index] = getTime();
                        lastSentSeq = nextSeq;          //keeps track of last sent packet's sequence number
                    
                       
                        System.out.println("Message " + msg + " Sent to B with seqNo " + nextSeq);
                        System.out.println("Packet added to retransmission buffer at index [" + buffer_index + "]");
                        System.out.printf("Timer started at: %.2f\n", getTime());
                
                        numPacksSentA++;
                        originalT++;
                        
                
                        //updates the next sequence number
                        nextSeq++;
                        if(nextSeq == LimitSeqNo)
                            nextSeq = 0;
                        System.out.println("Next sequence number: " + nextSeq);
                        System.out.println("Number of messages waiting in buffer: " + waitingBuffer.size());
                    }
                    else
                        break;
                    
                       //System.out.println("Number of messages waiting in buffer: " + waitingBuffer.size());
                    
                    }
                    
                    
                    
                
                
                
            }
            else        // if ACK number is not within send window
            {
                System.out.println("Received duplicate ACK " + ack);
                System.out.println("Send_Base: " + send_base + " Send_End: " + send_end);
                
                // retransmits the first packet in the retrans buffer
                if (retransBuffer[0] != null)
                {
                    Packet pkt = retransBuffer[0];
                    toLayer3(0, pkt);
                    
                    isRetrans = true;
                    System.out.println("Retransmitted packet: " + pkt.getSeqnum() + " Payload: " + pkt.getPayload());
                    num_retrans++;
                    numPacksSentA++;
                }
                else
                {
                    System.out.println("Retransmission Buffer is empty. Ignore Duplicate ACK");
                }
                
                
            }
             
        }
        else    //Packet received is corrupt
        {
            System.out.println("Received Corrupted ACK");
            corruptedA++;
            System.out.println("Number of Corruped Packets to A: " + corruptedA);
            
            
            
        }
        
        System.out.println("_______________________________\n");
        
        
    }
    
    /**
     * This routine will be called when there is a timeout. The way I've chosen to handle retransmissions
     * is by using a timer with the last transmitted packet. When there is a timeout this function
     * will retransmit every packet in the retransmission buffer
     */
    protected void aTimerInterrupt()
    {
        System.out.println("aTimerInterrup()_______________________________");
        
        System.out.printf("TIMEOUT!!! at: %.2f\n", getTime());
        //stopTimer(0);
        
        System.out.println("Retransmitting every packet since last ACK'd");
        // retransmit everything in retrans_buffer
        for (int i = 0; i < WindowSize; i++)
        {
            if(retransBuffer[i] != null)
            {
                Packet pkt = retransBuffer[i];
                toLayer3(0, pkt);
                int seqNum = pkt.getSeqnum();
                System.out.println("Retransmitting Packet " + seqNum);
                num_retrans++;
                numPacksSentA++;
                
            }     
            
        }
        isRetrans = true;
        startTimer(0, RxmtInterval);
        System.out.println("Starting timer for packet " + lastSentSeq + " at time " + getTime());
        


        System.out.println("_______________________________\n");
    }
    
    /**
     * This routine acts as constructor for the A side sender
     */
    protected void aInit()
    {
       
       
        corruptedA = 0;                         //counts number of corrupted ACKs
        num_retrans = 0;                            // counts number of retransmissions
        originalT = 0;                          // counts number of original transmissions
        comTimes = new double[WindowSize];
        rttTimes = new double[WindowSize];
        
        seqNumA = 0;
        COMtimes = new ArrayList<Double>();
        RTTtimes = new ArrayList<Double>();
        waitingBuffer = new ArrayList<Message>();
        send_base = 0;
        nextSeq = 0;
        send_end = WindowSize -1;
        retransBuffer = new Packet[WindowSize];
        numPacksSentA = 0;
        

    }
    
    
    
    /**
     * This routine will be called when A_Input() receives and ACK and record 
     * the communication times for the packets that have just been ACK'd
     * 
     * @param slide - the number of packets that have just been ACK's
     */
    protected void getComTimes(int slide)
    {
        double time = getTime();        //gets current time
        double com;
        //loops through all send times in the buffer for packets that have been ACK'd
        for(int i = 0; i < slide; i++)
        {
            com = time - comTimes[i];
            COMtimes.add(com);
            
        }
        
        //updates the buffer holding send times
        int k = 0;
        for (int x = slide; x < WindowSize; x++)
        {
            comTimes[k] = comTimes[x];
            comTimes[x] = -1;   
            k++;
        }
        
        
        
    }
    
    
    
    
    
    /*******************************
     * This will deliver the packets from the buffer
     * in the receiver to layer5
     *******************************/
    protected void deliverBuffer()
    {
        int slide = 0;
        int i = 0;
        while (i < WindowSize)
        {
            //checks to see if the first spot in the buffer 
            if(bufferB[i] != null)
            {
                if (bufferB[i].getSeqnum() == rcv_base)
                {
                    //send data to layer 5
                    String data = bufferB[i].getPayload();
                    System.out.println("Packet " + bufferB[i].getSeqnum() + " Message " + data + " delivered to Layer 5");
                    toLayer5(data);
                
                    bufferB[i] = null;
                
                    num_delivered++;
                
                    //updating the sliding window 
                    rcv_base++;
                    if (rcv_base == LimitSeqNo)
                        rcv_base = 0;
                    rcv_end++;
                    if (rcv_end == LimitSeqNo)
                        rcv_end = 0;
                    slide++;
                
                }   
                        
            }
            i++;
        }
        
       
        //repositions data in the buffer
        int k = 0;
        for (int x = slide; x < WindowSize; x++)
        {
            bufferB[k] = bufferB[x];
            bufferB[x] = null;   
            k++;
        }
                  
        System.out.println("New rcv_base = " + rcv_base + "     rcv_end = " + rcv_end);
        
    }
    
    
    
    
    /**
     * This function will be called when B receives a packet from A. It first
     * checks if the packet is corrupt, if it is then it ignores it. Otherwise
     * it checks if the packet sequence number is within the receive window, if it 
     * isn't then it sends a duplicate ACK. If it is in the receive window, it checks
     * if the packet has the next expected sequence number, if it does then it delivers the
     * data along with any subsequent buffered data to layer 5, slide the receive window 
     * and sends an ACK. If it doesn't have the next expected sequence number it buffers 
     * the packet and sends a duplicate ACK
     * 
     * @param packet - packet received from A
     */
    protected void bInput(Packet packet)
    {
        
        System.out.println("bInput() _______________________________");
        
        
        System.out.println("Packet " + packet.getSeqnum() + " received. rcv_base: " + rcv_base
                            + "      rcv_end: " + rcv_end);
        
        
        if (!isCorrupt(packet))
        {
            // if the packet received is within the receive window
            int pktseqnum = packet.getSeqnum();
            System.out.println("Packet received is not corrupt. At time " + getTime());
            
            boolean in_window = inWindow(pktseqnum, rcv_base, rcv_end);
            if(in_window)                                  
            {      
                //if the packet received has the same sequence number as the rcv_base
                //deliver subsequent packets to layer5 and slide the window
                //send a cumulative ACK for all the packets delivered
                int index = determineIndex(pktseqnum, rcv_base, rcv_end);
                if (pktseqnum == rcv_base)
                {
                    System.out.println("Packet received is in order. Delivering packet(s) to layer5");
                    //adds the packet received
                    bufferB[index] = packet;
                    deliverBuffer();            //this will also update the rcv_base
                    
                    int acknum = rcv_base -1 ;
                    if(acknum < 0)
                        acknum = maxSeq;
                    
                    
                    
                    Packet ACK = new Packet(0, acknum,0);
                    ACK.setChecksum(generateChecksum(ACK));
                    toLayer3(1, ACK);
                    if (acknum != pktseqnum)
                        System.out.println("Cumulative ACK " + ACK.getAcknum() + " Sent");
                    else
                        System.out.println("In order ACK " + ACK.getAcknum() + " Sent");
                    numAcksB++;
                    numPacksSentB++;
                    
                    
                } 
                else        
                {
                    
                    //if the packet is received correctly and in the receive window
                    //send a duplicate ACK for the last correctly received packet
                    //and add that packet to the buffer  
                    bufferB[index] = packet;
                    
                    System.out.println("Packet received is in window, but out of order. Stored in buffer[" + index + "]");
                    int acknum = rcv_base -1 ;
                    if(acknum < 0)
                        acknum = maxSeq;
                    
                    Packet ACK = new Packet(0, acknum,0);
                    ACK.setChecksum(generateChecksum(ACK));
                    toLayer3(1, ACK);
                
                    System.out.println("Duplicate ACK " + ACK.getAcknum() + " Sent");
                    numAcksB++;
                    numPacksSentB++;
                    
                    
                }    
                
            }
            else 
            {
                // if the packet received seqNum is outside of the receive window
                // send a cumulative ACK
                int acknum = rcv_base -1 ;
                if(acknum < 0)
                    acknum = maxSeq;
                
                
                Packet ACK = new Packet(0, acknum,0);
                ACK.setChecksum(generateChecksum(ACK));
                toLayer3(1, ACK);
                
                System.out.println("Packet received is outside of window. ACK" + ACK.getAcknum() + " Sent");
                numAcksB++;
                numPacksSentB++;
                
                
            }   
            
        }
        else
        {
            corruptedB++;
        
            System.out.println("Packet received is corrupted. Discarding and ignoring packet");
            
            
        }
     
        
        System.out.println("_______________________________\n");
        

    }
    
    /**
     * Acts as a constructor for the B side variables
     */
    protected void bInit()
    {
        numPacksSentB = 0;
        numAcksB = 0;
        corruptedB = 0;
        num_delivered = 0; 
        expSeq = 0;
        bufferB = new Packet[WindowSize];
        rcv_base = 0;
        rcv_end = rcv_base + WindowSize - 1;
        maxSeq = (2* WindowSize) -1;

    }

    
  
    /**
     * Returns the average of the items in an ArrayList
     * 
     * @param arrayL - the ArrayList whose average is being returned
     * @return - the average of the ArrayList
     */
    protected double getAverage(List<Double> arrayL)
    {
        double sum = 0;
        int size = arrayL.size();
        for (int i = 0; i < size; i++)
        {
            sum += arrayL.get(i).doubleValue();
            
        }
        
        double average = (sum/(double)size);
        
        return average;
    }
    
    /**
     * Prints out statistics of simulation
     */
    protected void Simulation_done()
    {
        // TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
        System.out.println("\n\n===============STATISTICS=======================");
        System.out.println("Number of original packets transmitted by A: " + originalT);
        System.out.println("Number of retransmissions by A: " + num_retrans);
        System.out.println("Number of data packets delivered to layer 5 at B: " + num_delivered);
        System.out.println("Number of ACK packets sent by B: " + numAcksB);
        System.out.println("Number of corrupted packets: " + (corruptedA + corruptedB));
        
        double ratioLost = (num_retrans - (corruptedA + corruptedB))/(((originalT + num_retrans) + numAcksB));
        System.out.println("Ratio of lost packets: " + ratioLost);
        double ratioCorrupted = ((corruptedA + corruptedB)/((originalT + num_retrans) + numAcksB - (num_retrans - (corruptedA + corruptedB))));
        System.out.println("Ratio of corrupted packets: " + ratioCorrupted);
        averageRTT = getAverage(RTTtimes);
        System.out.println("Average RTT: " + averageRTT);
        averageCOM = getAverage(COMtimes);
        System.out.println("Average communication time: " + averageCOM);
        System.out.println("==================================================");
        
        

        // PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
        System.out.println("\nEXTRA:");
        // EXAMPLE GIVEN BELOW
        //System.out.println("Example statistic you want to check e.g. number of ACK packets received by A :" + "<YourVariableHere>"); 
    }   

}
