#!/bin/bash
# author: keith cascio, since: 20050406

# CUCIS stands for Clean, Update & Compile Inflib & SamIam

if ! source mypaths.bash &> /dev/null; then
  echo 'Did not find mypaths.bash, but '"$0"' relies on it.'
  echo '(1) Create a single file named mypaths.bash based on "mypaths.template.bash" from the samiam module.'
  echo '(2) Put mypaths.bash in a directory in your search path (e.g. ~/bin) so '"$0"' can find it.'
  echo '(3) Edit mypaths.bash, substituting the paths as they exist on your system.'
  exit 1
fi

echo "Cleaning  inflib @ $INFLIBPATH ..."
cd $INFLIBPATH
clean.bash &> /dev/null &
id_clean_inflib=$!

updatesafe()
{
  cvs update -d -P 2> /dev/null &
  id_save=$!;
  echo $id_save >| .cucis_pid_save.dat;
}

echo "Updating  inflib @ $INFLIBPATH ..."
cd $INFLIBPATH
updatesafe | grep -v '^?' &
id_update_inflib=`cat .cucis_pid_save.dat`;

echo "Cleaning  samiam @ $SAMIAMPATH ..."
cd $SAMIAMPATH
clean.bash &> /dev/null &
id_clean_samiam=$!

waitsafe()
{
	if [ -z "$1" ]; then
		return 1;
	fi
	if ! kill -0 $1 2> /dev/null; then
		return 0;
	fi
	wait "$1" &> /dev/null;
	waitexitval=$?;
	if [ $waitexitval -eq 127 ]; then
		# 127 means process id not a child of this shell
		# probably because process already terminated
		# that's OK, return non-error
		waitexitval=0;
	fi
	return $waitexitval;
}

failure=0

if ! waitsafe $id_clean_inflib; then
	echo "failed to clean inflib";
	failure=1
fi

if ! waitsafe $id_update_inflib; then
	echo "failed to update inflib";
	failure=1
fi

if [ $failure -eq 0 ]; then
	echo "Compiling inflib @ $INFLIBPATH ..."
	cd $INFLIBPATH
	compile.bash > /dev/null &
	id_compile_inflib=$!
fi

echo "Updating  samiam @ $SAMIAMPATH ..."
cd $SAMIAMPATH
updatesafe | grep -v '^?' &
id_update_samiam=`cat .cucis_pid_save.dat`;

if ! waitsafe $id_clean_samiam; then
	echo "failed to clean samiam";
	failure=1
fi

if ! waitsafe $id_update_samiam; then
	echo "failed to update samiam";
	failure=1;
fi

if ! waitsafe $id_compile_inflib; then
	echo "failed to compile inflib";
	failure=1
fi

if [ $failure -eq 0 ]; then
	echo "Compiling samiam @ $SAMIAMPATH ."
	cd $SAMIAMPATH
	compile.bash > /dev/null &
	id_compile_samiam=$!
fi

if ! waitsafe $id_compile_samiam; then
        echo "failed to compile samiam";
        failure=1;
fi

if [ -f .cucis_pid_save.dat ]; then
	rm .cucis_pid_save.dat 2>&1 > /dev/null;
fi

echo "Done $0"
