#
# =====================================================
# $Id$
# $URL$
# $LastChangedDate$
# $LastChangedRevision$
# $LastChangedBy$
# =====================================================
#

#MOLGENIS walltime=00:10:00
#FOREACH run

#
# Change permissions.
#
umask 0007

#
# Create run dirs.
#
mkdir -p ${runJobsDir}
mkdir -p ${runIntermediateDir}

#
# Create subset of samples for this project.
#
<#--<#assign unfolded = unfoldParametersCSV(parameters) />
<#list unfolded as sampleSequenceDetails>
echo ${sampleSequenceDetails} >> ${runJobsDir}/${run}.csv
</#list>-->
${tooldir}/scripts/extract_samples_from_GAF_list.pl --i ${worksheet} --o ${runJobsDir}/${run}.csv --c run --q ${run}

#
# Execute MOLGENIS/compute to create job scripts to analyse this project.
#
sh ${molgenisComputeDir}/molgenis_compute.sh \
-worksheet=${runJobsDir}/${run}.csv \
-outputscriptsdir=${runJobsDir}/ \
-parametersfile=${parametersFile} \
-workflowfile=${demultiplexWorkflowFile} \
-protocoldir=${protocoldir} \
-cluster=dummy \
-templatesdir=dummy dummy