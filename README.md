# nats stability
test code to test nats JAVA client implementation stability under high sender/receiver load and see that it can recover from this.

## setup :
- running local nats in docker , version nats:1.3.0-linux
- running java test application which launches 1 sender 
- small message "this is a message" 
- restart the nats server and observe that sometimes the sender will not recover but get
  a Out of Memory exception.
  

## branches

- main : basic example application of the problem with no workaround.
- sender-oom : example how to get Out of Memory exception when using very highspeed sender and
               restarting nats server , or disconnect , which forces the reconnect to handle reconnect
               and sender thread is still sending on high speed.
               

# Current workaround / fix

the root cause is that buffer checks are not thread save causing in some cases
the internal MessageQueue to be filled unbounded while the "reconnect" thread tries
to connect again but takes to long time.

the workaround is to used bounded Queues in MessageQueue so the number of outstanding
items can be limited between the publish() thread and the actual IO thread.
if the publisher thread overloads the queue, it will be blocked, while the IO threads
can get IO back running. 

Blocking the "publisher" thread is not "forever" because reconnect will flush the IO queue
but signals the users that the connection is not active.

as a side effect i noticed that the outgoing flow is more "stable" , because sender
gets blocked for a few nanoseconds , each time the queues are full , which allows
the IO thread to spend some more time to handle the output

on my system , having unbounded IO queues gives around 3M msg/sec 
when using the BlockedQueue , i get 5 M msg/sec using a value of 2048 for io queue size.

the fixed can be found in : 
git : https://github.com/lucwillems/java-nats.git
branch : fix-oom-sender
 


