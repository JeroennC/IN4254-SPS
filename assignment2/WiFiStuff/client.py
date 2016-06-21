import socket
import sys
import json
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


f = open('wifimeasurements.dat', 'r');

encoded = f.read();

# Store list of measurements
measurements = json.loads(encoded)['measurements'];



# Create a TCP/IP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Connect the socket to the port where the server is listening
# 
server_address = ('52.58.85.127', 10000)
print >>sys.stderr, 'connecting to %s port %s' % server_address
sock.connect(server_address)

try:
  # Reset server
  reset = {'type': 'reset'}
  send_message(sock, json.dumps(reset))
  data = recv_message(sock)
  print >>sys.stderr, 'received "%s"' % data
    
  for i in xrange(0, len(measurements)):
    measurement = measurements[i]
    measurement['cell'] = [measurement['cell']]
    measurement['type'] = 'store'
    
    send_message(sock, json.dumps(measurement))
    
    data = recv_message(sock)
    
    print >>sys.stderr, 'received "%s"' % data
    
  print len(measurements)
  # Test some measurements
  for i in [5,10,15,20,25,30,35,40,45,50,55]:
    measurement = measurements[i]
    measurement['cell'] = [measurement['cell']]
    measurement['type'] = 'predict'
    
    send_message(sock, json.dumps(measurement))
    
    data = recv_message(sock)
    
    print >>sys.stderr, 'received "%s"' % data
    print measurement['cell']
    
    '''
    # measurement = '{ "type":"store", "cell": [1,2], "data": [{"bssid": "5c:96:9d:65:76:8e", "strength": -78},{"bssid": "5c:96:9d:65:76:8d", "strength": -74},{"bssid": "80:ea:96:eb:1e:fc", "strength": -58},{"bssid": "80:ea:96:eb:1e:fd", "strength": -65},{"bssid": "e0:3f:49:09:d9:98", "strength": -67},{"bssid": "e0:3f:49:09:d9:9c", "strength": -77},{"bssid": "1c:aa:07:6e:31:af", "strength": -56},{"bssid": "00:22:f7:21:d0:38", "strength": -76},{"bssid": "1c:aa:07:6e:31:ad", "strength": -57},{"bssid": "1c:aa:07:b0:7a:bd", "strength": -74},{"bssid": "1c:aa:07:b0:7c:0f", "strength": -86},{"bssid": "1c:aa:07:7b:39:1f", "strength": -68},{"bssid": "64:d1:a3:3b:6f:72", "strength": -71},{"bssid": "1c:aa:07:6e:31:ae", "strength": -57},{"bssid": "1c:aa:07:7b:39:1d", "strength": -67},{"bssid": "1c:aa:07:7b:39:1e", "strength": -68},{"bssid": "1c:aa:07:b0:7a:bf", "strength": -74},{"bssid": "1c:aa:07:b0:80:cd", "strength": -90},{"bssid": "94:10:3e:98:19:6f", "strength": -89},{"bssid": "1c:aa:07:b0:7a:be", "strength": -75},{"bssid": "1c:aa:07:7b:37:0f", "strength": -88},{"bssid": "e8:94:f6:5d:77:16", "strength": -90},{"bssid": "1c:aa:07:b0:7a:b1", "strength": -68},{"bssid": "1c:aa:07:6e:31:a0", "strength": -67},{"bssid": "24:01:c7:76:2a:30", "strength": -77},{"bssid": "1c:aa:07:b0:80:c0", "strength": -83},{"bssid": "1c:aa:07:b0:80:cf", "strength": -91} ]}'
    # Send data
    sock.sendall(measurement)

    # Look for the response
    data = sock.recv(1024)
    
    print >>sys.stderr, 'received "%s"' % data
    
    
    #measurement = '{ "type":"store", "cell": [1], "data": [{"bssid": "5c:96:9d:65:76:8e", "strength": -78},{"bssid": "5c:96:9d:65:76:8d", "strength": -74},{"bssid": "80:ea:96:eb:1e:fc", "strength": -58},{"bssid": "80:ea:96:eb:1e:fd", "strength": -65},{"bssid": "e0:3f:49:09:d9:98", "strength": -67},{"bssid": "e0:3f:49:09:d9:9c", "strength": -77},{"bssid": "1c:aa:07:6e:31:af", "strength": -56},{"bssid": "00:22:f7:21:d0:38", "strength": -76},{"bssid": "1c:aa:07:6e:31:ad", "strength": -57},{"bssid": "1c:aa:07:b0:7a:bd", "strength": -74},{"bssid": "1c:aa:07:b0:7c:0f", "strength": -86},{"bssid": "1c:aa:07:7b:39:1f", "strength": -68},{"bssid": "64:d1:a3:3b:6f:72", "strength": -71},{"bssid": "1c:aa:07:6e:31:ae", "strength": -57},{"bssid": "1c:aa:07:7b:39:1d", "strength": -67},{"bssid": "1c:aa:07:7b:39:1e", "strength": -68},{"bssid": "1c:aa:07:b0:7a:bf", "strength": -74},{"bssid": "1c:aa:07:b0:80:cd", "strength": -90},{"bssid": "94:10:3e:98:19:6f", "strength": -89},{"bssid": "1c:aa:07:b0:7a:be", "strength": -75},{"bssid": "1c:aa:07:7b:37:0f", "strength": -88},{"bssid": "e8:94:f6:5d:77:16", "strength": -90},{"bssid": "1c:aa:07:b0:7a:b1", "strength": -68},{"bssid": "1c:aa:07:6e:31:a0", "strength": -67},{"bssid": "24:01:c7:76:2a:30", "strength": -77},{"bssid": "1c:aa:07:b0:80:c0", "strength": -83},{"bssid": "1c:aa:07:b0:80:cf", "strength": -91} ]}'
    # Send data
    #sock.sendall(measurement)

    # Look for the response
    #data = sock.recv(1024)
    
    #print >>sys.stderr, 'received "%s"' % data
    
    measurement = '{ "type":"predict", "cell": [1,2], "data": [{"bssid": "5c:96:9d:65:76:8e", "strength": -78},{"bssid": "5c:96:9d:65:76:8d", "strength": -74},{"bssid": "80:ea:96:eb:1e:fc", "strength": -58},{"bssid": "80:ea:96:eb:1e:fd", "strength": -65},{"bssid": "e0:3f:49:09:d9:98", "strength": -67},{"bssid": "e0:3f:49:09:d9:9c", "strength": -77},{"bssid": "1c:aa:07:6e:31:af", "strength": -56},{"bssid": "00:22:f7:21:d0:38", "strength": -76},{"bssid": "1c:aa:07:6e:31:ad", "strength": -57},{"bssid": "1c:aa:07:b0:7a:bd", "strength": -74},{"bssid": "1c:aa:07:b0:7c:0f", "strength": -86},{"bssid": "1c:aa:07:7b:39:1f", "strength": -68},{"bssid": "64:d1:a3:3b:6f:72", "strength": -71},{"bssid": "1c:aa:07:6e:31:ae", "strength": -57},{"bssid": "1c:aa:07:7b:39:1d", "strength": -67},{"bssid": "1c:aa:07:7b:39:1e", "strength": -68},{"bssid": "1c:aa:07:b0:7a:bf", "strength": -74},{"bssid": "1c:aa:07:b0:80:cd", "strength": -90},{"bssid": "94:10:3e:98:19:6f", "strength": -89},{"bssid": "1c:aa:07:b0:7a:be", "strength": -75},{"bssid": "1c:aa:07:7b:37:0f", "strength": -88},{"bssid": "e8:94:f6:5d:77:16", "strength": -90},{"bssid": "1c:aa:07:b0:7a:b1", "strength": -68},{"bssid": "1c:aa:07:6e:31:a0", "strength": -67},{"bssid": "24:01:c7:76:2a:30", "strength": -77},{"bssid": "1c:aa:07:b0:80:c0", "strength": -83},{"bssid": "1c:aa:07:b0:80:cf", "strength": -91} ]}'
    # Send data
    sock.sendall(measurement)

    # Look for the response
    data = sock .recv(1024)
    
    print >>sys.stderr, 'received "%s"' % data
    '''
finally:
  print >>sys.stderr, 'closing socket'
  sock.close()