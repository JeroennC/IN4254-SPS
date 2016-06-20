import socket
import sys
import struct

# Socket protocol
def send_message(sock, data):
  l = len(data)
  sock.sendall(struct.pack('!I', l))
  sock.sendall(data)
  
def recv_message(sock):
  lbuf = recv_all(sock, 4)
  if not lbuf:
    return lbuf
  l, = struct.unpack('!I', lbuf)
  return recv_all(sock, l)
  
def recv_all(sock, count):
  buf = b''
  while count:
    newbuf = sock.recv(count)
    if not newbuf: return None
    buf += newbuf
    count -= len(newbuf)
  return buf

# Create a TCP/IP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Bind the socket to the port
server_address = ('0.0.0.0', 10000)
print >>sys.stderr, 'starting up on %s port %s' % server_address
sock.bind(server_address)

# Listen for incoming connections
sock.listen(1)

while True:
    # Wait for a connection
    print >>sys.stderr, 'waiting for a connection'
    connection, client_address = sock.accept()
	
    try:
        print >>sys.stderr, 'connection from', client_address

        # Receive the data in small chunks and retransmit it
        while True:
            data = recv_message(connection)
            
            if not data:
              break
              
            print >>sys.stderr, 'received "%s"' % data
            
            send_message(connection, data)
            
            
    finally:
        # Clean up the connection
        connection.close()