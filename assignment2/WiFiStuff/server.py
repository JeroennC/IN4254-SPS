import socket
import sys
import json
import struct
from sklearn import svm
from sklearn.linear_model import perceptron
from random import shuffle

class APClassifier:
  def __init__(self, bssid):
    self.bssid = bssid
    self.X = []
    self.y = []
    #self.clf = perceptron.Perceptron(n_iter=100, verbose=0, random_state=None, fit_intercept=True, eta0=0.002)
    self.clf = svm.SVC(decision_function_shape='ovo', probability=True)
    self.classes = []
    
  def addSample(self, strength, cell):
    self.X.append([strength])
    self.y.append(cell)
    if cell not in self.classes:
      self.classes.append(cell)
    
  def containsSample(self):
    return len(self.y) > 0

  def predict(self, strength):
    if len(self.classes) == 1:
      return (self.classes, [[1]])
      
    self.clf.fit(self.X, self.y)
    return (self.clf.classes_, self.clf.predict_proba([strength]))
    

known_bssids = []
classifiers = []
    
def predictCells(measurement):
  data = measurement['data']
  sum = 0
  cellDist = [None] * 21
  for i in xrange(0, 21):
    cellDist[i] = 0
  
  for sample in data:
    bssid = sample['bssid']
    strength = int(sample['strength'])
    if bssid not in known_bssids:
      continue
      
    idx = known_bssids.index(bssid)
    
    (cells, probs) = classifiers[idx].predict([strength])
    
    for i in range(len(cells)):
      cellDist[cells[i]] += probs[0][i]
      sum += probs[0][i]
      
  # Normalize
  if sum > 0:
    for i in xrange(0, 21):
      cellDist[i] /= sum
  
  return cellDist
  
def storeData(measurement):
  cells = measurement['cell']
  data = measurement['data']
  
  for sample in data:
    bssid = sample['bssid']
    strength = int(sample['strength'])
    if bssid not in known_bssids:
      known_bssids.append(bssid)
      classifiers.append(APClassifier(bssid))
      
    idx = known_bssids.index(bssid)
    # Shuffle cells to not have last cell come up
    shuffle(cells)
    
    for cell in cells:
      cell = int(cell)
      classifiers[idx].addSample(strength, cell)
  return
  
def reset():
  known_bssids = []
  classifiers = []
  print "Reset everything"
  return
  
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

        # Receive the data and use it
        while True:
            data = recv_message(connection)
            
            if not data:
              break
            #print >>sys.stderr, 'received "%s"' % data
            # Decode the received JSON
            measurement = json.loads(data)
            if measurement['type'] == 'store':
              # Store
              storeData(measurement)
              send_message(connection, "Measurement stored")
            elif measurement['type'] == 'predict':
              # Predict
              prediction = predictCells(measurement)
              send_message(connection, str(prediction))
            elif measurement['type'] == 'reset':
              reset()
              send_message(connection, "Classifiers reset")
            else:
              send_message(connection, "Unknown request")
            
    finally:
        # Clean up the connection
        connection.close()