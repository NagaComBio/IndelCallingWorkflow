package de.dkfz.b080.co.indelcallingworkflow;

import de.dkfz.b080.co.common.WorkflowUsingMergedBams;
import de.dkfz.b080.co.files.*;
import de.dkfz.roddy.config.RecursiveOverridableMapContainerForConfigurationValues;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.Tuple2;
import de.dkfz.roddy.knowledge.files.Tuple5;
import de.dkfz.roddy.knowledge.methods.GenericMethod;
import de.dkfz.roddy.tools.LoggerWrapper;

import java.lang.reflect.InvocationTargetException;

/**
 * Indel calling based on the platypus pipeline.
 */
public class IndelCallingWorkflow extends WorkflowUsingMergedBams {

    private static LoggerWrapper logger = LoggerWrapper.getLogger(IndelCallingWorkflow.class.getName());

    @Override
    public boolean execute(ExecutionContext context, BasicBamFile _bamControlMerged, BasicBamFile _bamTumorMerged) {
        BamFile bamControlMerged = new BamFile(_bamControlMerged);
        BamFile bamTumorMerged = new BamFile(_bamTumorMerged);

        RecursiveOverridableMapContainerForConfigurationValues configurationValues = context.getConfiguration().getConfigurationValues();
        boolean runFilter = configurationValues.getBoolean("runIndelVCFFilter", true);
        boolean runDeepAnnotation = configurationValues.getBoolean("runIndelDeepAnnotation", true);
        boolean runAnnotation = configurationValues.getBoolean("runIndelAnnotation", true);
        boolean runTinda = configurationValues.getBoolean("runTinda", true);
        boolean runGermline = configurationValues.getBoolean("GERMLINE_AVAILABLE", true);

        if (!runGermline && runTinda) {
            logger.always("Not running Tinda, since no germline.");
            runTinda = false;
        }

        VCFFileForIndels rawVCF = GenericMethod.callGenericTool(COConstants.TOOL_INDEL_CALLING, bamTumorMerged, bamControlMerged);

        if (runTinda) call("checkSampleSwap", rawVCF, bamTumorMerged, bamControlMerged);

        if (!runAnnotation) return true;
        VCFFileForIndels vcfFileForIndels = rawVCF; //Use the raw vcf for further processing.
        VCFFileForIndels annotatedVCFFile = vcfFileForIndels.annotate().value0;

        if (!runDeepAnnotation) return true;
        VCFFileForIndels deepAnnotatedVCFFile = annotatedVCFFile.deepAnnotate().value0;

        if (!runFilter) return true;
        deepAnnotatedVCFFile.filter();

        return true;
    }
}
