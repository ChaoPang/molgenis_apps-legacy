#
# =====================================================
# $Id$
# $URL$
# $LastChangedDate$
# $LastChangedRevision$
# $LastChangedBy$
# =====================================================
#

#MOLGENIS walltime=65:59:00 mem=12 cores=1
#FOREACH externalSampleID

inputs "${mergedbam}"
alloutputsexist "${sample}.coverage.csv" \
"${samplecoverageplotpdf}" \
"${sample}.coverage.Rdata"

export R_HOME=${R_HOME}
export PATH=${R_HOME}/bin:<#noparse>${PATH}</#noparse>
export R_LIBS=${R_LIBS} 

${coveragescript} \
--bam ${mergedbam} \
--chromosome 1 \
--interval_list ${targetintervals} \
--csv ${sample}.coverage.csv \
--pdf ${samplecoverageplotpdf} \
--Rcovlist ${sample}.coverage.Rdata
