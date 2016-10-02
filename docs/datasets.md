# Datasets

We performed performance tests on three different data sets, which are described below.

## GeoLife

This GPS trajectory dataset was collected in (Microsoft Research Asia) Geolife project by 182 users in a period of over five years (from April 2007 to August 2012). A GPS trajectory of this dataset is represented by a sequence of time-stamped points, each of which contains the information of latitude, longitude and altitude. This dataset contains 17,621 trajectories with a total distance of 1,292,951kilometers and a total duration of 50,176 hours. These trajectories were recorded by different GPS loggers and GPS- phones, and have a variety of sampling rates. 91.5 percent of the trajectories are logged in a dense representation, e.g. every 1~5 seconds or every 5~10 meters per point. Although this dataset is wildly distributed in over 30 cities of China and even in some cities located in the USA and Europe, the majority of the data was created in Beijing, China.

Text taken from the GeoLife user guide, found at https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/User20Guide-1.2.pdf

### GDELT

GDELT—Global Data on Events, Location and Tone—is a new CAMEO-coded data
set containing more than 200-million geolocated events with global coverage for 1979 to
the present. The data are based on news reports from a variety of international news
sources coded using the Tabari system for events and additional software for location
and tone. The data is freely available and is updated daily. The GDELT data we have tested against
contains data up through August 2016.

Text taken from the ISA 2013 paper introducing GDELT, found at http://data.gdeltproject.org/documentation/ISA.2013.GDELT.pdf

### Synthesized Tracks

We tested against a dataset supplied by a third party that that contain a total of 6.34 million synthesized tracks.
This set of tracks has a median length of 29.8 km, a mean length of 38.82 km and each track contains an average of 491.45 points.
There is approx. 35.88 GB of data compressed and stored as around 730 avro encoded files.
The tracks are generated through a statistical process using Global Open Street Map data and Global Landscan data as inputs.
The dataset is available at `s3://geotrellis-sample-datasets/generated-tracks/`

Here is a view of the data for a specific time slice of the data, as shown in GeoServer:

![Synthetic Tracks SIZE::60](img/tracks/synthetic-tracks.png)

##### Track Length Stats (in miles)

|     count         |  min | max | mean | std dev | median | skewness | kurtosis |
|:-----------------:|:-----------------:|:-----------------:|:-----------------:|:-----------------:|:-----------------:|:-----------------:|:-----------------:|
| 2054751           |0.064998          |2839.198486       |38.829134         |115.975988        |29.791367         |15.466978         |266.782216        |
