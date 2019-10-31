package gov.nist.drmf.interpreter.cas.translation.components.cases;

import gov.nist.drmf.interpreter.common.exceptions.TranslationException;

/**
 * @author Andre Greiner-Petter
 */
public enum SpecialFunctionsAndDerivatives implements TestCase {
    AIRY(
            "\\AiryAi@{x}",
            "AiryAi(x)",
            "AiryAi[x]"
    ),
    AIRY_PRIME(
            "\\AiryAi'@{x}",
            "subs( temp=x, diff( AiryAi(temp), temp$(1) ) )",
            "D[AiryAi[temp], {temp, 1}]/.temp-> x"
    ),
    AIRY_PRIME_SQUARE(
            "\\AiryAi'@{x}^2",
            "(subs( temp=x, diff( AiryAi(temp), temp$(1) )))^(2)",
            "(D[AiryAi[temp], {temp, 1}]/.temp-> x)^(2)"
    ),
    AIRY_LAGRANGE(
            "\\AiryAi^{(5)}@{x}",
            "subs( temp=x, diff( AiryAi(temp), temp$(5) ) )",
            "D[AiryAi[temp], {temp, 5}]/.temp-> x"
    ),
    AIRY_POWER(
            "\\AiryAi^{5}@{x}",
            "(AiryAi(x))^(5)",
            "(AiryAi[x])^(5)"
    ),
    HURWITZ_ZETA(
            "\\Hurwitzzeta@{s}{a}",
            "Zeta(0, s, a)",
            "HurwitzZeta[s, a]"
    ),
    HURWITZ_ZETA_PRIME(
            "\\Hurwitzzeta'@{0}{a}",
            "subs( temp=0, diff( Zeta(0, temp, a), temp$(1) ) )",
            "D[HurwitzZeta[temp, a], {temp, 1}]/.temp-> 0"
    ),
    EMBEDDED_SPECIAL_FUNCTIONS(
            "\\ln@@{\\EulerGamma@{a}}",
            "ln(GAMMA(a))",
            "Log[Gamma[a]]"
    ),
    LOGARITHM(
            "\\ln@@{2\\pi}",
            "ln(2*pi)",
            "Log[2*\\[Pi]]"
    ),
    HURWITZ_ZETA_LAGRANGE(
            "\\Hurwitzzeta^{(2+3n)}@{s}{a}",
            "subs( temp=s, diff( Zeta(0, temp, a), temp$(2 + 3*n) ) )",
            "D[HurwitzZeta[temp, a], {temp, 2+3*n}]/.temp-> s"
    ),
    HURWITZ_ZETA_LAGRANGE_PRIME_10(
            "\\Hurwitzzeta''''''''''@{s}{a}",
            "subs( temp=s, diff( Zeta(0, temp, a), temp$(10) ) )",
            "D[HurwitzZeta[temp, a], {temp, 10}]/.temp-> s"
    ),
    MODIFIED_BESSEL_K(
            "\\modBesselKimag{\\nu}@{x}",
            "BesselK(I*(nu), x)",
            "BesselK[I*\\[Nu], x]"
    ),
    MODIFIED_BESSEL_K_PRIME(
            "\\modBesselKimag{\\nu}'@{x}",
            "subs( temp=x, diff( BesselK(I*(nu), temp), temp$(1) ) )",
            "D[BesselK[\\[Nu], temp], {temp, 1}]/.temp-> x"
    ),
    MODIFIED_BESSEL_K_LAGRANGE(
            "\\modBesselKimag{\\nu}^{(5^3)}@{x}",
            "subs( temp=x, diff( BesselK(I*(nu), temp), temp$((5)^(3)) ) )",
            "D[BesselK[\\[Nu], temp], {temp, 5^3}]/.temp-> x"
    ),
    HYPER_F(
            "\\hyperF@@@{a}{b}{c}{z}",
            "hypergeom([a, b], [c], z)",
            "Hypergeometric2F1[a, b, c, z]"
    ),
    HYPER_F_PRIME(
            "\\hyperF'@@@{a}{b}{c}{z}",
            "subs( temp=z, diff( hypergeom([a, b], [c], temp), temp$(1) ) )",
            "D[Hypergeometric2F1[a, b, c, temp], {temp, 1}]/.temp-> z"
    ),
    HYPER_F_LAGRANGE(
            "\\hyperF^{(2n)}@@@{a}{b}{c}{z}",
            "subs( temp=z, diff( hypergeom([a, b], [c], temp), temp$(2*n) ) )",
            "D[Hypergeometric2F1[a, b, c, temp], {temp, 2*n}]/.temp-> z"
    ),
    HYPER_F_PRIME_COS_ARGUMENT(
            "\\hyperF'@@@{a}{b}{c}{\\cos@@{2z}}",
            "subs( temp=cos(2*z), diff( hypergeom([a, b], [c], temp), temp$(1) ) )",
            "D[Hypergeometric2F1[a, b, c, temp], {temp, 1}]/.temp-> Cos[2*z]"
    ),
    HYPER_F_PRIME_COMPLEX_ARGUMENT(
            "\\hyperF'@@@{a}{b}{\\ln@@{c}+2}{\\cos@@{2z}}",
            "subs( temp=cos(2*z), diff( hypergeom([a, b], [ln(c)+ 2], temp), temp$(1) ) )",
            "D[Hypergeometric2F1[a, b, Log[c]+2, temp], {temp, 1}]/.temp-> Cos[2*z]"
    ),
    FERRERS_P(
            "\\FerrersP[\\mu]{\\nu}@{x}",
            "LegendreP(nu, mu, x)",
            "LegendreP[\\[Nu], \\[Mu], x]"
    ),
    FERRERS_P_PRIME(
            "\\FerrersP[\\mu]{\\nu}'@{x}",
            "subs( temp=x, diff( LegendreP(nu, mu, temp), temp$(1) ) )",
            "D[LegendreP[\\[Nu], \\[Mu], temp], {temp, 1}]/.temp-> x"
    ),
    FERRERS_P_LAGRANGE(
            "\\FerrersP[\\mu]{\\nu}^{(6)}@{x}",
            "subs( temp=x, diff( LegendreP(nu, mu, temp), temp$(6) ) )",
            "D[LegendreP[\\[Nu], \\[Mu], temp], {temp, 6}]/.temp-> x"
    ),
    POCHHAMMER(
            "\\pochhammer{a}{n}",
            "pochhammer(a, n)",
            "Pochhammer[a, n]"
    ),
    AIRY_AI_BI_PRIME(
            "\\AiryAi'@{\\AiryBi@{x}}",
            "subs( temp=AiryBi(x), diff( AiryAi(temp), temp$(1) ) )",
            "D[AiryAi[temp], {temp, 1}]/.temp-> AiryBi[x]"
    ),
    AIRY_AI_BI_INNER_PRIME(
            "\\AiryAi@{\\AiryBi''@{x}}",
            "AiryAi(subs( temp=x, diff( AiryBi(temp), temp$(2) ) ))",
            "AiryAi[D[AiryBi[temp], {temp, 2}]/.temp-> x]"
    ),
    AIRY_AI_BI_INNER_OUTER_PRIME(
            "\\AiryAi'''@{\\AiryBi''@{x}}",
            "subs( temp=subs( temp=x, diff( AiryBi(temp), temp$(2) ) ), diff( AiryAi(temp), temp$(3) ) )",
            "D[AiryAi[temp], {temp, 3}]/.temp-> D[AiryBi[temp], {temp, 2}]/.temp-> x"
    ),
    AIRY_AI_BI_INNER_PRIME_OUTER_LAGRANGE(
            "\\AiryAi^{(n)}@{\\AiryBi'''@{x}}",
            "subs( temp=subs( temp=x, diff( AiryBi(temp), temp$(3) ) ), diff( AiryAi(temp), temp$(n) ) )",
            "D[AiryAi[temp], {temp, n}]/.temp-> D[AiryBi[temp], {temp, 3}]/.temp-> x"
    ),
    AIRY_AI_BI_INNER_LAGRANGE_OUTER_PRIME(
            "\\AiryAi'''@{\\AiryBi^{(n)}@{x}}",
            "subs( temp=subs( temp=x, diff( AiryBi(temp), temp$(n) ) ), diff( AiryAi(temp), temp$(3) ) )",
            "D[AiryAi[temp], {temp, 3}]/.temp-> D[AiryBi[temp], {temp, n}]/.temp-> x"
    ),
    AIRY_AI_BI_INNER_LAGRANGE_OUTER_PRIME_NESTED_ARG(
            "\\AiryAi'''@{\\AiryBi^{(n)}@{\\EulerGamma@{z}}}",
            "subs( temp=subs( temp=GAMMA(z), diff( AiryBi(temp), temp$(n) ) ), diff( AiryAi(temp), temp$(3) ) )",
            "D[AiryAi[temp], {temp, 3}]/.temp-> D[AiryBi[temp], {temp, n}]/.temp-> Gamma[z]"
    ),
    AIRY_AI_NESTED_DIFFS(
            "\\AiryAi'''@{\\AiryBi^{(n)}@{\\EulerGamma'@{z}}}",
            "subs( temp=subs( temp=subs( temp=z, diff( GAMMA(temp), temp$(1) ) ), diff( AiryBi(temp), temp$(n) ) ), diff( AiryAi(temp), temp$(3) ) )",
            "D[AiryAi[temp], {temp, 3}]/.temp-> D[AiryBi[temp], {temp, n}]/.temp-> D[Gamma[temp], {temp, 1}]/.temp-> z"
    ),
    WRONSKIAN_AI(
            "\\Wronskian@{\\AiryAi@{z}, \\AiryBi@{z}}",
            "(AiryAi(z))*diff(AiryBi(z), z)-diff(AiryAi(z), z)*(AiryBi(z))",
            "Wronskian[{AiryAi[z], AiryBi[z]}, z]"
    ),
    WRONSKIAN_OLVER(
            "\\Wronskian@{\\OlverconfhyperM@{a}{b}{z}, z^{1-b} \\OlverconfhyperM@{a-b+1}{2-b}{z}}",
            "(KummerM(a, b, z)/GAMMA(b))*diff((z)^(1 - b)* KummerM(a - b + 1, 2 - b, z)/GAMMA(2 - b), z)-diff(KummerM(a, b, z)/GAMMA(b), z)*((z)^(1 - b)* KummerM(a - b + 1, 2 - b, z)/GAMMA(2 - b))",
            "Wronskian[{Hypergeometric1F1Regularized[a, b, z], (z)^(1 - b)*Hypergeometric1F1Regularized[a - b + 1, 2 - b, z]}, z]"
    ),
    WRONSKIAN_OLVER_2(
            "\\Wronskian@{z^{1-b} \\OlverconfhyperM@{a-b+1}{2-b}{z}, \\KummerconfhyperU@{a}{b}{z}}",
            "((z)^(1 - b)* KummerM(a - b + 1, 2 - b, z)/GAMMA(2 - b))*diff(KummerU(a, b, z), z)-diff((z)^(1 - b)* KummerM(a - b + 1, 2 - b, z)/GAMMA(2 - b), z)*(KummerU(a, b, z))",
            "Wronskian[{(z)^(1 - b)*Hypergeometric1F1Regularized[a - b + 1, 2 - b, z], HypergeometricU[a, b, z]}, z]"
    );

    private String tex, maple, mathematica;

    SpecialFunctionsAndDerivatives(String tex, String maple, String mathematica) {
        this.tex = tex;
        this.maple = maple;
        this.mathematica = mathematica;
    }

    @Override
    public String getTitle() {
        return name();
    }

    @Override
    public String getTeX() {
        return tex;
    }

    @Override
    public String getMaple() {
        return maple;
    }

    @Override
    public String getMathematica() {
        return mathematica;
    }
}
