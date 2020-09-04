package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.cas.constraints.Constraints;
import gov.nist.drmf.interpreter.evaluation.constraints.MLPConstraintAnalyzer;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class CaseAnalyzer {
    private static final Logger LOG = LogManager.getLogger(CaseAnalyzer.class.getName());

    private static final Pattern META_INFO_PATTERN = Pattern.compile(
            "\\\\constraint\\{(.*?)} |" +
            "\\\\url\\{(.*?)}|" +
            "\\\\symbolDefined\\[(.*?)]\\{([a-zA-Z0-9.]*?)}|" +
            "\\\\symbolUsed\\[(.*?)]\\{([a-zA-Z0-9.]*?)}|" +
            "\\\\source|\\\\authorproof|\\\\keyphrase|\\\\cite|\\\\comments"
    );

    private static final int CONSTRAINT_GRP = 1;
    private static final int URL_GRP = 2;
    private static final int SYMB_DEF_GRP_SYMB = 3;
    private static final int SYMB_DEF_GRP_ID = 4;
    private static final int SYMB_USED_GRP_SYMB = 5;
    private static final int SYMB_USED_GRP_ID = 6;

    private static final String EOL = "<EOL>";

    private static final Pattern END_OF_MATH_MATCHER = Pattern.compile(
            "^(.*?)[\\\\,;.\\s]*\\Q"+EOL+"\\E.*$"
    );

    public static boolean ACTIVE_BLUEPRINTS = true;

    private static class SymbolDefInfo {
        private String symbolDefSymb = null;
        private String symbolDefID = null;
        private String link = null;

        public SymbolDefInfo(){
            reset();
        }

        void reset() {
            symbolDefID = null;
            symbolDefSymb = null;
            link = null;
        }
    }

    public static SimpleCase extractRawLines(String line, int lineNumber) {
        // matching group
        Matcher metaDataMatcher = META_INFO_PATTERN.matcher(line);
        StringBuffer mathSB = new StringBuffer();

        String url = null;

        // extract all information
        while( metaDataMatcher.find() ) {
            if ( metaDataMatcher.group(URL_GRP) != null ) {
                url = metaDataMatcher.group(URL_GRP);
            }
            metaDataMatcher.appendReplacement(mathSB, EOL);
        }
        metaDataMatcher.appendTail(mathSB);

        Label label = null;
        if ( url != null ) {
            try {
                label = new Label(url);
            } catch (Exception e) {
                LOG.warn("Unable to parse label url: " + url);
            }
        }

        String eq = getEquation(mathSB);
        return new SimpleCase(lineNumber, eq, label);
    }

    /**
     * Creates a case element from a line that contains a test case.
     * Tries to find constraints, labels and split the test case into left- and right-hand site.
     *
     * @param line the entire line with a test case, constraints and so on
     * @param lineNumber the current line number of this test case
     * @return Case object
     */
    public static LinkedList<Case> analyzeLine(String line, int lineNumber, SymbolDefinedLibrary symbDefLib ){
        // matching group
        Matcher metaDataMatcher = META_INFO_PATTERN.matcher(line);
        StringBuffer mathSB = new StringBuffer();

        LinkedList<String> constraints = new LinkedList();
        LinkedList<SymbolTag> symbolsUsed = new LinkedList<>();

        SymbolDefInfo symbInfo = new SymbolDefInfo();

        // extract all information
        while( metaDataMatcher.find() ) {
            fillString(metaDataMatcher, symbInfo, constraints, symbolsUsed);
            metaDataMatcher.appendReplacement(mathSB, EOL);
        }
        metaDataMatcher.appendTail(mathSB);

        String eq = getEquation(mathSB);
        CaseMetaData metaData = CaseMetaData.extractMetaData(constraints, symbolsUsed, symbInfo.link, lineNumber);

        if ( symbInfo.symbolDefID != null && !symbInfo.symbolDefSymb.contains("\\NVar") ) {
            handleNVar(symbInfo, metaData, eq, symbDefLib);
            return null;
        }

        EquationSplitter splitter = new EquationSplitter(metaData);
        return splitter.split(eq);
    }

    private static void fillString(
            Matcher metaDataMatcher,
            SymbolDefInfo symbDef,
            LinkedList<String> constraints,
            LinkedList<SymbolTag> symbolsUsed
    ) {
        if ( metaDataMatcher.group(CONSTRAINT_GRP) != null ) {
            constraints.add(metaDataMatcher.group(CONSTRAINT_GRP));
        } else if ( metaDataMatcher.group(URL_GRP) != null ) {
            symbDef.link = metaDataMatcher.group(URL_GRP);
        } else if ( metaDataMatcher.group(SYMB_DEF_GRP_ID) != null ) {
            symbDef.symbolDefSymb = metaDataMatcher.group(SYMB_DEF_GRP_SYMB);
            symbDef.symbolDefID = metaDataMatcher.group(SYMB_DEF_GRP_ID);
            checkResetSymbs(symbDef);
        } else if ( metaDataMatcher.group(SYMB_USED_GRP_ID) != null ) {
            String id = metaDataMatcher.group(SYMB_USED_GRP_ID);
            String symb = metaDataMatcher.group(SYMB_USED_GRP_SYMB);

            if ( !symb.contains("\\NVar") ){
                SymbolTag used = new SymbolTag(id, symb);
                symbolsUsed.add(used);
            }
        }
    }

    private static void checkResetSymbs(SymbolDefInfo symbDef) {
        if ( symbDef.symbolDefSymb.contains("\\NVar") ) {
            LOG.warn("Found potential definition of macros. Ignore this definition and treat it as normal test case.");
            symbDef.symbolDefSymb = null;
            symbDef.symbolDefID = null;
        }
    }

    private static String getEquation(StringBuffer mathSB) {
        Matcher mathMatcher = END_OF_MATH_MATCHER.matcher(mathSB.toString());
        String eq = "";
        if ( !mathMatcher.matches() ){
            eq = mathSB.toString();
        } else eq = mathMatcher.group(1);
        return eq;
    }

    private static void handleNVar(
        SymbolDefInfo symbInfo,
        CaseMetaData metaData,
        String eq,
        SymbolDefinedLibrary symbDefLib
    ) {
        // TODO you know what, fuck \zeta(z)
        if ( symbInfo.symbolDefSymb.equals("\\zeta(z)") ) symbInfo.symbolDefSymb = "\\zeta";

        EquationSplitter splitter = new EquationSplitter(metaData);
        LinkedList<Case> caseList = splitter.split(eq);
        if ( caseList == null || caseList.isEmpty() ) return;

        Case c = caseList.get(0);
        if ( c.getLHS().equals( symbInfo.symbolDefSymb ) ) {
            LOG.info("Store line definition: " + symbInfo.symbolDefSymb + " is defined as " + c.getRHS());
            symbDefLib.add(
                    symbInfo.symbolDefID,
                    symbInfo.symbolDefSymb,
                    c.getRHS(),
                    metaData
            );
        } else {
            LOG.warn("LHS does not match defined symbol:" + c.getLHS() + " vs " + symbInfo.symbolDefSymb);
        }
    }

    public static Relations getRelation(String eq) {
        if ( eq.matches(".*(?:\\\\leq?|<=).*") ){
            return Relations.LESS_EQ_THAN;
        }
        else if ( eq.matches( ".*(?:\\\\geq?|=>).*" ) ){
            return Relations.GREATER_EQ_THAN;
        }
        else if ( eq.matches( ".*(?:\\\\neq?|<>).*" ) ){
            return Relations.UNEQUAL;
        }
        else if ( eq.contains("<") ){
            return Relations.LESS_THAN;
        }
        else if ( eq.contains( ">" ) ){
            return Relations.GREATER_THAN;
        }
        else if ( eq.contains( "=" ) ) {
            return Relations.EQUAL;
        }
        else return null;
    }
}
