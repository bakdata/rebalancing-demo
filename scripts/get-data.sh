#!/usr/bin/env bash

mkdir data
curl -o ./data/data.zip  http://vep.cs.wisc.edu/VEPCorporaRelease/zips/vep_big_names_of_science_v2_txt.zip
unzip ./data/data.zip -d ./data
rm -f ./data/data.zip