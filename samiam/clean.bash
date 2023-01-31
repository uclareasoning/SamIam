#!/bin/bash
# keith cascio 20050406

effort=

[ -d compiled/edu ] && {
  rm -r compiled/edu
  effort=1
}

[ -d compiled/images ] && {
  rm -r compiled/images
  effort=1
}

[ "${effort}" ] && echo 'samiam was already clean' || echo 'Done cleaning samiam.'
