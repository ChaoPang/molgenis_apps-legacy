
#MOLGENIS walltime=48:00:00 nodes=1 cores=1 mem=4

#INPUTS ${preparedStudyDir}/chr${chr}.bgl,${preparedStudyDir}/chr${chr}.markersBeagleFormat,${referenceBeagleFile},${referenceMarkersFile}
#OUTPUTS ${preparedStudyDir}/chr${chr}.bgl
#EXES ${beagle}
#LOGS log
#TARGETS project,chr

#FOREACH project,chr

inputs "${preparedStudyDir}/chr${chr}.bgl"
inputs "${referenceBeagleFile}"
inputs ${referenceMarkersFile}
alloutputsexist "${preparedStudyDir}/chr${chr}.bgl"

mkdir -p ${projectTempDir}/beagle_temp

java -Xmx11g -Djava.io.tmpdir=${projectTempDir}/beagle_temp -jar ${beagle} unphased=${preparedStudyDir}/chr${chr}.bgl phased=${referenceBeagleFile} markers=${referenceMarkersFile} missing=0 out=${imputationResultDir}/results

