import httplib
import json
import sys, os
from random import shuffle

TEST = False

LB = 'tf-lb-201609261113030729130213z2-121839236.us-east-1.elb.amazonaws.com'


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

south_american_countries = ["Bolivia",
                            "Falkland-Islands",
                            "Guyana",
                            "Suriname",
                            "Venezuela",
                            "Peru",
                            "Ecuador",
                            "Paraguay",
                            "Uruguay",
                            "Chile",
                            "Colombia",
                            "Brazil",
                            "Argentina"]

sizes = [10, 50, 150, 250, 350, 450, 550, 650]

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

def in_city_buffers_fourteen_months(year, city, size):
    return '%s/in-city-buffers-fourteen-months?test=%s&year=%d&city=%s&size=%d' % (BASE, TEST_STR, year, city, size)

def in_city_buffers_ten_months(year, city, size):
    return '%s/in-city-buffers-ten-months?test=%s&year=%d&city=%s&size=%d' % (BASE, TEST_STR, year, city, size)

def in_city_buffers_six_months(year, city, size):
    return '%s/in-city-buffers-six-months?test=%s&year=%d&city=%s&size=%d' % (BASE, TEST_STR, year, city, size)

def in_city_buffers_two_months(year, city, size):
    return '%s/in-city-buffers-two-months?test=%s&year=%d&city=%s&size=%d' % (BASE, TEST_STR, year, city, size)

def in_city_buffers_two_weeks(year, city, size):
    return '%s/in-city-buffers-two-weeks?test=%s&year=%d&city=%s&size=%d' % (BASE, TEST_STR, year, city, size)

def in_city_buffers_six_days(year, city, size):
    return '%s/in-city-buffers-six-days?test=%s&year=%d&city=%s&size=%d' % (BASE, TEST_STR, year, city, size)

def in_south_america_three_weeks(year, country):
    return '%s/in-south-america-countries-three-weeks?test=%s&year=%d&country=%s' % (BASE, TEST_STR, year, country)

gwWins = []
gmWins = []

gwBads = {}
gmBads = {}

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

        if gwDuration > 10000:
            print
            print " ##### BAD RESULT! GEOWAVE: %d ms" % gwDuration
            if not req in gwBads:
                gwBads[req] = 0
            gwBads[req] += 1

        if gmDuration > 10000:
            print
            print " ##### BAD RESULT! GEOMESA: %d ms" % gmDuration
            if not req in gmBads:
                gmBads[req] = 0
            gmBads[req] += 1

        print

urls = []

for year in range(start_year,end_year + 1):
    for city in cities:
        for size in sizes:
            urls.append(in_city_buffers_six_months(year, city, size))
            urls.append(in_city_buffers_two_months(year, city, size))
            urls.append(in_city_buffers_two_weeks(year, city, size))
            urls.append(in_city_buffers_six_days(year, city, size))
    for country in south_american_countries:
        urls.append(in_south_america_three_weeks(year, country))

shuffle(urls)

if __name__ == '__main__':
    times = 1
    if 'loop' in sys.argv:
        print "LOOPING!"
        times = 10000
    elif len(sys.argv) > 1:
        times = sys.argv[1]

    for run_id in range(0, times):
        print "-------------RUNNING %d TESTS: Run %d----------------" % (len(urls), run_id)
        for url in urls:
            print "RUNNING %s" % url
            run(url)

    print "GeoWave wins: %d" % len(gwWins)
    for w in gwWins:
        print w

    print "GeoMesa wins: %d" % len(gmWins)
    for w in gmWins:
        print w

    print "GeoWave bads: %d" % len(gwBads)
    for k in gwBads:
        print "%s - %d times" % (k, gwBads[k])

    print "GeoMesa bads: %d" % len(gwBads)
    for k in gmBads:
        print "%s - %d times" % (k, gmBads[k])


    print
    if len(bad) > 0:
        print "Bad apples"
        for w in bad:
            print w
