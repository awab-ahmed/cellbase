package org.opencb.cellbase.core.variant.annotation.hgvs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.util.ajax.JSON;
import org.opencb.biodata.models.core.Gene;
import org.opencb.biodata.models.core.Transcript;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.commons.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * All test cases are:
 *
 * grch38
 * ensembl v90
 * tested with cellbase 4.8
 */
public class HgvsProteinCalculatorTest {

    private ObjectMapper jsonObjectMapper;
    List<Gene> geneList;

    public HgvsProteinCalculatorTest() throws IOException {

    }

    @Before
    public void setUp() throws IOException {

        jsonObjectMapper = new ObjectMapper();
        jsonObjectMapper.configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true);
        jsonObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        geneList = loadGenes(Paths.get(getClass().getResource("/hgvs/gene.test.json.gz").getFile()));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// INSERTIONS //////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void testInsertion() throws Exception {
//9:83970290:-:TGA        9       83970290        -       TGA     indel   ENSP00000365439 p.Lys411_Gln412insSer   p.Lys411_Gln412insSer
        Gene gene = getGene("ENSG00000165119");
        Transcript transcript = getTranscript(gene, "ENST00000376263");
        Variant variant = new Variant("9",
                83970290,
                "-",
                "TGA");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Lys411_Gln412insSer", hgvsProtein.getHgvs());
    }

    @Test
    public void testDup() throws Exception {
//2:51027601:-:CCTCGCCCT  2       51027601        -       CCTCGCCCT       indel   ENSP00000490017 p.Glu75_Glu77dup        p.Gly78Ter      vep_dup_cb_ter
        Gene gene = getGene("ENSG00000179915");
        Transcript transcript = getTranscript(gene, "ENST00000636066");
        Variant variant = new Variant("2",
                51027601,
                "-",
                "CCTCGCCCT");
        // Reverse strand:  AGGGCGAGG
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Glu75_Glu77dup", hgvsProtein.getHgvs());
    }

    @Test
    public void testInsertionPositiveStrandPhase0() throws Exception {
        // 17:18173905:-:A  indel   ENSP00000408800 p.Leu757AlafsTer79      p.Leu757fs      fs_shorthand_same_pos
        // phase 0
        // positive strand
        // confirmed start
        Gene gene = getGene("ENSG00000091536");
        Transcript transcript = getTranscript(gene, "ENST00000418233");
        Variant variant = new Variant("17",
                18173905,
                "-",
                "A");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Leu757AlafsTer79", hgvsProtein.getHgvs());
    }

    @Test
    public void testInsertionPositiveStrandPhase0Nonsense() throws Exception {
        // Issue #5 - NonsenseReportedAsFrameShift
        // positive strand
        // phase 0, confirmed start
        //        1:236717940:-:T 1       236717940       -       T       indel   ENSP00000443495 p.Lys71Ter      p.Lys71fs       nonsense_as_fs_same_pos
        Gene gene = getGene("ENSG00000077522");
        Transcript transcript = getTranscript(gene, "ENST00000542672");
        Variant variant = new Variant("1",
                236717940,
                "-",
                "T");
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Lys71Ter", predictor.calculate().getHgvs());
    }

    @Test
    public void testInsertionPositiveStrandPhase0FS() throws Exception {
        // Issue #6 - FrameShiftReportedAsDup
        // positive strand
        // phase 0, confirmed start (no flags)
        //4:102582930:-:T 4       102582930       -       T       indel   ENSP00000424790 p.Ser301PhefsTer7       p.Phe300dup     fs_as_dup
        Gene gene = getGene("ENSG00000109320");
        Transcript transcript = getTranscript(gene, "ENST00000505458");
        Variant variant = new Variant("4",
                102582930,
                "-",
                "T");
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Ser301PhefsTer7", predictor.calculate().getHgvs());
    }

    @Test
    public void testInsertionPositiveStrandPhase0LargeAlternate() throws Exception {
        // fs_shorthand_same_pos
        // positive strand
        // phase 0
        // confirmed start (no flags)
        // 32102   4:154744350:-:CTTCATGGAAGAACCC  4       154744350       -       CTTCATGGAAGAACCC        indel   ENSP00000426761 p.Val9PhefsTer23        p.Val9fs        fs_shorthand_same_pos
        Gene gene = getGene("ENSG00000121207");
        Transcript transcript = getTranscript(gene, "ENST00000507827");
        Variant variant = new Variant("4",
                154744350,
                "-",
                "CTTCATGGAAGAACCC");
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Val9PhefsTer23", predictor.calculate().getHgvs());
    }

    @Test
    public void testInsertionPositiveStrandPhase2() throws Exception {
        // fs_shorthand_diff_pos
        // positive strand
        // phase 2
        //19:11111569:-:GGGT      19      11111569        -       GGGT    indel   ENSP00000453513 p.Tyr202TrpfsTer7       p.Gly201TrpfsTer41      fs_shorthand_diff_pos
        Gene gene = getGene("ENSG00000130164");
        Transcript transcript = getTranscript(gene, "ENST00000560467");
        Variant variant = new Variant("19",
                11111569,
                "-",
                "GGGT");
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Tyr202TrpfsTer7", predictor.calculate().getHgvs());

        //19:11111569:-:GGGT      19      11111569        -       GGGT    indel   ENSP00000252444 p.Tyr460TrpfsTer7       p.Gly459TrpfsTer41      fs_shorthand_diff_pos
        transcript = getTranscript(gene, "ENST00000252444");
        variant = new Variant("19",
                11111569,
                "-",
                "GGGT");
        predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Tyr460TrpfsTer7", predictor.calculate().getHgvs());
    }

    @Test
    public void testInsReverseStrandPhase2() throws Exception {
        // fs_shorthand_diff_pos
        // reverse strand
        // phase 2
        // unconfirmed start
        //16:2092152:-:GTGT    16    2092152    -    GTGT    indel    ENSP00000461391    p.Cys8HisfsTer?    p.Thr7GlnfsTer176
        Gene gene = getGene("ENSG00000008710");
        Transcript transcript = getTranscript(gene, "ENST00000561668");
        Variant variant = new Variant("16",
                2092152,
                "-",
                "GTGT");    // ACAC
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Cys8HisfsTer?", predictor.calculate().getHgvs());
    }

    @Test
    public void testInsertionInframe() throws Exception {
        Gene gene = getGene("ENSG00000077522");
        Transcript transcript = getTranscript(gene, "ENST00000542672");
        Variant variant = new Variant("1",
                236747758,
                "-",
                "AAAAAGAGC");
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Lys499_Arg500insLysLysSer", hgvsProtein.getHgvs());

        assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("ENSP00000443495"));
    }

    // -------------------- negative strand --------------------------------------

    @Test
    public void testInsertionNegativeStrandPhase0() throws Exception {
        // negative strand, phase 0
        // unconfirmed start
        // 15110   16:2106127:-:T  16      2106127 -       T       indel   ENSP00000457132 p.Asp786GlyfsTer38      p.Gln785fs      fs_shorthand_diff_pos

        // cdna start Position = 1, cds = 2355, cdnaVariantIndex = 2355
        // GTGGTGGTGCAG[t]GACCAGCTGGGAGCCGCTGTG

        Gene gene = getGene("ENSG00000008710");
        Transcript transcript = getTranscript(gene, "ENST00000487932");
        Variant variant = new Variant("16",
                2106127,
                "-",
                "T");
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Asp786GlyfsTer38", predictor.calculate().getHgvs());
    }

    @Test
    public void testInsertionNegativeStrandPhase2UnconfirmedStart() throws Exception {
        //         14086   X:109669061:-:T X       109669061       -       T       indel   ENSP00000423539 p.Tyr19IlefsTer2        p.Gly18fs
        // phase II
        // negative strand
        // unconfirmed start, protein sequence starts with X
        Gene gene = getGene("ENSG00000068366");
        Transcript transcript = getTranscript(gene, "ENST00000514500");
        Variant variant = new Variant("X",
                109669061,
                "-",
                "T");

        String proteinSequence =
                "XTLFKIGYDYKLEQIKKGYDAPLCNLLLFKKVKALLGGNVRMMLSGGAPLSPQTHRFMNVCFCCPIGQGYGLTESCGAGTVTEVTDYTTGRVGAPLICCEIKLKDWQEGGYTINDKPNPRGEIVIGGQNISMGYFKNEEKTAEDYSVDENGQRNLG";

        String reference =
                "AAACTCTGTTCAAGATAGGGTATGATTACAAATTGGAACAGATCAAAAAGGGATATGATGCACCTCTTTGCAATCTGTTACTGTTTAAAAAGGTCAAGGCCCTGCTGGGAGGGAATGTCCGCATGATGCTGTCTGGAGGGGCCCCGCTATCTCCTCAGACACACCGATTCATGAATGTCTGCTTCTGCTGCCCAATTGGCCAGGGTTATGGACTGACAGAATCATGTGGTGCTGGGACAGTTACTGAAGTAACTGACTATACTACTGGCAGAGTTGGAGCACCTCTTATTTGCTGTGAAATTAAGCTAAAAGACTGGCAAGAAGGCGGTTATACAATTAATGACAAGCCAAACCCCAGAGGTGAAATCGTAATTGGTGGACAGAACATCTCCATGGGATATTTTAAAAATGAAGAGAAAACAGCAGAAGATTATTCTGTGGATGAAAATGGACAAAGGAACTTGGGTTGATATCTGCAATAATCCTGCTATGGAAGCTGAAATACTGAAAGAAATTCGAGAAGCTGCAAATGCCATGAAATTGGAGCGATTTGAAATTCCAATCAAGGTTCGATTAAGCCCAGAGCCATGGACCCCTGAAACTGGTTTGGTAACTGATGCTTTCAAACTGAAAAGGAAGGAGCTGAGGAACCATTACCTCAAAGACATTGAACGAATGTATGGGGGCAAATAAAAT";

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Tyr19IlefsTer2", predictor.calculate().getHgvs());
    }

    @Test
    public void testInsertionNegativeStrandPhase2ConfirmedStart() throws Exception {
        // phase II
        // negative strand
        // confirmed start (no annotation flags)
        //        14085   X:109669061:-:T X       109669061       -       T       indel   ENSP00000262835 p.Tyr374IlefsTer2       p.Tyr374fs      fs_shorthand_same_pos

        String proteinSequence =
                "MAKRIKAKPTSDKPGSPYRSVTHFDSLAVIDIPGADTLDKLFDHAVSKFGKKDSLGTREILSEENEMQPNGKVFKKLILGNYKWMNYLEVNRRVNNFGSGLTALGLKPKNTIAIFCETRAEWMIAAQTCFKYNFPLVTLYATLGKEAVVHGLNESEASYLITSVELLESKLKTALLDISCVKHIIYVDNKAINKAEYPEGFEIHSMQSVEELGSNPENLGIPPSRPTPSDMAIVMYTSGSTGRPKGVMMHHSNLIAGMTGQCERIPGLGPKDTYIGYLPLAHVLELTAEISCFTYGCRIGYSSPLTLSDQSSKIKKGSKGDCTVLKPTLMAAVPEIMDRIYKNVMSKVQEMNYIQKTLFKIGYDYKLEQIKKGYDAPLCNLLLFKKVKALLGGNVRMMLSGGAPLSPQTHRFMNVCFCCPIGQGYGLTESCGAGTVTEVTDYTTGRVGAPLICCEIKLKDWQEGGYTINDKPNPRGEIVIGGQNISMGYFKNEEKTAEDYSVDENGQRWFCTGDIGEFHPDGCLQIIDRKKDLVKLQAGEYVSLGKVEAALKNCPLIDNICAFAKSDQSYVISFVVPNQKRLTLLAQQKGVEGTWVDICNNPAMEAEILKEIREAANAMKLERFEIPIKVRLSPEPWTPETGLVTDAFKLKRKELRNHYLKDIERMYGGK";
        Gene gene = getGene("ENSG00000068366");
        Transcript transcript = getTranscript(gene, "ENST00000348502");
        Variant variant = new Variant("X",
                109669061,
                "-",
                "T");
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Tyr374IlefsTer2", predictor.calculate().getHgvs());
    }

    //@Test
    public void testDuplicationAsNonsense() throws Exception {
        // Issue #9 Dups reported as nonsense
        // positive strand
        // 12271   X:71137733:-:CTC        X       71137733        -       CTC     indel   ENSP00000404373 p.Pro167dup     p.Tyr168Ter     vep_dup_cb_ter
        Gene gene = getGene("ENSG00000184634");
        Transcript transcript = getTranscript(gene, "ENST00000444034");
        Variant variant = new Variant("X",
                71137733,
                "-",
                "CTC");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Pro167dup", predictor.calculate().getHgvs());
    }

    /////////////////////////////////////
    ///////////// DELETIONS /////////////
    /////////////////////////////////////

    @Test
    public void testDeletionInframe() throws Exception {
        Gene gene = getGene("ENSG00000165119");
        Transcript transcript = getTranscript(gene, "ENST00000481820");
        Variant variant = new Variant("9",
                83970774,
                "CCA",
                "-");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Gly29del", hgvsProtein.getHgvs());

        assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("ENSP00000473957"));
    }

    @Test
    public void testDeletionSynonymousFS() throws Exception {
        // Issue #3
        //2701    6:112061056:G:- 6       112061056       G       -       indel   ENSP00000357653 p.Ser57GlnfsTer27       p.Val56fs       fs_shorthand_diff_pos
        // change at del position 56 is synonymous GTG (val) > GTT (Val)
        // Therefore first affected aa is Ser(57)
        Gene gene = getGene("ENSG00000112761");
        Transcript transcript = getTranscript(gene, "ENST00000368664");
        Variant variant = new Variant("6",
                112061056,
                "G",
                "-");
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Ser57GlnfsTer27", predictor.calculate().getHgvs());
    }

    @Test
    public void testDeletionFS() throws Exception {
        // 2:47822224:T:-  2       47822224        T       -       indel   ENSP00000385398 p.Ile482PhefsTer6       p.Ile482fs
        // negative strand
        Gene gene = getGene("ENSG00000138081");
        Transcript transcript = getTranscript(gene, "ENST00000402508");
        Variant variant = new Variant("2",
                47822224,
                "T",
                "-");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Ile482PhefsTer6", predictor.calculate().getHgvs());
    }

   @Test
    public void testDeletion0() throws Exception {
        // Issue #4
        // 6:121447732:TTC:-  indel   ENSP00000282561 p.Ser297del     p.Ser297_Cys298del      del_cb_aa_1_out
        Gene gene = getGene("ENSG00000152661");
        Transcript transcript = getTranscript(gene, "ENST00000282561");
        Variant variant = new Variant("6",
                121447732,
                "TTC",
                "-");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Ser297del", predictor.calculate().getHgvs());
    }

    @Test
    public void testDeletion1() throws Exception {
        Gene gene = getGene("ENSG00000221859");
        Transcript transcript = getTranscript(gene, "ENST00000380095");
        Variant variant = new Variant("21",
                46057614,
                "TGC",
                "-");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Cys95del", predictor.calculate().getHgvs());
    }

    @Test
    public void testDeletion2() throws Exception {
        // Issue #4
        // shift
        // 20291   14:104714676:GAGGAC:-   14      104714676       GAGGAC  -       indel   ENSP00000376410 p.Asp1175_Glu1176del    p.Glu1176_Asp1178del    del_cb_aa_more_than_1_out
        Gene gene = getGene("ENSG00000203485");
        Transcript transcript = getTranscript(gene, "ENST00000392634");
        Variant variant = new Variant("14",
                104714676,
                "GAGGAC",
                "-");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Asp1175_Glu1176del", predictor.calculate().getHgvs());
    }

    @Test
    public void testDeletion3() throws Exception {
        // Issue #4
        // off by one
        // 1861    11:75566844:CAAGCG:-    11      75566844        CAAGCG  -       indel   ENSP00000435452 p.Lys166_Arg167del      p.Lys166_Ser168del      del_cb_aa_1_out
        Gene gene = getGene("ENSG00000149257");
        Transcript transcript = getTranscript(gene, "ENST00000525611");
        Variant variant = new Variant("11",
                75566844,
                "CAAGCG",
                "-");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
//        Assert.assertEquals("ENSP00000435452:p.Lys166_Arg167del", predictor.calculate().getHgvs());
        Assert.assertEquals("p.Lys166_Arg167del", predictor.calculate().getHgvs());
    }



    /////////////////////////////////////
    ///////////// SNV ///////////////////
    /////////////////////////////////////

    // TODO add - 14:20447645:C:T bad reference codon


    @Test
    public void testArrayOutOfBounds() throws Exception {
        Gene gene = getGene("ENSG00000162688");
        Transcript transcript = getTranscript(gene, "ENST00000361302");
        Variant variant = new Variant("A",
                99884391,
                "G",
                "A");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertEquals("Don't know!", hgvsProtein.getHgvs());
        assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("ENSP00000355106"));
    }

    @Test
    public void testSnvOffByOne() throws Exception {
        Gene gene = getGene("ENSG00000162688");
        Transcript transcript = getTranscript(gene, "ENST00000370165");
        Variant variant = new Variant("A",
                99884391,
                "G",
                "A");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Asn829Ser", hgvsProtein.getHgvs());

        gene = getGene("ENSG00000130164");
        transcript = getTranscript(gene, "ENST00000252444");
        variant = new Variant("19",
                11110721,
                "A",
                "G");

        predictor = new HgvsProteinCalculator(variant, transcript);
        hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Glu422Gly", hgvsProtein.getHgvs());
        assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("ENSP00000252444"));


        gene = getGene("ENSG00000144028");
        transcript = getTranscript(gene, "ENST00000323853");
        variant = new Variant("2",
                96291454,
                "C",
                "T");

        predictor = new HgvsProteinCalculator(variant, transcript);
        hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Ala787Thr", hgvsProtein.getHgvs());
        assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("ENSP00000317123"));

        gene = getGene("ENSG00000160285");
        transcript = getTranscript(gene, "ENST00000397728");
        variant = new Variant("22",
                46228567,
                "G",
                "A");

        predictor = new HgvsProteinCalculator(variant, transcript);
        hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Thr16Ile", hgvsProtein.getHgvs());
        assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("ENSP00000380837"));
    }

    @Test
    public void testSnv() throws Exception {
        // Issue #2 missing protein example
        // phase 0
        // positive strand
      //  61      3:155143536:G:A 3       155143536       G       A       snv     ENSP00000420389 p.Val428Met             cb_empty
        Gene gene = getGene("ENSG00000196549");
        Transcript transcript = getTranscript(gene, "ENST00000492661");
        Variant variant = new Variant("3",
                155143536,
                "G",
                "A");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Val428Met", hgvsProtein.getHgvs());
        assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("ENSP00000420389", "P08473"));
    }

    @Test
    public void testSilentSNV() throws Exception {
        // 288     11:77179045:G:A 11      77179045        G       A       snv     ENSP00000386635 p.Arg750=       p.Arg750=
        Gene gene = getGene("ENSG00000137474");
        Transcript transcript = getTranscript(gene, "ENST00000409619");
        Variant variant = new Variant("11",
                77179045,
                "G",
                "A");
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Arg750=", hgvsProtein.getHgvs());

        assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("ENSP00000386635"));

    }

    @Test
    public void testStopLoss() throws Exception {
        // Issue #7 - stop loss reported as missense

        // phase 0
        // positive strand
        //	3:183650359:G:A	3	183650359	G	A	snv	ENSP00000419120	p.Met1?	p.Met1Ile	start_loss_as_missense
        Gene gene = getGene("ENSG00000114796");
        Transcript transcript = getTranscript(gene, "ENST00000473045");
        Variant variant = new Variant("3",
                183650359,
                "G",
                "A");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Met1?", hgvsProtein.getHgvs());

        assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("ENSP00000419120"));
        // uniprot trembl name, not swissprot
        //assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("C9JXR5"));

        // negative strand
        // phase 0
//2731    12:132687314:A:T        12      132687314       A       T       snv     ENSP00000442578 p.Met1? p.Met1Lys       q
        gene = getGene("ENSG00000177084");
        transcript = getTranscript(gene, "ENST00000537064");
        variant = new Variant("12",
                132687314,
                "A",
                "T");

        predictor = new HgvsProteinCalculator(variant, transcript);
        hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Met1?", hgvsProtein.getHgvs());

        assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("ENSP00000442578"));
    }

    @Test
    public void testSnv1() throws Exception {
        // 288     11:77179045:G:A 11      77179045        G       A       snv     ENSP00000386635 p.Arg750=       p.Arg750=
        Gene gene = getGene("ENSG00000137474");
        Transcript transcript = getTranscript(gene, "ENST00000409619");
        Variant variant = new Variant("11",
                77179045,
                "G",
                "A");
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertEquals("p.Arg750=", hgvsProtein.getHgvs());

        assertThat(hgvsProtein.getIds(), CoreMatchers.hasItems("ENSP00000386635"));
    }

    @Test
    public void testSnvSynonymous() throws Exception {
        Gene gene = getGene("ENSG00000221859");
        Transcript transcript = getTranscript(gene, "ENST00000380095");
        Variant variant = new Variant("21",
                44637453,
                "C",
                "T");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Ala12=", predictor.calculate().getHgvs());
    }

    @Test
    public void testSnvMissense() throws Exception {
        Gene gene = getGene("ENSG00000221859");
        Transcript transcript = getTranscript(gene, "ENST00000380095");
        Variant variant = new Variant("21",
                44637501,
                "G",
                "C");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Glu28Asp", predictor.calculate().getHgvs());
    }

    @Test
    public void testSnvStopGained() throws Exception {
        Gene gene = getGene("ENSG00000221859");
        Transcript transcript = getTranscript(gene, "ENST00000380095");
        Variant variant = new Variant("21",
                44637643,
                "C",
                "T");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Gln76Ter", predictor.calculate().getHgvs());
    }


    // ------------------- from HgvsCalculator --------------------------------
//    @Test
//    public void testSNVBadReferenceSequence() throws Exception {
//
//        // Weird character ("U") in protein sequence (e.g. ENST00000525566/ENSP00000434516, position 648) must not
//        // return any protein HGVS description
//        Gene gene = getGene("ENSG00000198431");
//        Transcript transcript = getTranscript(gene, "ENST00000525566");
//        Variant variant = new Variant("12",
//                104742191,
//                "T",
//                "C");
//        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
//        HgvsProtein hgvsProtein = predictor.calculate();
//        Assert.assertNull(hgvsProtein);
//
//    }

    @Test
    public void testSnvBadAltAA() throws Exception {
        // Invalid alternte nt - no prot hgvs should be returned
        Gene gene = getGene("ENSG00000137474");
        Transcript transcript = getTranscript(gene, "ENST00000409619");
        Variant variant = new Variant("11",
                77179045,
                "G",
                "S");
        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        HgvsProtein hgvsProtein = predictor.calculate();
        Assert.assertNull(hgvsProtein);
    }

    // Frameshift on the last aa causes generation of exact same aa followed by stop codon, i.e.
    // original sequence            ......CTGGCT
    // original sequence                        GTAATCAC......
    // codons                             |  |  |  |
    // original aa sequence               T  T  L  STOP
    // sequence after TTAA insertion            ttaaGTAA
    // codons                             |  |  |  |
    // altered aa sequence                T  T  L  STOP
    // Variant validator describes it as a simple frameshift and that's how we're handling it
    @Test
    public void testFrameShiftOutlier() throws Exception {
        Gene gene = getGene("ENSG00000018408");
        Transcript transcript = getTranscript(gene, "ENST00000465804");
        Variant variant = new Variant("3",
                149238596,
                "-",
                "TTAA");

        // CDS positon 1457


        Assert.assertEquals(400, transcript.getProteinSequence().length());

        Assert.assertEquals("L", String.valueOf(transcript.getProteinSequence().charAt(399)));

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);

        String proteinSequence = "MNPASAPPPLPPPGQQVIHVTQDLDTDLEALFNSVMNPKPSSWRKKILPESFFKEPDSGSHSRQSSTDSSGGHPGPRLAGGAQHVRSHSSPASLQLGTGAGAAGSPAQQHAHLRQQSYDVTDELPLPPGWEMTFTATGQRYFLNHIEKITTWQDPRKAMNQPLNHMNLHPAVSSTPVPQRSMAVSQPNLVMNHQHQQQMAPSTLSQQNHPTQNPPAGLMSMPNALTTQQQQQQKLRLQRIQMERERIRMRQEELMRQEAALCRQLPMEAETLAPVQAAVNPPTMTPDMRSITNNSSDPFLNGGPYHSREQSTDSGLGLGCYSVPTTPEDFLSNVDEMDTGENAGQTPMNINPQQTRFPDFLDCLPGTNVDLGTLESEDLIPLFNDVESALNKSEPFLTWL";

        String reference = "GACACACTCCTCTACAACACCAGAGACTCCCAAACACAAGGCCTTATATTGACTCATTTCAGCTCACATCCTGGCGACTCTCAAGAGAGAAACCTCAGAGTGACTAAAATCTCCATAATGAGAAGACATGTACATTCAGTATCTATTTTGGCATTTTCCCCAATACATCTCTGCTCATCTGACTCTTATCTTGGCATCTGCTTCCTGGTGGATCTGAACTGACCCATAAGCCACGCTTACTAGTGATTTTCCAGAAGATGAATCCGGCCTCGGCGCCCCCTCCGCTCCCGCCGCCTGGGCAGCAAGTGATCCACGTCACGCAGGACCTAGACACAGACCTCGAAGCCCTCTTCAACTCTGTCATGAATCCGAAGCCTAGCTCGTGGCGGAAGAAGATCCTGCCGGAGTCTTTCTTTAAGGAGCCTGATTCGGGCTCGCACTCGCGCCAGTCCAGCACCGACTCGTCGGGCGGCCACCCGGGGCCTCGACTGGCTGGGGGTGCCCAGCATGTCCGCTCGCACTCGTCGCCCGCGTCCCTGCAGCTGGGCACCGGCGCGGGTGCTGCGGGTAGCCCCGCGCAGCAGCACGCGCACCTCCGCCAGCAGTCCTACGACGTGACCGACGAGCTGCCACTGCCCCCGGGCTGGGAGATGACCTTCACGGCCACTGGCCAGAGGTACTTCCTCAATCACATAGAAAAAATCACCACATGGCAAGACCCTAGGAAGGCGATGAATCAGCCTCTGAATCATATGAACCTCCACCCTGCCGTCAGTTCCACACCAGTGCCTCAGAGGTCCATGGCAGTATCCCAGCCAAATCTCGTGATGAATCACCAACACCAGCAGCAGATGGCCCCCAGTACCCTGAGCCAGCAGAACCACCCCACTCAGAACCCACCCGCAGGGCTCATGAGTATGCCCAATGCGCTGACCACTCAGCAGCAGCAGCAGCAGAAACTGCGGCTTCAGAGAATCCAGATGGAGAGAGAAAGGATTCGAATGCGCCAAGAGGAGCTCATGAGGCAGGAAGCTGCCCTCTGTCGACAGCTCCCCATGGAAGCTGAGACTCTTGCCCCAGTTCAGGCTGCTGTCAACCCACCCACGATGACCCCAGACATGAGATCCATCACTAATAATAGCTCAGATCCTTTCCTCAATGGAGGGCCATATCATTCGAGGGAGCAGAGCACTGACAGTGGCCTGGGGTTAGGGTGCTACAGTGTCCCCACAACTCCGGAGGACTTCCTCAGCAATGTGGATGAGATGGATACAGGAGAAAACGCAGGACAAACACCCATGAACATCAATCCCCAACAGACCCGTTTCCCTGATTTCCTTGACTGTCTTCCAGGAACAAACGTTGACTTAGGAACTTTGGAATCTGAAGACCTGATCCCCCTCTTCAATGATGTAGAGTCTGCTCTGAACAAAAGTGAGCCCTTTCTAACCTGGCTGTAATCACTACCATTGTAACTTGGATGTAGCCATGACCTTACATTTCCTGGGCCTCTTGGAAAAAGTGATGGAGCAGAGCAAGTCTGCAGGTGCACCACTTCCCGCCTCCATGACTCGTGCTCCCTCCTTTTTATGTTGCCAGTTTAATCATTGCCTGGTTTTGATTGAGAGTAACTTAAGTTAAACATAAATAAATATTCTATTTTCATTTTCTGCAAGCCTGCGTTCTTGTGACAGATTATACAGAATTGTGTCTGCAGGATTGATTATGCAGAATACTTTTCTCTTTCTTCTCTGCTGCCCCATGGCTAAGCTTTATGGGTGTTAATTGAAATTTATACACCAATTGATTTTAAACCATAAAAAGCTGACCACAGGCAGTTACTTCTGAGGGCATCTTGGTCCAGGAAATGTGCACAAAATTCGACCTGATTTACAGTTTCAAAAACTGTATTGATGACAGTAGTACCAAATGCTTTAAAAACTATTTAACTTGAGCTTTAAAAATCATTGTATGGATAGTAAAATTCTACTGTATGGAATACAATGTAATTTTGAATCCATGCTGGCTCTGATGGCTCTTATTAGTCTGTATTTATAAAGGCACACAGTCCTATTGTAGCTTATCTTTCGTTATTTTACTGCAGAGCATCTAGACAACTTAGTCCCTCCAGCGGGAAAGTAGCAGCAGCAGCATTAGTCACAGGTCTTACACTACAGATCTTGTGAAAGAGACCAGTTTGGTACTAATTATGAGCATTTTATTCAAACAAAAGTTTTTGAAATATTACAACTGGGGATTTAAAAAATTGCAGCTTAGAATCTGATGGTTTTTTTTTTTCTTGATGTTGTTTGTTTGTTTTTGAGATCGAGTTTTGCTCTTGTTGTCCAGGCTGGAATGCAATGGCACAATCTCGGCTCACTGCAACCTCTGCCTTCTGGGTTCAAGCGATTCTCCTGCCTTAGCCTCCCGAGTAGCTGGGATTACAGGCACCTGCCACCACGTCCGGCTAATTTTTTGTATTTTGAGTAGAGACGGGGTTTCACCATAATGGTCAGGCTGTTCTCAAACTCCTGATCTCAGGTGATCCACCCATCTCGGCCTCCCAAAGTGCTGGGATTACTGGCGTGAGCCACCGCACCCGGCCTTGATGTTTATTTTATAAAGCACTGTAATTTTGTAGCTGATGACAAAAGGCAGCCAAATGTTTTTGATAAATCAGTGGCAACTGTATTTTTGTCTTTTGAAATAACTCTGAAAACATCAGGACAACATAGATTTCAACCTGATAGCACACCACACACAGTGAGCTGTTGCTTTTTAAATTCTGAAGCCTTGTCAGGTTTGCTTCCTAGATTTCAAGTGTTTAAAATAATTCTATCTATGAAACTGAAGGATGAAGCAGATCTCTGACTGACATGTAAAAAAAAATGCCCTTTGAGGGTGTATGGTGGAGATAAATGTTTCTGAATTCAGTAAAATTGATTCCTAAGTATATTATCCTAATCCTGTTTGCTACAGTTGGTATAAAAAGGCATGAAATATGTATTCAATACCTCTTATGTAACCAAAACCATTTTTAATTAGCTTTTAAGGACTGAGAGAGCATCATGTTCAACTGGCATGCAGTCTGCCTGCATTGCCAATGAAGTCCTCAACTGTTTAATATTTTGAACTAATATTATTTATAATCTATGAATTTAATCTTTTTTGAAAGACTTTAATAATTTGAGTCTCTGAGAGGATACTTTCAATTTCCATGGGGGACTTATTTGTTGGGGATCTTAAATAAGATTCCTTTTGATCTACCGGAATATACATGTACAGAGTACATTGGATCATGTTGGAAAGAAGGCAAGTGAAAAGGTCAGAGATGAAGTAGCAAAGTTATGGAATATCGTGGAAAGGATACTAGTTGTGAAATGGAAAGAGACAAGTTATAGTACCCCAAAAGCAAAACAAGCAGGAGATGCAAGAGATGCCCCAAAAGGACAAAGCAACAATTTTCTGTTGCCACCTTTATACCGGAAGACTCTGTTGTAGAAGAAAAGAAGGCTTTGGTGCACCTTATGTGGGAGGAGGAGGGGCAGGGCATGCTGATGCTGAGCGTACAGGCAGACAAGAGCGTAGCCTGCTGTTGCCTCCATCACTATGAAATGACTTATTTTACCTGAAGGACCCATGGTTTATGTTCCTCTAATTCCTTTCACTCTCCCTAAGCCCTCTGAGAGAGATGAAGATAGATGATTTTATTGCTACTAAATTGAAGGGAGCACTATTTCTTTTTGTCTTTTGTTAGCAAAAAATTGCAAAAAGAATTGTACATTCTTGCTAAAAATAAATAAATAAATAAAAAATTAAAAAAACAAGGGACCTAACAAAACTCAGCAGTGTTACTGTATTTTTAAAAAATATTTTTATAGACTCATTTTCAGGTTATTAAATGTAAGAGAAACAGATACCCCTCTTTTTTAAAGTAGGTAAATCATTGATGATTTATATTACCAATTTTTAGAAGTAATTTTCTAGTAAGCTTGTGGCATCAGAAAATACTAGAAGATTTTTTTAGTTAAATTAGTTAGAACATTTATGAATGAATATAATAAATATTTTTTCAGAATAAAATATGGACCCTTTGTGTTTACTAATAGATAAAGCCAGATATAATTTTTTGTTTTTAAGGCCACAAAATATGGCCTTTGTTAAAGAACACTAAAGTTAGAAATCTAAAGTTAGAGCAACTTTTTAATGGCTATTTCCTATTATTGTAAGTGTTAAAACCCCTGCAGAATTCTTGATAAGGTGCTATTTATACTATATTTCTTATTATAAGATAACTGTCTTTAGTCTTCTTAGTACTAGTCTTTTTAGTACTAAATCAATCAGTAAACATCATCATTTCACCCCAAAATTTTGTCACAGAAAAGGCGTATCAAATGAAAAATAATTTCAGAGATCTTTCTTTCAAGATATTTTTTCCTGATAAAATACATTGTCTTGAAGTAAATACATTGTCAAAACCTAATTGCAATTCTGTTAAATCTAAGTAATTTTTAGACAGTGTTTCACCGTATTATTTAGGATGTGAAATGCCATTTCTTTCACTGATTACACCATATACAGGAAACAGGTAAAACAGTGAAAACTTTATTGTGCTGGTTGATGCCAACTTGGTTGAAAAGCTCTCTGCAGAAGAAGTGATCTAGACTGACAGAAGTGTTGCTAATTACAAGTTGTGTTCTCATGACGTAATTAGAAAGTAACTTCTCAAAGTACAACTTTTATGAAAAAAATAAGCTGTTAAAAAAAGGAAATCGTAGGTTAATTTAATTGGGAAAATGGGCAATTGACAGAGACCATTTTCCTAACACATATATGTGCTAGTACTTTAACTTTTTAAAATTTTACTTCTACGTTTTGTAATATAAAAATTTCTATTTTAAGTTTAGAATGTTATACGTACCGAAAGTATGCAGCCAAATCGATCAGATCAAACCATTTTACCTGGAGTTTGGTACTGGTTTTTACTTCTCTGAATCTGTATAAGAAAAATAAAGACAATTGAACTTCCA";

        String expected =
                "GACACACTCCTCTACAACACCAGAGACTCCCAAACACAAGGCCTTATATTGACTCATTTCAGCTCACATCCTGGCGACTCTCAAGAGAGAAACCTCAGAGTGACTAAAATCTCCATAATGAGAAGACATGTACATTCAGTATCTATTTTGGCATTTTCCCCAATACATCTCTGCTCATCTGACTCTTATCTTGGCATCTGCTTCCTGGTGGATCTGAACTGACCCATAAGCCACGCTTACTAGTGATTTTCCAGAAGATGAATCCGGCCTCGGCGCCCCCTCCGCTCCCGCCGCCTGGGCAGCAAGTGATCCACGTCACGCAGGACCTAGACACAGACCTCGAAGCCCTCTTCAACTCTGTCATGAATCCGAAGCCTAGCTCGTGGCGGAAGAAGATCCTGCCGGAGTCTTTCTTTAAGGAGCCTGATTCGGGCTCGCACTCGCGCCAGTCCAGCACCGACTCGTCGGGCGGCCACCCGGGGCCTCGACTGGCTGGGGGTGCCCAGCATGTCCGCTCGCACTCGTCGCCCGCGTCCCTGCAGCTGGGCACCGGCGCGGGTGCTGCGGGTAGCCCCGCGCAGCAGCACGCGCACCTCCGCCAGCAGTCCTACGACGTGACCGACGAGCTGCCACTGCCCCCGGGCTGGGAGATGACCTTCACGGCCACTGGCCAGAGGTACTTCCTCAATCACATAGAAAAAATCACCACATGGCAAGACCCTAGGAAGGCGATGAATCAGCCTCTGAATCATATGAACCTCCACCCTGCCGTCAGTTCCACACCAGTGCCTCAGAGGTCCATGGCAGTATCCCAGCCAAATCTCGTGATGAATCACCAACACCAGCAGCAGATGGCCCCCAGTACCCTGAGCCAGCAGAACCACCCCACTCAGAACCCACCCGCAGGGCTCATGAGTATGCCCAATGCGCTGACCACTCAGCAGCAGCAGCAGCAGAAACTGCGGCTTCAGAGAATCCAGATGGAGAGAGAAAGGATTCGAATGCGCCAAGAGGAGCTCATGAGGCAGGAAGCTGCCCTCTGTCGACAGCTCCCCATGGAAGCTGAGACTCTTGCCCCAGTTCAGGCTGCTGTCAACCCACCCACGATGACCCCAGACATGAGATCCATCACTAATAATAGCTCAGATCCTTTCCTCAATGGAGGGCCATATCATTCGAGGGAGCAGAGCACTGACAGTGGCCTGGGGTTAGGGTGCTACAGTGTCCCCACAACTCCGGAGGACTTCCTCAGCAATGTGGATGAGATGGATACAGGAGAAAACGCAGGACAAACACCCATGAACATCAATCCCCAACAGACCCGTTTCCCTGATTTCCTTGACTGTCTTCCAGGAACAAACGTTGACTTAGGAACTTTGGAATCTGAAGACCTGATCCCCCTCTTCAATGATGTAGAGTCTGCTCTGAACAAAAGTGAGCCCTTTCTAACCTGGCTTTAAGTAATCACTACCATTGTAACTTGGATGTAGCCATGACCTTACATTTCCTGGGCCTCTTGGAAAAAGTGATGGAGCAGAGCAAGTCTGCAGGTGCACCACTTCCCGCCTCCATGACTCGTGCTCCCTCCTTTTTATGTTGCCAGTTTAATCATTGCCTGGTTTTGATTGAGAGTAACTTAAGTTAAACATAAATAAATATTCTATTTTCATTTTCTGCAAGCCTGCGTTCTTGTGACAGATTATACAGAATTGTGTCTGCAGGATTGATTATGCAGAATACTTTTCTCTTTCTTCTCTGCTGCCCCATGGCTAAGCTTTATGGGTGTTAATTGAAATTTATACACCAATTGATTTTAAACCATAAAAAGCTGACCACAGGCAGTTACTTCTGAGGGCATCTTGGTCCAGGAAATGTGCACAAAATTCGACCTGATTTACAGTTTCAAAAACTGTATTGATGACAGTAGTACCAAATGCTTTAAAAACTATTTAACTTGAGCTTTAAAAATCATTGTATGGATAGTAAAATTCTACTGTATGGAATACAATGTAATTTTGAATCCATGCTGGCTCTGATGGCTCTTATTAGTCTGTATTTATAAAGGCACACAGTCCTATTGTAGCTTATCTTTCGTTATTTTACTGCAGAGCATCTAGACAACTTAGTCCCTCCAGCGGGAAAGTAGCAGCAGCAGCATTAGTCACAGGTCTTACACTACAGATCTTGTGAAAGAGACCAGTTTGGTACTAATTATGAGCATTTTATTCAAACAAAAGTTTTTGAAATATTACAACTGGGGATTTAAAAAATTGCAGCTTAGAATCTGATGGTTTTTTTTTTTCTTGATGTTGTTTGTTTGTTTTTGAGATCGAGTTTTGCTCTTGTTGTCCAGGCTGGAATGCAATGGCACAATCTCGGCTCACTGCAACCTCTGCCTTCTGGGTTCAAGCGATTCTCCTGCCTTAGCCTCCCGAGTAGCTGGGATTACAGGCACCTGCCACCACGTCCGGCTAATTTTTTGTATTTTGAGTAGAGACGGGGTTTCACCATAATGGTCAGGCTGTTCTCAAACTCCTGATCTCAGGTGATCCACCCATCTCGGCCTCCCAAAGTGCTGGGATTACTGGCGTGAGCCACCGCACCCGGCCTTGATGTTTATTTTATAAAGCACTGTAATTTTGTAGCTGATGACAAAAGGCAGCCAAATGTTTTTGATAAATCAGTGGCAACTGTATTTTTGTCTTTTGAAATAACTCTGAAAACATCAGGACAACATAGATTTCAACCTGATAGCACACCACACACAGTGAGCTGTTGCTTTTTAAATTCTGAAGCCTTGTCAGGTTTGCTTCCTAGATTTCAAGTGTTTAAAATAATTCTATCTATGAAACTGAAGGATGAAGCAGATCTCTGACTGACATGTAAAAAAAAATGCCCTTTGAGGGTGTATGGTGGAGATAAATGTTTCTGAATTCAGTAAAATTGATTCCTAAGTATATTATCCTAATCCTGTTTGCTACAGTTGGTATAAAAAGGCATGAAATATGTATTCAATACCTCTTATGTAACCAAAACCATTTTTAATTAGCTTTTAAGGACTGAGAGAGCATCATGTTCAACTGGCATGCAGTCTGCCTGCATTGCCAATGAAGTCCTCAACTGTTTAATATTTTGAACTAATATTATTTATAATCTATGAATTTAATCTTTTTTGAAAGACTTTAATAATTTGAGTCTCTGAGAGGATACTTTCAATTTCCATGGGGGACTTATTTGTTGGGGATCTTAAATAAGATTCCTTTTGATCTACCGGAATATACATGTACAGAGTACATTGGATCATGTTGGAAAGAAGGCAAGTGAAAAGGTCAGAGATGAAGTAGCAAAGTTATGGAATATCGTGGAAAGGATACTAGTTGTGAAATGGAAAGAGACAAGTTATAGTACCCCAAAAGCAAAACAAGCAGGAGATGCAAGAGATGCCCCAAAAGGACAAAGCAACAATTTTCTGTTGCCACCTTTATACCGGAAGACTCTGTTGTAGAAGAAAAGAAGGCTTTGGTGCACCTTATGTGGGAGGAGGAGGGGCAGGGCATGCTGATGCTGAGCGTACAGGCAGACAAGAGCGTAGCCTGCTGTTGCCTCCATCACTATGAAATGACTTATTTTACCTGAAGGACCCATGGTTTATGTTCCTCTAATTCCTTTCACTCTCCCTAAGCCCTCTGAGAGAGATGAAGATAGATGATTTTATTGCTACTAAATTGAAGGGAGCACTATTTCTTTTTGTCTTTTGTTAGCAAAAAATTGCAAAAAGAATTGTACATTCTTGCTAAAAATAAATAAATAAATAAAAAATTAAAAAAACAAGGGACCTAACAAAACTCAGCAGTGTTACTGTATTTTTAAAAAATATTTTTATAGACTCATTTTCAGGTTATTAAATGTAAGAGAAACAGATACCCCTCTTTTTTAAAGTAGGTAAATCATTGATGATTTATATTACCAATTTTTAGAAGTAATTTTCTAGTAAGCTTGTGGCATCAGAAAATACTAGAAGATTTTTTTAGTTAAATTAGTTAGAACATTTATGAATGAATATAATAAATATTTTTTCAGAATAAAATATGGACCCTTTGTGTTTACTAATAGATAAAGCCAGATATAATTTTTTGTTTTTAAGGCCACAAAATATGGCCTTTGTTAAAGAACACTAAAGTTAGAAATCTAAAGTTAGAGCAACTTTTTAATGGCTATTTCCTATTATTGTAAGTGTTAAAACCCCTGCAGAATTCTTGATAAGGTGCTATTTATACTATATTTCTTATTATAAGATAACTGTCTTTAGTCTTCTTAGTACTAGTCTTTTTAGTACTAAATCAATCAGTAAACATCATCATTTCACCCCAAAATTTTGTCACAGAAAAGGCGTATCAAATGAAAAATAATTTCAGAGATCTTTCTTTCAAGATATTTTTTCCTGATAAAATACATTGTCTTGAAGTAAATACATTGTCAAAACCTAATTGCAATTCTGTTAAATCTAAGTAATTTTTAGACAGTGTTTCACCGTATTATTTAGGATGTGAAATGCCATTTCTTTCACTGATTACACCATATACAGGAAACAGGTAAAACAGTGAAAACTTTATTGTGCTGGTTGATGCCAACTTGGTTGAAAAGCTCTCTGCAGAAGAAGTGATCTAGACTGACAGAAGTGTTGCTAATTACAAGTTGTGTTCTCATGACGTAATTAGAAAGTAACTTCTCAAAGTACAACTTTTATGAAAAAAATAAGCTGTTAAAAAAAGGAAATCGTAGGTTAATTTAATTGGGAAAATGGGCAATTGACAGAGACCATTTTCCTAACACATATATGTGCTAGTACTTTAACTTTTTAAAATTTTACTTCTACGTTTTGTAATATAAAAATTTCTATTTTAAGTTTAGAATGTTATACGTACCGAAAGTATGCAGCCAAATCGATCAGATCAAACCATTTTACCTGGAGTTTGGTACTGGTTTTTACTTCTCTGAATCTGTATAAGAAAAATAAAGACAATTGAACTTCCA";
        Assert.assertEquals("p.Leu400fs", predictor.calculate());
    }


    @Test
    public void testDelins() throws Exception {
        // Issue #8 delins reported as dels
        // 166     14:91313274:CCTGCTGCC:- 14      91313274        CCTGCTGCC       -       indel   ENSP00000374507 p.Trp845_Val848delinsLeu        p.Trp845_Val848del      delins_as_del
        Gene gene = getGene("ENSG00000015133");
        Transcript transcript = getTranscript(gene, "ENST00000389857");
        Variant variant = new Variant("14",
                91313274,
                "CCTGCTGCC",
                "-");

        HgvsProteinCalculator predictor = new HgvsProteinCalculator(variant, transcript);
        Assert.assertEquals("p.Trp845_Val848delinsLeu", predictor.calculate());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////// UTILS ///////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Transcript getTranscript(Gene gene, String id) {
        for (Transcript transcript : gene.getTranscripts()) {
            if (transcript.getId().equals(id)) {
                return transcript;
            }
        }
        return null;
    }

    private Gene getGene(String id) {
        for (Gene gene : geneList) {
            if (gene.getId().equals(id)) {
                return gene;
            }
        }
        return null;
    }

    private List<Gene> loadGenes(Path path) throws IOException {
        List<Gene> repeatSet = new ArrayList<>();

        try (BufferedReader bufferedReader = FileUtils.newBufferedReader(path)) {
            String line = bufferedReader.readLine();
            while (line != null) {
                repeatSet.add(jsonObjectMapper.convertValue(JSON.parse(line), Gene.class));
                line = bufferedReader.readLine();
            }
        }

        return repeatSet;
    }
}