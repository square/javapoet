#!/bin/bash

set -ex

REPO="git@github.com:square/javapoet.git"
GROUP_ID="com.squareup"
ARTIFACT_ID="javapoet"

DIR=temp-clone

# Delete any existing temporary website clone
rm -rf $DIR

# Clone the current repo into temp folder
git clone $REPO $DIR

# Move working directory into temp folder
cd $DIR

# Checkout and track the gh-pages branch
git checkout -t origin/gh-pages

# Delete everything
rm -rf *

# Download the latest javadoc to directories like 'javadoc/javapoet'.
for DOCUMENTED_ARTIFACT in javawriter javapoet
do
  curl -L "https://search.maven.org/remote_content?g=$GROUP_ID&a=$DOCUMENTED_ARTIFACT&v=LATEST&c=javadoc" > javadoc.zip
  JAVADOC_DIR="javadoc/${DOCUMENTED_ARTIFACT}"
  mkdir -p $JAVADOC_DIR
  unzip javadoc.zip -d $JAVADOC_DIR
  rm javadoc.zip
done

# Stage all files in git and create a commit
git add .
git add -u
git commit -m "Website at $(date)"

# Push the new files up to GitHub
git push origin gh-pages

# Delete our temp folder
cd ..
rm -rf $DIR
