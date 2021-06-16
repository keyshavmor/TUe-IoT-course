#!/bin/bash
#Bash script for image acquisition
echo "Capturing and Filtering Number Plate"


alpr -c eu nl_59-xh-jt.jpg -j>nl_59-xh-jt.json

python compare.py


