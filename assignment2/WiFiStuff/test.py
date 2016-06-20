import json
from sklearn import svm
import random

f = open('wifimeasurements.dat', 'r');

encoded = f.read();

# Store list of measurements
measurements = json.loads(encoded)['measurements'];

# Create SVM
#clf = svm.SVC(decision_function_shape='ovo')

classifiers = []
X = []
y = []
Xtest = []
ytest = []
known_ssids = []


def addMeasurement(measurement, isTest):
  cell = int(measurement['cell'])
  data = measurement['data']
  
  #featureset = list(base_featureset)
  
  for sample in data:
    bssid = sample['bssid']
    strength = int(sample['strength'])
    if bssid not in known_ssids:
      known_ssids.append(bssid)
      classifiers.append(svm.SVC(decision_function_shape='ovo'))
      X.append([])
      Xtest.append([])
      y.append([])
      ytest.append([])
    idx = known_ssids.index(bssid)
    #featureset[idx] = strength
  
    if isTest:
      Xtest[idx].append([strength])
      ytest[idx].append(cell)
    else:
      X[idx].append([strength])
      y[idx].append(cell)
  
for i in xrange(0, len(measurements)):
  if random.random() < 0.1:
    addMeasurement(measurements[i], True)
  else:
    addMeasurement(measurements[i], False)
    

for i in xrange(0, len(classifiers)):
  classifiers[i].fit(X[i], y[i])
  print ytest[i]
  print known_ssids[i] + ' ' + str(classifiers[i].score(Xtest[i], ytest[i]))