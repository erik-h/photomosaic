# Photomosaic Generator
### CMPT450 Project
### Erik H

# Compiling

Run `make` in the project root directory to compile all of the programs.

# Running

Run `./run.sh` to execute the programs.

`./run.sh ProcessDB` will print a usage message for preprocessing of database
images.
`./run.sh PhotoMosaic` will print a usage message for creating a photomosiac.

# ProcessDB

This program requires as input a directory of images, or a single image to add
to the database.

The program will create resized versions of all the provided input images. A
CSV file is also created that contains the path to the patch file, along with the
average RGB values of the 4 square regions of the image. This allows the PhotoMosaic
program to run more quickly, as this processing only has to be done once when an
image is added to the database.

The resized images along with the CSV file are placed in the directory `./db/`
which is created if it does not exist.

# PhotoMosaic

A database CSV file generated by ProcessDB is required as one of the parameters
to PhotoMosaic.

The database image dimensions and unique box search size provided as arguments
will determine how well the image turns out (output is also dependant on the
expansiveness of the database).

# Improvements

There are a few improvements I've recognized could be implemented:

- alpha adjust or tinting could be applied to input images to "fudge" them a
  bit, to make a better match
- the deduplication/weighting algorithm can be modified to reduce the number of
  clumped duplicate tiles in the output

PRs implementing these or other ideas are welcome!
