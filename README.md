# Selective Repeat with Cumulative ACKs


PROJECT TITLE: Selective Repeat with Cumulative ACKs<br/>
VERSION: 11/8/17<br/>
AUTHOR: Darren Hoffmann-Marks<br/>
ID: U34821624<br/>


###Compilation Instructions:

While having all files of the simulator in the same directory, compile using 
the command line command javac Project.java and run it with the
java Project command. After starting the program, the program will prompt you
for parameters to run the simulation. Simply enter the value for the parameter and press
your return key. When prompted for the retransmission timeout you should enter the value 30;
however, you can run it with whatever timeout interval you’d like.


###Possible Tradeoffs and Extensions:

The way I chose to deal with the wrap around is by incrementing the 
base and end number by the appropriate amount whenever packets were received.
When the base or end number equaled the (WindowSize * 2) I would set the value equal to 0 to wrap it around. 

For dealing with checking if a sequence/ACK number was within a window, I used a function called inWindow(seqNum, base, end). It would first check if the window was in a wrap around state by checking if the base number was greater than the end number. If it was in a wrap around state, then to check if the sequence/ACK number was within the window I would check if seqNum was greater than or equal to the base or less than or equal to the end. If it wasn’t in a wrap around state then I would just check if seqnum was greater than or equal to the base and less than or equal to the end. inWindow() would return 
true if seqNum was within the window


For dealing with receive/send buffers, the buffers size would be equal to WindowSize and packets would be inserted into the buffer according to their sequence number so they would line up with their position in the their
receive/send window. I used a function called determineIndex(seqNum, base, end) to determine a packets position within a buffer. If the window wasn’t in a wrap around state then the packet’s position in the buffer was just seqNum - base. If it was in a wrap around state than I would check if the seqnum was greater than or equal to the base number. If it was then the packet’s position in the buffer was still seqnum - base. If seqnum was less than the base number, then its position in the buffer was (seqNum + (WindowSize * 2) - base).

For dealing with retransmissions. I associated the timer with the last sent new packet. Whenever there was a timer interrupt I would retransmit everything in my retransmit buffer. The timer was stopped whenever A would receive an ACK equal to the last sent sequence number. With this implementation, some bandwidth may be wasted by retransmitting packets that have already been received correctly. One such case is when several packets are sent by A such as packets 2,3, and 4, but packet 2 is lost on its way. B would receive packets 3,4 and send two duplicate ACK 1s. A would retransmit the first unACK’d packet, which is packet 2, upon receiving a duplicate ACK. When B
receives packet 2 it would finally send ACK 4; however in this scenario it is likely that the timer associated with packet 4 would timeout before that ACK 4 arrives, thus leading to a wasted retransmission of packets 2,3,4 and duplicate ACKS 4,4,4.

Overall Design & How it Works:
aOutput() will receive a message from layer5.  If the next sequence number is within the send window it will send the message if there are
no other messages waiting to be sent. Otherwise it will add the message to the end of the buffer of waiting messages. bInput() will be called when B receives a packet from A. It first checks if the packet is corrupt, if it is then it ignores it. Otherwise it checks if the packet sequence number is within the receive window, if it  isn't then it sends a duplicate ACK. If it is in the receive window, it checks if the packet has the next expected sequence number, if it does then it delivers the data along with any subsequent buffered data to layer 5, slides the receive window, and sends an ACK.  If it doesn't have the next expected sequence number it buffers the packet and  sends a duplicate ACK. aInput() is called when A side receives an ACK packet from B side. It checks to see if the packet is corrupt.
If it is then it ignores it, otherwise it checks to see if the ACK number is within the send window. If it is within the send window, then it will slide the send window and update the retransmission buffer.  After sliding the window it checks to see if there are messages waiting to be sent, if there are then it will send them as long as the next sequence number is within the send window. If the ACK number received wasn't within the send window, it sees it as a duplicate ACK and retransmits the first unACK'd packet.

I used a static timer set to 30, because we know that the average time for a packet to travel between links
is 5 units of time
