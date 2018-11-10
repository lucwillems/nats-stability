# nats-stability
test coding to test nats implementation stability under high sender/receiver load

setup :
- we have 1 thread sending highspeed messages to a localhost running nats server.
- we have 1 or more receiver threads to collect the messages from the sender.

in general , receiving messager speed is lower than sender speed, so when sender can reach
1M msg/sec on my i7 system , receiver can not handle this.

this cause the slow connection , and after some time the receiver is disconnected from the nats
server. https://nats.io/documentation/server/gnatsd-slow-consumers/

Reciever thread should reconnect, at as such this will happen.

the problem :
-------------

receiver process can be  a long running server, which expect that the nats connection is "self" healing, meaning
reconnect and collect messages again.

under high load , the receiver thread gets , after some time , into a Connection timeout and can not get a working conneciton anymore.
causing the server to be not getting messages.



