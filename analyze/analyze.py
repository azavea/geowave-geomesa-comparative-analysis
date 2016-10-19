import os
import csv

path = "gwgm-ca-run-results-initial-geolife-Sept21.csv"

fields = ["uuid","duration","system","timeAtFirstResult","clusterId","startTime","endTime","result","queryName"]

# clusterIds = ['j-2XPAUZO5DFFGZ', 'j-2D7LTJ7YTZSVR']
# clusterIds = ['j-224ANWOQ9RL3V', 'j-3G1WOHPKBA7HO']
# clusterIds = ['j-11FRN8ESB4T4O',
#               'j-14A8QWE1YUDDW',
#               'j-21PU6MN5BQ7FF',
#               'j-Q6SLDGT5OIA0',
#               'j-3600OD0Q6ZXZV',
#               'j-1CLETSC45VWKT'] # geolife

all_good = ['j-Q6SLDGT5OIA0',
            'j-11FRN8ESB4T4O',
            'j-14A8QWE1YUDDW',
            'j-3600OD0Q6ZXZV',
            'j-1CLETSC45VWKT',
            'j-2XPAUZO5DFFGZ',
            'j-2D7LTJ7YTZSVR',
            'j-224ANWOQ9RL3V',
            'j-3G1WOHPKBA7HO']

cluserIds = all_good

results = {}

with open(path, 'rb') as csvfile:
    reader = csv.reader(csvfile)
    reader.next()
    for row in reader:
        (duration, system, timeAtFirstResult, clusterId, startTime, endTime, count, queryName) = (int(row[1]), row[2], int(row[3]), row[4], int(row[5]), int(row[6]), int(row[7]), row[8])
        if clusterId in clusterIds and count > 1000 and 'BBOXES-ITERATE-8-' in queryName:
            if not queryName in results:
                results[queryName] = {}
            if not system in results[queryName]:
                results[queryName][system] = []
            if 'count' in results[queryName]:
                if results[queryName]['count'] != count:
                    print "DOESN'T MATCH %s %d %d" % (queryName, count, results[queryName]['count'])
            else:
                results[queryName]['count'] = count
            results[queryName]['%s-COUNT' % system] = count
            results[queryName][system].append((duration, timeAtFirstResult - startTime, endTime - timeAtFirstResult))

def print_results():
    for test in results:
        print test
        print "         GW: %d             GM: %d" % (results[test]['GW-COUNT'], results[test]['GM-COUNT'])
        gw = results[test]['GW']
        gm = results[test]['GM']
        for i in range(0, len(gw)):
            wr = gw[i]
            mr = gm[i]
            print "    %s      %s" % (str(wr), str(mr))

if __name__ == "__main__":
    print_results()
