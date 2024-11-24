# protohackers
My solutions to [Protohackers](https://protohackers.com/)

### testing with netcat

`netcat` was designed to work until both sides have closed the connection.
You still have one side left open.
When the server closes the connection with FIN the connection remains in a half-open state
where you can send data and the server can receive it.
The connection will remain in this state until you close netcat's stdin (type Ctrl-D),
which makes it send a FIN packet to the server.

`nc [host] [port]`
1) Type text, send with enter
2) Use `ctrl + D` to send FIN and close the connection
