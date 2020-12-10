package gov.nist.drmf.interpreter.generic;

import gov.nist.drmf.interpreter.generic.elasticsearch.AssumeElasticsearchAvailability;
import gov.nist.drmf.interpreter.generic.mlp.struct.MOIPresentations;
import gov.nist.drmf.interpreter.generic.pojo.FormulaDefiniens;
import gov.nist.drmf.interpreter.generic.pojo.SemanticEnhancedDocument;
import mlp.ParseException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Andre Greiner-Petter
 */
@AssumeElasticsearchAvailability
public class GenericLatexSemanticEnhancerTest {
    @Test
    void simpleWikitextTest() throws IOException {
        String text = getResourceContent("mlp/simpleWikitest.xml");
        GenericLatexSemanticEnhancer enhancer = new GenericLatexSemanticEnhancer();
        SemanticEnhancedDocument semanticDocument = enhancer.getSemanticEnhancedDocument(text);
        List<MOIPresentations> moiPresentationsList = semanticDocument.getFormulae();

        MOIPresentations jacobi = moiPresentationsList.stream()
                .filter( m -> m.getGenericLatex().equals("P_n^{(\\alpha, \\beta)} (x)") )
                .findFirst()
                .orElse(null);

        assertNotNull(jacobi);
        assertEquals("\\JacobipolyP{\\alpha}{\\beta}{n}@{x}", jacobi.getSemanticLatex());
        assertEquals("JacobiP(n, alpha, beta, x)", jacobi.getCasRepresentation("Maple"));
        assertEquals("JacobiP[n, \\[Alpha], \\[Beta], x]", jacobi.getCasRepresentation("Mathematica"));

        List<FormulaDefiniens> definitions = jacobi.getDefiniens();
        long hits = definitions.stream()
                .filter( d ->
                        d.getDefinition().equals("Jacobi polynomial") ||
                                d.getDefinition().equals("Carl Gustav Jacob Jacobi")
                )
                .count();
        assertEquals(2, hits);

        assertEquals(0, jacobi.getIngoingNodes().size());
        assertEquals(0, jacobi.getOutgoingNodes().size());
    }

    @Test
    void annotateSingleFormulaTest() throws ParseException {
        String context = "The Gamma function <math>\\Gamma(z)</math> and " +
                "the pochhammer symbol <math>(a)_n</math> are often used together.";

        String includedMath = "\\Gamma(z)";
        String notIncludedMath = "\\Gamma( (\\alpha+1)_n )";

        GenericLatexSemanticEnhancer enhancer = new GenericLatexSemanticEnhancer();
        MOIPresentations gammaMOI = enhancer.enhanceGenericLaTeX(context, includedMath);
        assertNotNull(gammaMOI);
        assertEquals("\\Gamma(z)", gammaMOI.getGenericLatex());
        assertEquals("\\EulerGamma@{z}", gammaMOI.getSemanticLatex());
        assertEquals("GAMMA(z)", gammaMOI.getCasRepresentation("Maple"));
        assertEquals("Gamma[z]", gammaMOI.getCasRepresentation("Mathematica"));

        MOIPresentations gammaCompositionMOI = enhancer.enhanceGenericLaTeX(context, notIncludedMath);
        assertNotNull(gammaCompositionMOI);
        assertEquals("\\Gamma( (\\alpha+1)_n )", gammaCompositionMOI.getGenericLatex());
        assertEquals("\\EulerGamma@{\\Pochhammersym{\\alpha + 1}{n}}", gammaCompositionMOI.getSemanticLatex());
        assertEquals("GAMMA(pochhammer(alpha + 1, n))", gammaCompositionMOI.getCasRepresentation("Maple"));
        assertEquals("Gamma[Pochhammer[\\[Alpha]+ 1, n]]", gammaCompositionMOI.getCasRepresentation("Mathematica"));
    }

    private String getResourceContent(String resourceFilename) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(resourceFilename), StandardCharsets.UTF_8);
    }
}
