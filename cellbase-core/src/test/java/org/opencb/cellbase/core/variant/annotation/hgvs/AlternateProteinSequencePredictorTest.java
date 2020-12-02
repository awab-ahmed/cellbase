package org.opencb.cellbase.core.variant.annotation.hgvs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.util.ajax.JSON;
import org.opencb.biodata.models.core.Gene;
import org.opencb.biodata.models.core.Transcript;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.avro.VariantType;
import org.opencb.cellbase.core.serializer.CellBaseFileSerializer;
import org.opencb.cellbase.core.serializer.CellBaseJsonFileSerializer;
import org.opencb.commons.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by fjlopez on 14/02/17.
 */
public class AlternateProteinSequencePredictorTest {

    private ObjectMapper jsonObjectMapper;
    List<Gene> geneList;

    public AlternateProteinSequencePredictorTest() throws IOException {

    }

    @Before
    public void setUp() throws IOException {

        jsonObjectMapper = new ObjectMapper();
        jsonObjectMapper.configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true);
        jsonObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        geneList = loadGenes(Paths.get(getClass().getResource("/hgvs/gene.test.json.gz").getFile()));
    }

    @Test
    public void testInsert() throws Exception {
        Gene gene = getGene("ENSG00000091536");
        Transcript transcript = getTranscript(gene, "ENST00000418233");
        Variant variant = new Variant("17",
                18173905,
                "-",
                "A");

        AlternateProteinSequencePredictor predictor = new AlternateProteinSequencePredictor(variant, transcript);

        String expected =
                "TCATCCTAGGAGGTGCCTGTGGCCGGGCGCAGTAGCTCATGCCTGTAATCCCAGCACTTTGGGAGGCCGAGGCGGGCGGACCACCTGAGGTCAGGAATTTGAGACTAGCCGGCCCAACATGGCGAAACCCCATCTCTACTAAACATACAAAAAATTAGCCAGGCGTCGTGGCGGGCGCCTGTAATCCCAGCTACTCAGGAGGCTGAGGCAGGAGAATCGCTTGAACCCAGGAGGCGGAGCTTGCAGTGGGCCGAGATTGCGCCACTGCACTCTAGCCTGGGGGACAACAGCGAAACTCCGTCTCAAAAATATATATATATATTAATTAAATAAAAAAACGAGGTGCCTTCTCCTGACTCCCTGATCCCCGCGCTCTCCAGCTCTGCCCTCGCGATCGCTGGAGCCCCCTGAGGAACTCACGCAGACGCGGCTGCACCGCCTCATCAATCCCAACTTCTACGGCTATCAGGACGCCCCCTGGAAGATCTTCCTGCGCAAAGAGGTGTTTTACCCCAAGGACAGCTACAGCCATCCTGTGCAGCTTGACCTCCTGTTCCGGCAGATCCTGCACGACACGCTCTCCGAGGCCTGCCTTCGCATCTCTGAGGATGAGAGGCTCAGGAATGAAGGCCTTGTTTGCCCAGAACCAGCTGGACACACAGAAGCCTCTGGTAACGGAAAGCGTGAAGCGGGCCGTGGTCAGCACTGCACGAGACACCTGGGAGGTCTACTTCTCCCGCATCTTCCCCGCCACGGGCAGCGTGGGCACTGGTGTGCAGCTCCTAGCTGTGTCCCACGTGGGCATCAAACTCCTGAGGATGGTCAAGGGTGGCCAGGAGGCCGGCGGGCAGCTGCGGGTCCTGCGTGCATACAGCTTTGCAGATATCCTGTTTGTGACCATGCCCTCCCAGAACATGCTGGAGTTCAACCTGGCCAGTGAGAAGGTCATCCTCTTCTCAGCCCGAGCGCACCAGGTCAAGACCCTGGTAGATGACTTCATCTTGGAGCTGAAGAAGGACTCTGACTACGTGGTCGCTGTGAGGAACTTCCTGCCTGAGGACCCTGCGCTGCTGGCTTTCCACAAGGGTGACATCATACACCTGCAGCCCCTAGAGCCACCTCGAGTGGGCTACAGTGCTGGCTGCGTGGTTCGCAGGAAGGTGGTGTACCTGGAGGAGCTGCGACGTAGAGGCCCCGACTTTGGCTGGAGGTTCGGGACCATCCACGGGCGCGTGGGCCGCTTCCCTTCGGAGCTGGTGCAGCCCGCTGCTGCCCCCGACTTCCTGCAGCTGCCAACGGAGCCAGGCCGCGGCCGAGCAGCCGCCGTGGCCGCTGCTGTGGCCTCTGCAGCCGCTGCACAGGAGGTGGGCCGCAGGAGAGAGGGTCCCCCAGTCAGGGCCCGCTCTGCTGACCATGGGGAGGACGCCCTGGCGCTCCCACCCTACACAATGCTCGAGTTTGCCCAGAAGTATTTCCGAGACCCTCAGAGGAGACCCCAGGATGGCCTCAGGCTGAAATCCAAGGAGCCTCGGGAGTCCAGAACCTTGGAGGACATGCTTTGCTTCACCAAGACTCCCCTCCAGGAATCCCTCATCGAACTCAGCGACAGCAGCCTCAGCAAGATGGCCACCGACATGTTCCTAGCTGTAATGAGGTTCATGGGGGATGCCCCACTGAAGGGCCAGAGTGACCTGGACGTGCTTTGTAACCTCCTGAAGCTGTGCGGGGACCATGAGGTCATGCGGGATGAATGTTACTGCCAAGTTGTGAAGCAGATCACAGACAATACCAGCTCCAAGCAGGACAGCTGCCAGCGAGGCTGGAGGCTGCTGTATATCGTGACCGCCTACCACAGCTGCTCTGAGGTCCTCCACCCACACCTCACTCGCTTCCTCCAAGACGTGAGCCGGACCCCAGGCCTGCCCTTTCAGGGGATCGCCAAGGCCTGCGAGCAGAACCTGCAGAAAACCTTGCGCTTCGGAGGTCGTCTGGAGCTCCCCAGCAGCATAGAGCTTCGGGCCATGTTGGCAGGCCGCAGTTCCAAGAGGCAACTCTTTCTTCTTCCTGGAGGCCTTGAACGCCATCTCAAAATCAAAACATGCACTGTGGCCCTGGACGTGGTGGAAGAGATATGTGCTGAGATGGCTCTGACACGCCCTGAGGCCTTCAATGAATATGTTATCTTCGTTGTCACCAACCGTGGCCAGCATGTGTGCCCACTCAGTCGCCGTGCTTACATCCTGGATGTGGCCTCAGAGATGGAGCAGGTGGACGGCGGCTACATGCTCTGGTTCCGGCGTGTGCTCTGGGATCAGCCACTCAAGTTCGAGAATGAGCTATATGTGACCATGCACTACAACCAGGTCCTGCCTGACTACCTGAAGGGACTCTTCAGCAGTGTGCCGGCCAGCCGGCCCAGCGAGCAGCTGCTGCAGCAGGTGTCCAAGCTGGCTTCACTGCAGCATCGCGCCAAGGACCACTTCTACCTGCCGAGCGTGCGGGAAGTCCAGGAGTACATCCCAGCCCAGCTCTACCGTACAACGGCAGGCTCGACCTGGCTCAACCTGGTCAGCCAGCACCGGCAGCAGACACAGGCGCTCAGCCCCCACCAGGCCCGTGCCCAGTTTCTGGGCCTCCTCAGCGCCTTACCTATGTTCGGCTCCTCCTTCTTCTTCATCCAGAGCTGCAGCAACATTGCTGTGCCAGCCCCTTGCATCCTTGCCATCAACCACAATGGCCTCAACTTTCTCAGCACAGAGACTCATGAATTGATGGTGAAGTTCCCCCTGAAGGAGATCCAGTCGACGCGGACCCAGCGGCCCACGGCCAACTCCAGCTACCCCTATGTGGAGATTGCGCTGGGGGACGTGGCGGCCCAGCGCACCTTGCAGCTGCAGCTGGAGCAGGTAAGAGCTGGGGAAGTGTTGGATGGGCGTGGACTGTCACTGTCACCTGCAGGGACTGGAACTGTGTCGTGTGGTGGCCGTGCACGTGGAGAACCTGCTCAGTGCCCATGAGAAGCGGCTCACATTGCCCCCCAGCGAGATCACCCTGCTCTGACCCAGCCCCCAGCCCTCCAGTACCTTCTGCCAGAAGACTCACTGTGTGGCCTCAGAGAAATCACTGAACCTCTCAGGATCAATGACCCCTGTAAGGGGCCAGAGCCTTGGAGGACACTAAGAGGAGGCAGGAGGAGCAACTCAAATCCCCAAGAACACAAGAAGACCCATCCTGAACTGGGATGGAATGGCAGCATGCAAACTTGGATCAGATAGCAGGAGGAACTTTCAAAAGTCTGGCCCACTGTGCAGTGGAGCAGAAGGCAGGACCATGAGGCCTCCTGCCATGTACCCATTGCAGACCCTGCCCCTAACTCCTGCCTATGACACAGAAGCCCCACACCAGTTGCCCA";

        String actual = predictor.getAlternateDnaSequence();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDeletion() throws Exception {
        Gene gene = getGene("ENSG00000138081");
        Transcript transcript = getTranscript(gene, "ENST00000402508");
        Variant variant = new Variant("2",
                47822224,
                "T",
                "-");

        AlternateProteinSequencePredictor predictor = new AlternateProteinSequencePredictor(variant, transcript);

        String expected =
                "ATCATAGCAATAAGATTTGTAAAACATTGCAATGGCTGAAAAACTGTCTAACAGGAAATTTTAAGTGTTATTCCAAAGAAGAACAAGCCTAAAAATGATGATGTGCCTGCAGATTGGTTGCAGAAGAATCAGGTCCTGGTGCACAAAATAGTCCATACCAACTTCGTAGAAAAACTCTTTTGCCGAAAAGAACAGCGTGTCCCACAAAGAACAGTATGGAGGGCGCCTCAACTTCAACTACAGAAAACTTTGGTCATCGTGCAAAACGTGCAAGAGTGTCTGGAAAATCACAAGATCTATCAGCAGCACCTGCTGAACAGTATCTTCAGGAGAAACTGCCAGATGAAGTGGTTCTAAAAATCTTCTCTTACTTGCTGGAACAGGATCTTTGTAGAGCAGCTTGTGTATGTAAACGCTTCAGTGAACTTGCTAATGATCCAATTTTGTGGAAACGATTATATATGGAAGTATTTGAATATACTCGCCCTATGATGCATCCTGAACCTGGAAAATTCTACCAGATTAATCCAGAAGAGTATGAACATCCAAATCCCTGGAAAGAGAGTTTCCAGCAGTTGTATAAAGGTGCACATGTAAAGCCAGGATTTGCTGAACATTTCTACAGTAACCCTGCAAGATATAAAGGAAGAGAAAATATGTTGTATTATGATACTATTGAAGATGCCCTTGGTGGGGTACAAGAGGCTCATTTTGATGGACTTATCTTTGTTCATTCTGGAATATATACTGATGAATGGATATATATTGAATCTCCAATCACCATGATTGGTGCAGCACCTGGGAAAGTGGCAGACAAAGTTATAATTGAAAACACTAGAGATTCAACCTTCGTTTTTATGGAAGGCTCTGAAGATGCTTATGTTGGATATATGACAATAAGGTTTAACCCTGATGACAAATCTGCACAACACCACAATGCACACCACTGCTTAGAGATTACAGTAAATTGTAGCCCTATTATTGATCACTGTATCATCCGAAGTACATGTACAGTTGGTTCTGCAGTATGTGTTAGTGGTCAAGGAGCATGTCCCACCATCAAGCACTGTAACATCAGTGACTGTGAAAATGTTGGACTATATATAACAGATCATGCACAGGGAATATATGAGGATAATGAAATTTCCAATAATGCGTTAGCTGGGATTTGGGTTAAAAATCATGGAAACCCAATTATTAGACGGAATCATATTCATCATGGACGTGATGTTGGTGTGTTCACATTTGATCATGGCATGGGTTACTTTGAAAGTTGCAATATACACAGAAATAGGATAGCAGGCTTTGAAGTAAAAGCCTATGCTAACCCTACAGTGGTTCGATGTGAAATTCACCATGGGCAGACTGGAGGAATATATGTCCATGAAAAAGGAAGAGGACAATTCATAGAGAATAAAATCTATGCAAACAACTTTGCAGGTGTATGGATTACCTCAAATAGTGACCCAACAATAAGGGGAAATTCTATATTTAATGGAAATCAAGGAGGAGTTTACATCTTTGGTGATGGACGAGGCCTTATTGAAGGAAATGACATTTATGGCAATGCATTAGCAGGAATTCAAATTAGGACAAACAGTTGTCCAATTGTTCGGCATAACAAAATTCATGATGGCCAGCATGGTGGGATTTATGTGCATGAAAAGGGACAAGGAGTAATAGAAGAGAATGAAGTTTATAGTAACACTCTAGCTGGAGTCTGGGTGACAACTGGCAGCACTCCAGTACTGAGAAGAAACCGGATACACAGTGGCAAGCAGGTTGGTGTTTATTTTTATGACAATGGACATGGAGTGCTAGAAGACAATGATATCTATAATCATATGTATTCAGGGGTTCAGATAAGGACTGGAAGCAACCCCAAAATTAGACGCAACAAAATCTGGGGAGGACAGAATGGTGGAATTCTAGTTTATAATTCTGGTCTAGGCTGTATAGAAGACAATGAAATATTTGACAATGCAATGGCTGGAGTCTGGATTAAGACAGATAGTAATCCTACACTAAGAAGAAATAAAATCCATGATGGAAGAGATGGTGGCATCTGTATATTTAATGGGGGTCGAGGTCTCCTTGAAGAAAATGATATTTTCAGGAATGCTCAAGCAGGTGTTCTCATCAGCACTAATAGTCATCCAATCTTAAGGAAAAACAGAATATTTGATGGATTTGCCGCAGGTATTGAAATTACAAATCACGCAACTGCAACACTAGAAGGCAATCAGATTTTTAACAACCGGTTTGGAGGCTTATTTTTAGCATCTGGTGTTAATGTGACAATGAAAGATAACAAAATAATGAACAATCAAGATGCCATAGAAAAGGCTGTTAGTAGAGGCCAATGTTTATATAAAATATCAAGTTATACCAGCTATCCCATGCATGATTTCTACAGATGTCATACTTGTAACACCACAGATCGAAATGCCATATGTGTGAACTGCATTAAGAAGTGCCATCAGGGACATGATGTAGAGTTTATTAGACATGATAGGTTTTTCTGTGACTGTGGTGCTGGAACACTGTCTAATCCTTGTACATTAGCTGGTGAGCCTACACATGATACAGATACACTATATGACTCTGCTCCACCTATAGAATCTAATACATTGCAGCACAACTGAATTCCTTCCCTAAAGAAAAAGTCCTGCCATTGTAACATCATAACTTAAAACACTTTTTTGGAAGAAGATTTAAAATATTTGCCCATGCTACAGGAAGAGACTGTATTAAAAATGGATACACAAGGTCAGTTGACACTATGAAGCTCAAGCTACCAAAAAGAAAGTGGCAATATATTGACTCAGGATCTCAAAGCTGGGTGTTTTAGCATTACTGTGTAAAGACTTGAAGGGACAGAAGTGAAGAAAATAAGCTGCAATTTTGTACAGATACCAACTTCTGAAAAGCTGGTGTTTTTACAACTAGCATTGAATGCAGTCCAATTTGCAGTAGTAATCTTCAATAGACAAGCAGCTTTGTTGCTGCCTTTATGGACATGGGTACCAATTGCTTTTGTAATATGGTAAAAATGTGAGCTAGCACTTCTGCGTTCCTTTTGATTTTTTTTTTTTAACATGTATTCAGATTGAGAAATATGATTTTAATGCTTTAATCTCATGTAGTTTGTTTTTAATTTCAAGCAAAATCTTACTGTACTTGAATGTGCCCTGTTTTGTTAGCACACCTAGACTTGCTGTAACTGTACTCATGTCCCAGTATGTACGTTCTTTCTATGAAAGAGAAAACACTAATCTTAAATTATATCCAGCAATGTTTCTGGTATCCTTTAAGAAAAGTTAAGACTATTATTTCCTTCCCTTCCCTGTGCAGCATTCAAAATCACCCCTAGAAAAAGTGAGTGTTTTAATGAGACTTCCTAGGAAGGGATGCACTGTAAAACAAAAGTATTCTTGTAACTCAGTTTTGTGAAAGGTAAAAAACTACTATGTTTTAAGTACACTTTAGAAAGTCTCTTCAAAACAAACAGTCCTGTATTTGAACTCTGTTCATAGTTTTTTTTTGAACAGTTTAGACAAAACTGCTGGAAATTAAACCAATTTCCTGCATTAAGCTGAGACAAGTATATATACAGCCCTATACAGTTTCATAGCTTTTCCTCCCCCATTTATGTGTATTGGTGACAGTGGGTATAAAACAGCCTGAAAGTGTATGCATGTACCATAACATCTAGACTTAATATATTGTGGAGTATTCAATAACCATTATGTAGAAGGTAGATAAGAATTAAAAGGGTTTAATTTCCTAGAAAGAAAATGGAAAAATGGTCATTTTTAAAAAATAAAGTTTATTAGATCATAA";

        String actual = predictor.getAlternateDnaSequence();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSNV() throws Exception {
        Gene gene = getGene("ENSG00000196549");
        Transcript transcript = getTranscript(gene, "ENST00000492661");
        Variant variant = new Variant("3",
                155143536,
                "G",
                "A");

        AlternateProteinSequencePredictor predictor = new AlternateProteinSequencePredictor(variant, transcript);

        String expected =
                "ACATTCCTACTGAGTATGACGAATGGTTGTTGATGGTTATCACTGGAGATTGAAAACTGGGATTCCTTTTTCACCAGGAATTGGTGCCTACCATGGCTGGGGATACACAGTGATGGATTTG"
                        + "TGACTTGCTCCTCTGAAAGCTGGCCCAAGGGTGTACAGCATATGAATGTGGGGGAGTTTACACTACACACGCTCTTGGCTATTTTAGGTGATGGGCAAGTCAGAAAGTCAG"
                        + "ATGGATATAACTGATATCAACACTCCAAAGCCAAAGAAGAAACAGCGATGGACTCCACTGGAGATCAGCCTCTCGGTCCTTGTCCTGCTCCTCACCATCATAGCTGTGACA"
                        + "ATGATCGCACTCTATGCAACCTACGATGATGGTATTTGCAAGTCATCAGACTGCATAAAATCAGCTGCTCGACTGATCCAAAACATGGATGCCACCACTGAGCCTTGTACA"
                        + "GACTTTTTCAAATATGCTTGCGGAGGCTGGTTGAAACGTAATGTCATTCCCGAGACCAGCTCCCGTTACGGCAACTTTGACATTTTAAGAGATGAACTAGAAGTCGTTTTG"
                        + "AAAGATGTCCTTCAAGAACCCAAAACTGAAGATATAGTAGCAGTGCAGAAAGCAAAAGCATTGTACAGGTCTTGTATAAATGAATCTGCTATTGATAGCAGAGGTGGAGAA"
                        + "CCTCTACTCAAACTGTTACCAGACATATATGGGTGGCCAGTAGCAACAGAAAACTGGGAGCAAAAATATGGTGCTTCTTGGACAGCTGAAAAAGCTATTGCACAACTGAAT"
                        + "TCTAAATATGGGAAAAAAGTCCTTATTAATTTGTTTGTTGGCACTGATGATAAGAATTCTGTGAATCATGTAATTCATATTGACCAACCTCGACTTGGCCTCCCTTCTAGA"
                        + "GATTACTATGAATGCACTGGAATCTATAAAGAGGCTTGTACAGCATATGTGGATTTTATGATTTCTGTGGCCAGATTGATTCGTCAGGAAGAAAGATTGCCCATCGATGAA"
                        + "AACCAGCTTGCTTTGGAAATGAATAAAGTTATGGAATTGGAAAAAGAAATTGCCAATGCTACGGCTAAACCTGAAGATCGAAATGATCCAATGCTTCTGTATAACAAGATG"
                        + "ACATTGGCCCAGATCCAAAATAACTTTTCACTAGAGATCAATGGGAAGCCATTCAGCTGGTTGAATTTCACAAATGAAATCATGTCAACTGTGAATATTAGTATTACAAAT"
                        + "GAGGAAGATGTGGTTGTTTATGCTCCAGAATATTTAACCAAACTTAAGCCCATTCTTACCAAATATTCTGCCAGAGATCTTCAAAATTTAATGTCCTGGAGATTCATAATG"
                        + "GATCTTGTAAGCAGCCTCAGCCGAACCTACAAGGAGTCCAGAAATGCTTTCCGCAAGGCCCTTTATGGTACAACCTCAGAAACAGCAACTTGGAGACGTTGTGCAAACTAT"
                        + "GTCAATGGGAATATGGAAAATGCTGTGGGGAGGCTTTATGTGGAAGCAGCATTTGCTGGAGAGAGTAAACATGTGGTCGAGGATTTGATTGCACAGATCCGAGAAGTTTTT"
                        + "ATTCAGACTTTAGATGACCTCACTTGGATGGATGCCGAGACAAAAAAGAGAGCTGAAGAAAAGGCCTTAGCAATTAAAGAAAGGATCGGCTATCCTGATGACATTGTTTCA"
                        + "AATGATAACAAACTGAATAATGAGTACCTCGAGTTGAACTACAAAGAAGATGAATACTTCGAGAACATAATTCAAAATTTGAAATTCAGCCAAAGTAAACAACTGAAGAAG"
                        + "CTCCGAGAAAAGGTGGACAAAGATGAGTGGATAAGTGGAGCAGCTGTAGTCAATGCATTTTACTCTTCAGGAAGAAATCAGATAGTCTTCCCAGCCGGCATTCTGCAGCCC"
                        + "CCCTTCTTTAGTGCCCAGCAGTCCAACTCATTGAACTATGGGGGCATCGGCATGGTCATAGGACACGAAATCACCCATGGCTTCGATGACAATGGCAGAAACTTTAACAAA"
                        + "GATGGAGACCTCGTTGACTGGTGGACTCAACAGTCTGCAAGTAACTTTAAGGAGCAATCCCAGTGCATGGTGTATCAGTATGGAAACTTTTCCTGGGACCTGGCAGGTGGA"
                        + "CAGCACCTTAATGGAATTAATACACTGGGAGAAAACATTGCTGATAATGGAGGTCTTGGTCAAGCATACAGAGCCTATCAGAATTATATTAAAAAGAATGGCGAAGAAAAA"
                        + "TTACTTCCTGGACTTGACCTAAATCACAAACAACTATTTTTCTTGAACTTTGCACAGGTGTGGTGTGGAACCTATAGGCCAGAGTATGCGGTTAACTCCATTAAAACAGAT"
                        + "GTGCACAGTCCAGGCAATTTCAGGATTATTGGGACTTTGCAGAACTCTGCAGAGTTTTCAGAAGCCTTTCACTGCCGCAAGAATTCATACATGAATCCAGAAAAGAAGTGC"
                        + "CGGGTTTGGTGATCTTCAAAAGAAGCATTGCAGCCCTTGGCTAGACTTGCCAACACCACAGAAATGGGGAATTCTCTAATCGAAAGAAAATGGGCCCTAGGGGTCACTGTA"
                        + "CTGACTTGAGGGTGATTAACAGAGAGGGCACCATCACAATACAGATAACATTAGGTTGTCCTAGAAAGGGTGTGGAGGGAGGAAGGGGGTCTAAGGTCTATCAAGTCAATC"
                        + "ATTTCTCACTGTGTACATAATGCTTAATTTCTAAAGATAATATTACTGTTTATTTCTGTTTCTCATATGGTCTACCAGTTTGCTGATGTCCCTAGAAAACAATGCAAAACC"
                        + "TTTGAGGTAGACCAGGATTTCTAATCAAAAGGGAAAAGAAGATGTTGAAGAATACAGTTAGGCACCAGA";

        String actual = predictor.getAlternateDnaSequence();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void test1() throws Exception {
        Gene gene = getGene("ENSG00000068366");
        Transcript transcript = getTranscript(gene, "ENST00000514500");
        Variant variant = new Variant("X",
                109669061,
                "-",
                "T");

        AlternateProteinSequencePredictor predictor = new AlternateProteinSequencePredictor(variant, transcript);

        String expected =
                "AATACTCTGTTCAAGATAGGGTATGATTACAAATTGGAACAGATCAAAAAGGGATATGATGCACCTCTTTGCAATCTGTTACTGTTTAAAAAGGTCAAGGCCCTGCTGGGAGGGAATGTCC"
                        + "GCATGATGCTGTCTGGAGGGGCCCCGCTATCTCCTCAGACACACCGATTCATGAATGTCTGCTTCTGCTGCCCAATTGGCCAGGGTTATGGACTGACAGAATCATGTGGTG"
                        + "CTGGGACAGTTACTGAAGTAACTGACTATACTACTGGCAGAGTTGGAGCACCTCTTATTTGCTGTGAAATTAAGCTAAAAGACTGGCAAGAAGGCGGTTATACAATTAATG"
                        + "ACAAGCCAAACCCCAGAGGTGAAATCGTAATTGGTGGACAGAACATCTCCATGGGATATTTTAAAAATGAAGAGAAAACAGCAGAAGATTATTCTGTGGATGAAAATGGAC"
                        + "AAAGGAACTTGGGTTGATATCTGCAATAATCCTGCTATGGAAGCTGAAATACTGAAAGAAATTCGAGAAGCTGCAAATGCCATGAAATTGGAGCGATTTGAAATTCCAATC"
                        + "AAGGTTCGATTAAGCCCAGAGCCATGGACCCCTGAAACTGGTTTGGTAACTGATGCTTTCAAACTGAAAAGGAAGGAGCTGAGGAACCATTACCTCAAAGACATTGAACGA"
                        + "ATGTATGGGGGCAAATAAAAT";

        String actual = predictor.getAlternateDnaSequence();
        Assert.assertEquals(expected, actual);
    }


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