#!/bin/bash
#
# Install JDK from "download.java.net"
#
# Adapted from https://github.com/sormuras/bach/blob/master/install-jdk.sh
#

set -e

JDK_FEATURE='10'

TMP=$(curl -L jdk.java.net/${JDK_FEATURE})
TMP="${TMP#*Most recent build: jdk-${JDK_FEATURE}+}"    # remove everything before the number
TMP="${TMP%%<*}"                                        # remove everything after the number
JDK_BUILD="$(echo -e "${TMP}" | tr -d '[:space:]')"     # remove all whitespace

JDK_LICENSE='GPL'

while getopts F:B:L: option
do
 case "${option}"
 in
 F) JDK_FEATURE=${OPTARG};;
 B) JDK_BUILD=${OPTARG};;
 L) JDK_LICENSE=${OPTARG};;
 esac
done

JDK_BASENAME='jdk'
if [ "${JDK_LICENSE}" == 'GPL' ]; then
  JDK_BASENAME='openjdk'
fi

JDK_ARCHIVE=${JDK_BASENAME}-${JDK_FEATURE}+${JDK_BUILD}_linux-x64_bin.tar.gz

cd ~
wget http://download.java.net/java/jdk${JDK_FEATURE}/archive/${JDK_BUILD}/${JDK_LICENSE}/${JDK_ARCHIVE}
tar -xzf ${JDK_ARCHIVE}
export JAVA_HOME=~/jdk-${JDK_FEATURE}
export PATH=${JAVA_HOME}/bin:$PATH
cd -

java --version
