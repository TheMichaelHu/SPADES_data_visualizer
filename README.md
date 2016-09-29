# SPADES data visualizer
Generates visualization of SPADES data

## How to use
Create an executable `.jar` file using the source code. Be sure to include the apache commons cli external library.
This program supports CLI-style command like arguments:
* -w, --width, image width
* -h, --height, image row height
* -i, --input-dir, dir containing `MasterSynced` directory
* -o, --output-dir, target dir to save visualization to
* -s, --sensors, list of sensors to draw

###Example:
`./dataVisualizer.jar -i [input dir] -o [output dir] -s phone actigraph`

## Directory Structure
It's assumed the the SPADES dataset looks something like:
* SPADES_#
  * data
    * SPADES_# _(this is the input dir)_
      * MasterSynced
  * survey
  * Sessions.csv
  * sensor_locations.csv
