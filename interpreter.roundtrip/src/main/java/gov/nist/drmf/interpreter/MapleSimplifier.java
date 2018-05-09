package gov.nist.drmf.interpreter;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import com.maplesoft.openmaple.MString;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static gov.nist.drmf.interpreter.examples.MLP.NL;

/**
 *
 * Created by AndreG-P on 27.04.2017.
 */
public class MapleSimplifier {
    private static final Logger LOG = LogManager.getLogger(MapleSimplifier.class.toString());

    /**
     * This zero pattern allows expressions such as
     *  0 or 0. or 0.0 or 0.000 and so on.
     */
    private static final String ZERO_PATTERN = "0\\.?0*";

    private MapleInterface mapleInterface;
    private MapleListener mapleListener;

    private static final double INTERRUPTER_THRESHOLD = 50;

    MapleSimplifier( MapleInterface mapleInterface ){
        this.mapleInterface = mapleInterface;
        this.mapleListener = mapleInterface.getUniqueMapleListener();
        //this.mapleListener.activateAutoInterrupt( INTERRUPTER_THRESHOLD );
    }

    /**
     * This method takes two maple expressions and returns true when both expression
     * are symbolically the same. To verify this, we use the "simplify" command from
     * Maple. Be aware that both expressions still can be mathematically equivalent
     * even when this method returns false!
     *
     * Be also aware that null inputs always returns false, even when both inputs are null.
     * However, two empty expression such as "" and "" returns true.
     *
     * @param exp1 Maple string of the first expression
     * @param exp2 Maple string of the second expression
     * @return true if both expressions are symbolically equivalent or false otherwise.
     *          If it returns false, both expressions still can be mathematically equivalent!
     * @throws MapleException If the test of equivalence produces an Maple error.
     */
    public boolean isEquivalent( @Nullable String exp1, @Nullable String exp2 )
            throws MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "(" + exp1 + ") - (" + exp2 + ")";
        Algebraic a = simplify( command );
        return isZero(a);
    }

    public Algebraic isMultipleEquivalent( @Nullable String exp1, @Nullable String exp2 )
            throws MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "(" + exp1 + ") / (" + exp2 + ")";
        Algebraic a = simplify( command );
        return a;
    }

    /**
     * This method takes two maple expressions and converts the difference
     * to the specified function before it tries to simplify the difference.
     *
     * It works exactly in the same way as {@link #isEquivalent(String, String)},
     * but converts the difference of {@param exp1} and {@param exp2} before it tries
     * to simplify the new expression.
     *
     * @param exp1 Maple string of the first expression
     * @param exp2 Maple string of the second expression
     * @param conversion Specified the destination of the conversion. For example, "expe" or "hypergeom".
     * @return true if both expressions are symbolically equivalent or false otherwise.
     *          If it returns false, both expressions still can be mathematically equivalent!
     * @throws MapleException If the test of equivalence produces an Maple error.
     */
    public boolean isEquivalentWithConversion(
            @Nullable String exp1,
            @Nullable String exp2,
            @Nonnull String conversion )
            throws MapleException{
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "convert((" + exp1 + ") - (" + exp2 + "),"+ conversion +")";
        Algebraic a = simplify( command );
        return isZero(a);
    }

    public Algebraic isMultipleEquivalentWithConversion(
            @Nullable String exp1,
            @Nullable String exp2,
            @Nonnull String conversion )
            throws MapleException{
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "convert((" + exp1 + ") / (" + exp2 + "),"+ conversion +")";
        return simplify( command );
    }

    public boolean isEquivalentWithExpension(
            @Nullable String exp1,
            @Nullable String exp2,
            @Nullable String conversion
    ) throws MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "expand((" + exp1 + ") - (" + exp2 + ")";
        command += conversion == null ? ")" : "," + conversion + ")";
        Algebraic a = simplify( command );
        return isZero(a);
    }

    public Algebraic isMultipleEquivalentWithExpension(
            @Nullable String exp1,
            @Nullable String exp2,
            @Nullable String conversion
    ) throws MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "expand((" + exp1 + ") / (" + exp2 + ")";
        command += conversion == null ? ")" : "," + conversion + ")";
        return simplify( command );
    }

    /**
     * Simplify given expression. Be aware, the given expression should not
     * end with ';'.
     * @param maple_expr given maple expression, without ';'
     * @return the algebraic object of the result of simplify(maple_expr);
     * @throws MapleException if the given expression cannot be evaluated.
     * @see Algebraic
     */
    public Algebraic simplify( String maple_expr ) throws MapleException {
        String command = "simplify(" + maple_expr + ");";
        LOG.debug("Simplification: " + command);
        mapleListener.timerReset();
        return mapleInterface.evaluateExpression( command );
    }

    public Algebraic numericalMagic( String maple_expr ) throws MapleException {
        String command = "nTest := " + maple_expr + ":";
        command += "nVars := indets(nTest,name) minus {constants}:";
        command += "nVals := [-3/2, -1, -1/2, 0, 1/2, 1, 3/2]:";
        command += "nTestVals := createListInList(nVars,nVals):";
        LOG.debug("NumericalMagic: " + command);
        mapleInterface.evaluateExpression( command );

        command = "NumericalTester(nTest,nTestVals,0.0001,15);";
        LOG.debug("Start numerical test: " + command);
        return mapleInterface.evaluateExpression( command );
    }


    public String advancedNumericalTest(
            String maple_expr,
            String values,
            String specialVariables,
            String valuesSpecialVariables,
            int precision,
            int maxCombinations )
            throws MapleException, IllegalArgumentException {
        String command = buildCommandTestValues(
                maple_expr,
                values,
                specialVariables,
                valuesSpecialVariables,
                maxCombinations);
        LOG.debug("Generate value-variable pairs.");
        LOG.trace("Run: " + command);
        Algebraic nTestValsA = mapleInterface.evaluateExpression( command + NL + "nTestVals;" );
        checkValues(nTestValsA);

        command = "numResults := SpecialNumericalTester(nTest,nTestVals," + precision + ");";
        LOG.debug("Start numerical test.");
        mapleInterface.evaluateExpression( command );

        return "numResults";
    }

    private void checkValues( Algebraic nTestValsA ) throws MapleException, IllegalArgumentException {
        if (nTestValsA.isNULL()) {
            if ( checkNumericalNTest() ){
                // in this case, numResults is Null but nTest is numerical
                // continue normal work by reset numResults to an empty array
                mapleInterface.evaluateExpression( "nTestVals := [];" );
                return;
            }
            throw new IllegalArgumentException("There are no valid test values.");
        }

        if ( nTestValsA instanceof List ){
            if ( ((List) nTestValsA).length() <= 0 ){
                if (checkNumericalNTest()){
                    mapleInterface.evaluateExpression( "nTestVals := [];" );
                    return;
                } // else throw an exception
                throw new IllegalArgumentException("There are no valid test values.");
            }
        }
    }

    private boolean checkNumericalNTest() throws MapleException {
        Algebraic numericalCheck = mapleInterface.evaluateExpression("nTest;");
        return numericalCheck instanceof Numeric;
    }

    private String buildCommandTestValues (
            String maple_expr,
            String values,
            String specialVariables,
            String valuesSpecialVariables,
            int maxCombinations )
            throws MapleException, IllegalArgumentException {
        boolean specialVarsSwitch = specialVariables != null && !specialVariables.isEmpty()
                && valuesSpecialVariables != null && !valuesSpecialVariables.isEmpty();

        String command = "nTest := " + maple_expr + ";";
        LOG.debug("Numerical Test Expression: " + command);

        command += "nVars := indets(nTest,name) minus {constants}:" + NL;
        command += "nVals1:= " + values + ":" + NL;

        if ( specialVarsSwitch ) {
            command += "nVals2:= " + valuesSpecialVariables + ":" + NL;
            command += "nVars2:= nVars intersect " + specialVariables + ":" + NL;
            command += "nVars1:= nVars minus nVars2:" + NL;
            command += "inCombis := nops(nVals1)^nops(nVars1) + nops(nVals2)^nops(nVars2):";
        } else {
            command += "inCombis := nops(nVals1)^nops(nVars):";
        }

        LOG.trace("Numerical Preloads: " + command);
        mapleInterface.evaluateExpression( command );

        Algebraic numOfCombis = mapleInterface.evaluateExpression("inCombis;");
        try {
            int i = Integer.parseInt(numOfCombis.toString());
            if ( i >= maxCombinations ) throw new IllegalArgumentException("Too many combinations: " + i);
        } catch ( NumberFormatException e ){
            throw new IllegalArgumentException("Cannot calculate number of combinations!");
        }

        if ( specialVarsSwitch ) {
            command = "nTestVals := [op(createListInList(nVars1, nVals1)), op(createListInList(nVars2, nVals2))]:";
        } else {
            command = "nTestVals := createListInList(nVars,nVals1):";
        }

        return command;
    }

    /**
     *
     * @param expr
     * @return
     */
    public RelationResults holdsRelation( @Nullable String expr ) throws MapleException {
        try {
            String command = "op(1, ToInert(is(" + expr + ")));";
            mapleListener.timerReset();
            Algebraic a = mapleInterface.evaluateExpression( command );
            if ( !(a instanceof MString) ) return RelationResults.ERROR;

            MString ms = (MString) a;
            String s = ms.stringValue();
            if ( s.equals("true") ) return RelationResults.TRUE;
            if ( s.equals("false") ) return RelationResults.FALSE;
            if ( s.equals("FAIL") ) return RelationResults.FAIL;
            return RelationResults.ERROR;
        } catch ( MapleException me ){
            return RelationResults.ERROR;
        }
    }

    /**
     * Checks if the given algebraic object is 0.
     * @param a an algebraic object
     * @return true if the result is 0. False otherwise.
     * @throws MapleException if the given command produces an error in Maple.
     */
    private boolean isZero( Algebraic a ) throws MapleException {
        // null solutions returns false
        if ( a == null || a.isNULL() ) return false;
        // analyze the output string and returns true when it matches "0".
        String solution_str = a.toString();
        return solution_str.trim().matches(ZERO_PATTERN);
    }

    /**
     * If one of them is null, returns true.
     * If none is null but one of them is empty, it returns true
     * when both are empty, otherwise false.
     * Otherwise returns false.
     * @param exp1 string
     * @param exp2 string
     * @return true or false
     */
    private boolean isNullOrEmpty( String exp1, String exp2 ){
        // test if one of the inputs is null
        if ( exp1 == null || exp2 == null ) return true;
        // if one of the expressions is empty, it only returns true when both are empty
        if ( exp1.isEmpty() || exp2.isEmpty() ){
            return !(exp1.isEmpty() && exp2.isEmpty());
        }
        return false;
    }
}
