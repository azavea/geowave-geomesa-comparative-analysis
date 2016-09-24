import httplib
import json
import sys, os

TEST = False

LB = 'tf-lb-20160919144416835471629f6y-2076752119.us-east-1.elb.amazonaws.com'


# test_region = 'Picardie'
cities = ["Paris",
          "Philadelphia",
          "Istanbul",
          "Baghdad",
          "Tehran",
          "Beijing",
          "Tokyo",
          "Oslo",
          "Khartoum",
          "Johannesburg"]

# start_year = 1990
# end_year = 2016

start_year = 2000
end_year = 2016

# Hack to get stdout to flush so `tee` output shows up per line
sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)

print "==Running tests against city buffer polygons for two years, one year, ten months, six months and three month, over whole datasets==\n\n"

connection = httplib.HTTPConnection(LB)

if TEST:
    TEST_STR = "true"
else:
    TEST_STR = "false"

BASE = "/gdelt/spatiotemporal"

def in_city_buffers_fourteen_months(year, city):
    print "RUNNING CITYS-FOURTEEN-MONTHS %s %d" % (city, year)
    return '%s/in-city-buffers-fourteen-months?test=%s&year=%d&city=%s' % (BASE, TEST_STR, year, city)

def in_city_buffers_ten_months(year, city):
    print "RUNNING CITYS-TEN-MONTHS %s %d" % (city, year)
    return '%s/in-city-buffers-ten-months?test=%s&year=%d&city=%s' % (BASE, TEST_STR, year, city)

def in_city_buffers_six_months(year, city):
    print "RUNNING CITYS-SIX-MONTHS %s %d" % (city, year)
    return '%s/in-city-buffers-six-months?test=%s&year=%d&city=%s' % (BASE, TEST_STR, year, city)

def in_city_buffers_two_months(year, city):
    print "RUNNING CITYS-TWO-MONTHS %s %d" % (city, year)
    return '%s/in-city-buffers-two-months?test=%s&year=%d&city=%s' % (BASE, TEST_STR, year, city)

def in_city_buffers_two_weeks(year, city):
    print "RUNNING CITYS-TWO-WEEKS %s %d" % (city, year)
    return '%s/in-city-buffers-two-weeks?test=%s&year=%d&city=%s' % (BASE, TEST_STR, year, city)

def in_city_buffers_six_days(year, city):
    print "RUNNING CITYS-SIX-DAYS %s %d" % (city, year)
    return '%s/in-city-buffers-six-days?test=%s&year=%d&city=%s' % (BASE, TEST_STR, year, city)

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
        print response.read()
        print response.read().decode()
        raise

    for r in results:
        name = r["testName"]
        gwDuration = int(r["gwResult"]["duration"])
        gmDuration = int(r["gmResult"]["duration"])

        gwResult = int(r["gwResult"]["result"])
        gmResult = int(r["gmResult"]["result"])

        print "    %s   GW = %d  GM = %d  RESULTS = (%d,%d)" % (name, gwDuration, gmDuration, gwResult, gmResult),

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


if __name__ == '__main__':
    for year in range(start_year,end_year + 1):
        for city in cities:
            run(in_city_buffers_fourteen_months(year, city))
            run(in_city_buffers_ten_months(year, city))
            run(in_city_buffers_six_months(year, city))
            run(in_city_buffers_two_months(year, city))
            run(in_city_buffers_two_weeks(year, city))
            run(in_city_buffers_six_days(year, city))

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
