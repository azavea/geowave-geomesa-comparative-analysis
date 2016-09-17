import httplib
import json
import sys, os

LB = 'tf-lb-20160916133649562956791mcd-407657441.us-east-1.elb.amazonaws.com'

test_region = 'all'
# test_region = 'Picardie'

start_year = 1990
end_year = 2016

# Hack to get stdout to flush so `tee` output shows up per line
sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)

print "==Running tests against France region polygon for two years, one year, ten months, six months and three month, over whole datasets==\n\n"

connection = httplib.HTTPConnection(LB)

TEST = True

if TEST:
    TEST_STR = "true"
else:
    TEST_STR = "false"

BASE = "/gdelt/spatiotemporal"

def in_france_regions_two_years(year, region='all'):
    print "RUNNING FRANCE-REGIONS-TWO-YEARS %d" % year
    return '%s/in-france-regions-two-years?test=%s&year=%d&region=%s' % (BASE, TEST_STR, year, region)

def in_france_regions_bbox_one_year(year, region='all'):
    print "RUNNING FRANCE-REGIONS-ONE-YEAR %d" % year
    return '%s/in-france-regions-one-year?test=%s&year=%d&region=%s' % (BASE, TEST_STR, year, region)

def in_france_regions_ten_months(year, region='all'):
    print "RUNNING FRANCE-REGIONS-TEN-MONTHS %d" % year
    return '%s/in-france-regions-ten-months?test=%s&year=%d&region=%s' % (BASE, TEST_STR, year, region)

def in_france_regions_six_months(year, region='all'):
    print "RUNNING FRANCE-REGIONS-SIX-MONTHS %d" % year
    return '%s/in-france-regions-six-months?test=%s&year=%d&region=%s' % (BASE, TEST_STR, year, region)

def in_france_regions_three_months(year, region='all'):
    print "RUNNING FRANCE-REGIONS-THREE-MONTHS %d" % year
    return '%s/in-france-regions-three-months?test=%s&year=%d&region=%s' % (BASE, TEST_STR, year, region)

gwWins = []
gmWins = []

bad = []

def run(req):
    connection.request('GET', req)

    response = connection.getresponse()

    try:
        results = json.loads(response.read().decode())
    except:
        print "http://" + LB + req
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

for year in range(start_year,end_year + 1):
    run(in_france_regions_two_years(year, test_region))
    run(in_france_regions_bbox_one_year(year, test_region))
    run(in_france_regions_ten_months(year, test_region))
    run(in_france_regions_six_months(year, test_region))
    run(in_france_regions_three_months(year, test_region))

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
