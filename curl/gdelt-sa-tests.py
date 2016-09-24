import httplib
import json
import sys, os

LB = 'tf-lb-20160919144416835471629f6y-2076752119.us-east-1.elb.amazonaws.com'

# test_country = 'all'
test_country = 'Peru'

# Hack to get stdout to flush so `tee` output shows up per line
sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)

print "==Running tests against South America countries for three week periods==\n\n"

connection = httplib.HTTPConnection(LB)

TEST = True

if TEST:
    TEST_STR = "true"
else:
    TEST_STR = "false"

BASE = "/gdelt/spatiotemporal"

def in_south_america_three_weeks(year, country='all'):
    print "RUNNING SOUTH-AMERICA-THREE-WEEKS %d" % year
    #TODO-FIX
    return '%s/in-south-america-countries-three-weeks?test=%s&year=%d&region=%s' % (BASE, TEST_STR, year, country)

gwWins = []
gmWins = []

bad = []

def run(req):
    connection.request('GET', req)

    response = connection.getresponse()

    try:
        results = json.loads(response.read().decode())
    except:
        print "http://" + LB + "/" + req
        print response.read().decode()
        raise

    for r in results:
        name = r["testName"]
        gwDuration = int(r["gwResult"]["duration"])
        gmDuration = int(r["gmResult"]["duration"])

        gwResult = int(r["gwResult"]["result"])
        gmResult = int(r["gmResult"]["result"])

        print "    %s   GW = %d  GM = %d  RESULTS = %d" % (name, gwDuration, gmDuration, gwResult),

        if gwDuration - gmDuration < 0:
            print "          WWWWWWWWWWWWWWWW",
            gwWins.append(name)
        else:
            print "          MMMMMMMMMMMMMMMM",
            gmWins.append(name)

        if gwResult != gmResult and not "LOOSE" in name:
            print
            print " #######  BAD APPLE!  #######",
            bad.append(name)

        print

# for year in range(1980,2016):
for year in range(2013,2015):
    run(in_south_america_three_weeks(year, test_country))

print "GeoWave wins: %d" % len(gwWins)
for w in gwWins:
    print w

print "GeoMesa wins: %d" % len(gmWins)
for w in gmWins:
    print w

print
if len(bad) > 0:
    print "Bad apples"
    for w in bad:
        print w
