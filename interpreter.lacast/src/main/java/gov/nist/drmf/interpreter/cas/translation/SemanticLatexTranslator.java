package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.common.*;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.ITranslator;
import gov.nist.drmf.interpreter.common.replacements.ConditionalReplacementRule;
import gov.nist.drmf.interpreter.common.replacements.IReplacementCondition;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * This translation translate semantic LaTeX formula using
 * the math processor language by Abdou Youssef.
 * It based on BNF grammar programmed with JavaCC.
 *
 * It is the top level translation objects. That means
 * you can use {@link #translate(String)} to translate an
 * expression in general. To do so, you have to
 * invoke {@link #init(Path)} before you use this
 * translate method. On the other hand this translation can
 * handle also general PomTaggedExpression to translate.
 * @see PomTaggedExpression
 *
 * @author Andre Greiner-Petter
 */
public class SemanticLatexTranslator extends AbstractTranslator implements ITranslator {
    private static final Logger LOG = LogManager.getLogger(SemanticLatexTranslator.class.getName());

    /**
     * The latex parser
     */
    private PomParser parser;

    private ForwardTranslationProcessConfig config;

    private TranslatedExpression localTranslations;

    /**
     * Creates a forward translator to the specified language.
     * @param to_language the language key
     * @see gov.nist.drmf.interpreter.common.constants.Keys
     */
    public SemanticLatexTranslator( String to_language ){
        this(new ForwardTranslationProcessConfig(to_language));
    }

    /**
     * Creates a forward translator based on a the given config.
     * If the config does not contain a {@link BlueprintMaster} object
     * yet, it will be initialized in the {@link #init(Path)} function
     * and added to the config object.
     *
     * @param config config (with or without BlueprintMaster object)
     * @see BlueprintMaster
     */
    public SemanticLatexTranslator( ForwardTranslationProcessConfig config ) {
        super(null);
        super.setConfig(config);
        this.config = config;
        this.localTranslations = new TranslatedExpression();
    }

    /**
     * Copy constructor
     * @param orig the original translator
     */
    private SemanticLatexTranslator( SemanticLatexTranslator orig ) {
        super(null);
        super.setConfig(orig.getConfig());
        this.config = orig.config;
        this.parser = orig.parser;
        this.localTranslations = new TranslatedExpression();
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    /**
     * Initializes the back end for the translation from semantic LaTeX to
     * a computer algebra system. It loads all translation information
     * from the files in the given path and instantiate the PomParser from
     * Prof. Abdou Youssef.
     *
     * Note, that if the config did not contain a {@link BlueprintMaster}
     * object, it will be initialized and added to the config automatically.
     *
     * @param reference_dir_path the path to the ReferenceData directory.
     *                           You can find the path in
     *                           {@link GlobalPaths#PATH_REFERENCE_DATA}.
     * @throws IOException if it is not possible to read the information
     *                      from the files.
     */
    public void init( Path reference_dir_path ) throws IOException {
        config.init();

        // todo is that necessary?
        MacrosLexicon.init();

        parser = new PomParser(reference_dir_path.toString());
        parser.addLexicons( MacrosLexicon.getDLMFMacroLexicon() );

        if ( config.getLimitParser() == null ) {
            BlueprintMaster bm = new BlueprintMaster(
                    new SemanticLatexTranslator(this) // copy
            );
            bm.init();
            config.setLimitParser(bm);
        }
    }

    /**
     * Translates a given string to the another language with a given label.
     * The label is used to specify additional pre-processing steps.
     *
     * @param expression the input expression (will be pre-processed)
     * @param label specifies the label of the input expression. This trigger additional
     *              pre-processing steps if the label matches defined replacement rules.
     * @return the translated expression
     * @throws TranslationException if an error occurred due translation
     *
     * @see TeXPreProcessor
     * @see ConditionalReplacementRule
     * @see IReplacementCondition
     */
    public String translate( String expression, String label ) throws TranslationException {
        if ( expression == null || expression.isEmpty() ) {
            LOG.warn("Tried to translate an empty expression");
            return "";
        }

        expression = TeXPreProcessor.preProcessingTeX(expression, label);
        LOG.trace("Preprocessed input string. Parsing: " + expression);
        return innerTranslate(expression);
    }

    /**
     * Translates a given string to the another language.
     * @param expression the expression that should get translated.
     * @return the translated expression.
     * @throws TranslationException if an error occurred due translation
     */
    @Override
    public String translate( String expression ) throws TranslationException {
        return translate(expression, null);
    }

    private String innerTranslate( String expression ) throws TranslationException {
        try {
            PomTaggedExpression exp = parser.parse(expression);
            translate(exp); // return value can be ignored here
            if ( !expression.matches("(?:num[UL]|var).*") ){
                LOG.debug("Input:  " + expression);
                LOG.debug("Output: " + getGlobalTranslationList().toString());
            }
            if ( !super.getInfoLogger().isEmpty() && !config.shortOutput() ) {
                LOG.info(super.getInfoLogger().toString());
            }
            return super.getGlobalTranslationList().getTranslatedExpression();
        } catch ( ParseException pe ){
            throw buildException(
                    pe.getMessage(),
                    TranslationExceptionReason.MLP_ERROR,
                    pe
            );
        }
    }

    @Override
    public TranslatedExpression translate( PomTaggedExpression expression ) throws TranslationException {
        reset();
        localTranslations = new TranslatedExpression();
        TranslatedExpression global = super.getGlobalTranslationList();

        parseGeneralExpression(expression, null);

        localTranslations.clear();
        localTranslations.addTranslatedExpression(
                global
        );

        return localTranslations;
    }

    @Override
    public InformationLogger getInfoLogger(){
        return super.getInfoLogger();
    }

    public BlueprintMaster getBlueprintMaster() {
        return config.getLimitParser();
    }
}
