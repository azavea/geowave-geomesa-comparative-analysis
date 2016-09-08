#!/usr/bin/env bash

# This script builds an SBT project and upload the assembly jar to S3.
# Only output to STDOUT is the S3 URI of the uploaded jar.
# If the file already exists it will not be re-uploaded, intentionally.
# Instead of re-uploading increment the version number of the project.
PROJ_DIR=$1   # SBT project directory
PROJ_NAME=$2  # SBT project or sub-project to build
S3_URI=$3     # Base S3 URI to stage assembly
SBT_CMD=${SBT_CMD:-sbt}

cd $PROJ_DIR
(>&2 echo "Building assembly for $PROJ_NAME in $PROJ_DIR")
ASSEMBLY_BLOB=$($PROJ_DIR/sbt "project $PROJ_NAME" assembly)

if [[ $ASSEMBLY_BLOB =~ ([a-zA-Z0-9\.\/_-]+target\/scala[a-zA-Z0-9\.\/_-]+jar) ]]; then
    ASSEMBLY_JAR=${BASH_REMATCH[1]}
    (>&2 echo "Assembly: $ASSEMBLY_JAR")
else
    (>&2 echo "Unable to parse project assembly location (or compilation failed)")
    exit 1
fi

S3_ASSEMBLY_URI=$S3_URI/$(basename $ASSEMBLY_JAR)

if aws s3 ls $S3_ASSEMBLY_URI > /dev/null; then
    (>&2 echo "Found $S3_ASSEMBLY_URI, skipping upload")
else
    (>&2 aws s3 cp $ASSEMBLY_JAR $S3_ASSEMBLY_URI)
fi

echo $S3_ASSEMBLY_URI
