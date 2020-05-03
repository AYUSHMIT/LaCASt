package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.cas.common.IForwardTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.mlp.MacrosLexicon;
import mlp.FeatureSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class MacroInfoHolder {
    private static final Logger LOG = LogManager.getLogger(MacroInfoHolder.class.getName());

    private int
            numOfParams = Integer.MIN_VALUE,
            numOfAts    = Integer.MIN_VALUE,
            numOfVars   = Integer.MIN_VALUE,
            slotOfDifferentiation = Integer.MIN_VALUE;

    private String constraints;

    private String defDlmf, defCas;

    private String translationPattern, alternativePattern;

    private String branchCuts, casBranchCuts;

    private final String macro;

    private String variableOfDifferentiation = null;

    private MacroMetaInformation metaInformation;

    /**
     * Store information about the macro from an feature set.
     * @param fset future set
     * @param macro the macro
     * @throws TranslationException if the feature set does not provide
     * the necessary information for a translation
     */
    public MacroInfoHolder(
            IForwardTranslator translator,
            FeatureSet fset,
            String CAS,
            String macro
    ) throws TranslationException {
        this.macro = macro;
        this.checkFeatureSetValidity(translator, fset);
        this.storeInfosValidityCheck(translator, fset, CAS);
    }

    private void checkFeatureSetValidity(IForwardTranslator translator, FeatureSet fset)
            throws TranslationException {
        if ( fset == null ) {
            throw TranslationException.buildExceptionObj(
                    translator, "Cannot extract information from feature set: " + macro,
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    macro);
        }
    }

    private void storeInfosValidityCheck(IForwardTranslator translator, FeatureSet fset, String CAS)
            throws TranslationException {
        // try to extract the information
        try {
            this.storeInfos(fset, CAS);
            this.handleMultipleAlternativePatterns(CAS);
            if (getTranslationPattern() == null || getTranslationPattern().isEmpty()) {
                throw TranslationException.buildExceptionObj(
                        translator, "There are no translation patterns available for: " + macro,
                        TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                        macro);
            }
        } catch (NullPointerException | TranslationException npe) {
            throw TranslationException.buildExceptionObj(
                    translator, "Cannot extract information from feature set: " + macro,
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    macro);
        }
    }

    private void handleMultipleAlternativePatterns(String cas) {
        // maybe the alternative pattern got multiple alternatives
        if (!alternativePattern.isEmpty()) {
            try {
                alternativePattern = alternativePattern.split(MacrosLexicon.SIGNAL_INLINE)[0];
            } catch (Exception e) {
                throw new TranslationException(
                        Keys.KEY_LATEX,
                        cas,
                        "Cannot split alternative macro pattern!",
                        TranslationExceptionReason.DLMF_MACRO_ERROR);
            }

            if (translationPattern.isEmpty()) {
                LOG.warn("No direct translation available! Switch to alternative mode for " + macro);
                translationPattern = alternativePattern;
            }
        }
    }

    /**
     * Analyzes and extracts all information from a given feature set of a DLMF macro.
     * @param fset the feature set
     */
    private void storeInfos(FeatureSet fset, String CAS) {
        //LOG.info("Extract information for " + macro_term.getTermText());
        // now store all additional information
        // first of all number of parameters, ats and vars
        numOfParams = Integer.parseInt(DLMFFeatureValues.params.getFeatureValue(fset, CAS));
        numOfAts = Integer.parseInt(DLMFFeatureValues.ats.getFeatureValue(fset, CAS));
        numOfVars = Integer.parseInt(DLMFFeatureValues.variables.getFeatureValue(fset, CAS));

        try { // true slot is argument slot + numOfParams
            slotOfDifferentiation = Integer.parseInt(DLMFFeatureValues.slot.getFeatureValue(fset, CAS)) + numOfParams;
        } catch (NumberFormatException e) {
            //TODO should default be 1 or throw an exception?
            slotOfDifferentiation = 1; // if slot isn't in lexicon, value is null
        }

        // now store additional information about the translation
        // Meaning: name of the function (defined by DLMF)
        // Description: same like meaning, but more rough. Usually there is only one of them defined (meaning|descreption)
        // Constraints: of the DLMF definition
        // Branch Cuts: of the DLMF definition
        // DLMF: its the plain, smallest version of the macro. Like \JacobiP{a}{b}{c}@{d}
        //      we can reference our Constraints to a, b, c and d now. That makes it easier to read
        constraints = DLMFFeatureValues.constraints.getFeatureValue(fset, CAS);
        branchCuts = DLMFFeatureValues.branch_cuts.getFeatureValue(fset, CAS);

        // Translation information
        translationPattern = DLMFFeatureValues.CAS.getFeatureValue(fset, CAS);
        alternativePattern = DLMFFeatureValues.CAS_Alternatives.getFeatureValue(fset, CAS);
        casBranchCuts = DLMFFeatureValues.CAS_BranchCuts.getFeatureValue(fset, CAS);

        // links to the definitions
        defDlmf = DLMFFeatureValues.dlmf_link.getFeatureValue(fset, CAS);
        defCas = DLMFFeatureValues.CAS_Link.getFeatureValue(fset, CAS);

        metaInformation = new MacroMetaInformation(fset, CAS);
    }

    public boolean isWronskian() {
        return macro.equals("\\Wronskian");
    }

    public String getMacro() {
        return macro;
    }

    public String getVariableOfDifferentiation() {
        return variableOfDifferentiation;
    }

    public void setVariableOfDifferentiation(String variableOfDifferentiation) {
        this.variableOfDifferentiation = variableOfDifferentiation;
    }

    public int getNumOfParams() {
        return numOfParams;
    }

    public int getNumOfAts() {
        return numOfAts;
    }

    public int getNumOfVars() {
        return numOfVars;
    }

    public int getSlotOfDifferentiation() {
        return slotOfDifferentiation;
    }

    public String getConstraints() {
        return constraints;
    }

    public String getDefDlmf() {
        return defDlmf;
    }

    public String getDefCas() {
        return defCas;
    }

    public String getTranslationPattern() {
        return translationPattern;
    }

    public String getAlternativePattern() {
        return alternativePattern;
    }

    public String getBranchCuts() {
        return branchCuts;
    }

    public String getCasBranchCuts() {
        return casBranchCuts;
    }

    public MacroMetaInformation getMetaInformation() {
        return metaInformation;
    }
}
