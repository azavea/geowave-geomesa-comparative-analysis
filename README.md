# GeoWave/GeoMesa comparative analysis

A frequently asked question in the geo big data community is: what is the differences between GeoWave and GeoMesa? Azavea performed a focused comparative analysis of the two projects, and this project encapsulates the results of that effort.

## Contents

This repository contains documentation about our understanding of the GeoWave and GeoMesa projects, and also a completely repeatable deployment of the performance tests we ran. Here is an overview of the various sections of the repository:

### deployment

This folder contains

### emperical-data

### synthetic-data

### query-server

### client

This directory houses some python code and shell scripts that are used to hit the endpoints of a running query server.

### analyze

This directory is used to house code that pulls down result data from DynamoDB as a CSV file, and to do some simple python analysis on that data.

## Reference docs

https://locationtech.org/mhonarc/lists/location-iwg/msg01550.html - Initial email to the LocationTech mailing list about the comparative analysis effort.
