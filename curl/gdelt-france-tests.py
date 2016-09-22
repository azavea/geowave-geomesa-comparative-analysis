import httplib
import json
import sys, os

LB = 'tf-lb-20160916133649562956791mcd-407657441.us-east-1.elb.amazonaws.com'

# Hack to get stdout to flush so `tee` output shows up per line
sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)

print "==Running tests against France polygon and BBOX, for six months and one month, over whole datasets==\n\n"

connection = httplib.HTTPConnection(LB)

TEST = False

if TEST:
    TEST_STR = "true"
else:
    TEST_STR = "false"

BASE = "/gdelt/spatiotemporal"

def in_france_bbox_six_months(year):
    print "RUNNING FRANCE-BBOX-SIX-MONTHS YEAR %d" % year
    return '%s/in-france-bbox-six-months?test=%s&year=%d&loose=false' % (BASE, TEST_STR, year)

def in_france_bbox_one_month(year):
    print "RUNNING FRANCE-BBOX-ONE-MONTH %d" % year
    return '%s/in-france-bbox-one-month?test=%s&year=%d&loose=false' % (BASE, TEST_STR, year)

def in_france_bbox_six_months_loose(year):
    print "RUNNING FRANCE-BBOX-SIX-MONTHS YEAR %d LOOSE" % year
    return '%s/in-france-bbox-six-months?test=%s&year=%d&loose=true' % (BASE, TEST_STR, year)

def in_france_bbox_one_month_loose(year):
    print "RUNNING FRANCE-BBOX-ONE-MONTH %d LOOSE" % year
    return '%s/in-france-bbox-one-month?test=%s&year=%d&loose=true' % (BASE, TEST_STR, year)

def in_france_six_months(year):
    print "RUNNING FRANCE-BBOX-SIX-MONTHS YEAR %d" % year
    return '%s/in-france-six-months?test=%s&year=%d' % (BASE, TEST_STR, year)

def in_france_one_month(year):
    print "RUNNING FRANCE-BBOX-ONE-MONTH %d" % year
    return '%s/in-france-one-month?test=%s&year=%d' % (BASE, TEST_STR, year)

gwWins = []
gmWins = []

bad = []

def run(req):
    connection.request('GET', req)

    response = connection.getresponse()

    try:
        results = json.loads(response.read().decode())
    except:
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

for year in range(1980,2016):
    run(in_france_bbox_one_month(year))
    run(in_france_bbox_six_months_loose(year))
    run(in_france_bbox_one_month_loose(year))
    run(in_france_six_months(year))
    run(in_france_one_month(year))

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
