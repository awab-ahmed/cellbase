package org.opencb.cellbase.core.variant.annotation.hgvs;

import org.apache.commons.lang3.StringUtils;
import org.opencb.biodata.models.core.Exon;
import org.opencb.biodata.models.core.Transcript;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.cellbase.core.variant.annotation.VariantAnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Predicts AA sequence given a transcript and a variant.
 */
public class HgvsProteinCalculator {

    protected static Logger logger = LoggerFactory.getLogger(HgvsProteinCalculator.class);

    private final Variant variant;
    private final Transcript transcript;
    private TranscriptUtils transcriptUtils;
    // protein sequence updated with variation
    private StringBuilder alternateProteinSequence;

    private static final String MT = "MT";
    private static final String STOP_STRING = "STOP";
    private static final String FRAMESHIFT_SUFFIX = "fs";
    private static final String EXTENSION_TAG = "ext";
    private static final String UNKOWN_STOP_CODON_POSITION = "*?";
    private static final String TERMINATION_SUFFIX = "Ter";
    private static final String INS_SUFFIX = "ins";
    private static final String DUP_SUFFIX = "dup";
    private static final char STOP_CODON_INDICATOR = '*';
    private static final String UNIPROT_LABEL = "uniprotkb/swissprot";
    protected BuildingComponents buildingComponents = null;


    /**
     * Constructor.
     *
     * @param variant variant of interest. Can be SNV, DEL or INS
     * @param transcript transcript containing variant
     */
    public HgvsProteinCalculator(Variant variant, Transcript transcript) {
        this.variant = variant;
        this.transcript = transcript;
        this.transcriptUtils =  new TranscriptUtils(transcript);
    }

    /**
     * For the given variant and transcript, return the correctly formatted HGVSp string.
     *
     * @return HGVSp string for variant and transcript
     */
    public HgvsProtein calculate() {
        // FIXME  restore !onlySpansCodingSequence(variant, transcript) check
        if (!transcriptUtils.isCoding() || transcript.getProteinSequence() == null) {
            return null;
        }

        switch (this.variant.getType()) {
            case SNV:
                return this.calculateSnvHgvs();
            case INDEL:
                // insertion
                if (StringUtils.isBlank(variant.getReference())) {
                    return calculateIndelHgvs();
                // deletion
                } else if (StringUtils.isBlank(variant.getAlternate())) {
                    return calculateDeletionHgvs();
                } else {
                    logger.debug("No HGVS implementation available for variant MNV. Returning empty list of HGVS "
                            + "identifiers.");
                    return null;
                }
            case INSERTION:
                return this.calculateInsertionHgvs();
            case DELETION:
                return calculateDeletionHgvs();
            default:
                // TODO throw an error?
                logger.error("Don't know how to handle variant of type {}", variant.getType());
                return null;
        }
    }

    private HgvsProtein calculateSnvHgvs() {
        String alternate = transcript.getStrand().equals("+") ? variant.getAlternate() : reverseComplementary(variant.getAlternate());

        // Step 1 - Get Aminoacid change. For SNV we just need to replace the nucleotide.
        int cdsVariantStartPosition = HgvsCalculator.getCdsStart(transcript, variant.getStart());
        int codonPosition = transcriptUtils.getCodonPosition(cdsVariantStartPosition);
        String referenceCodon = transcriptUtils.getCodon(codonPosition);
        // three letter abbreviation
        String referenceAminoacid = VariantAnnotationUtils
                .buildUpperLowerCaseString(VariantAnnotationUtils
                .getAminoacid(VariantAnnotationUtils.MT.equals(transcript.getChromosome()), referenceCodon));

        int positionAtCodon = transcriptUtils.getPositionAtCodon(cdsVariantStartPosition);
        char[] chars = referenceCodon.toCharArray();
        chars[positionAtCodon - 1] = alternate.charAt(0);
        // three letter abbreviation
        String alternateAminoacid = VariantAnnotationUtils.buildUpperLowerCaseString(
                VariantAnnotationUtils.getAminoacid(VariantAnnotationUtils.MT.equals(transcript.getChromosome()), new String(chars)));

        // Step 2 -
        String hgvsString;
        if (referenceAminoacid.equals(alternateAminoacid)) {
            // SILENT mutation, this includes one STOP codon to another STOP codon
            hgvsString = "p." + referenceAminoacid + codonPosition + "=";
        } else {    // Different aminocacid, several scenarios
            if (referenceCodon.equalsIgnoreCase("STOP")) {
                // STOP lost     --> extension
                //  call to frameshift
                hgvsString = "";
            } else if (alternateAminoacid.equalsIgnoreCase("STOP")) {
                // NONSENSE
                hgvsString = "p." + referenceAminoacid + codonPosition + "Ter";
            } else {
                if ("Met".equals(referenceAminoacid) && codonPosition == 1) {
                    // start lost
                    hgvsString = "p.Met1?";
                } else {
                    // MISSENSE --> Substitution
                    hgvsString = "p." + referenceAminoacid + codonPosition + alternateAminoacid;
                }
            }
        }

        alternateProteinSequence = new StringBuilder(transcript.getProteinSequence());
        alternateProteinSequence.setCharAt(codonPosition, alternateAminoacid.charAt(0));
        List<String> proteinIds = Arrays.asList(transcript.getProteinID(), transcriptUtils.getXrefId(UNIPROT_LABEL));

        // Debug
        System.out.println("cdsVariantStartPosition = " + cdsVariantStartPosition);
        System.out.println("codonPosition = " + codonPosition);
        System.out.println("referenceCodon = " + referenceCodon);
        System.out.println("referenceAminoacid = " + referenceAminoacid);
        System.out.println("positionAtCodon = " + positionAtCodon);
        System.out.println("alternateAminoacid = " + alternateAminoacid);

        System.out.println("Reference:\n" + transcriptUtils.getFormattedCdnaSequence());
//        System.out.println();
//        System.out.println("Alternate:\n" + transcriptUtils.getFormattedCdnaSequence());
        System.out.println(alternateProteinSequence);

        return new HgvsProtein(proteinIds, hgvsString, alternateProteinSequence.toString());
    }

    /**
     * This method can produce 3 different HGVS mutations:
     *
     * HGVS Insertion: a sequence change between the translation initiation (start) and termination (stop) codon where,
     * compared to the reference sequence, one or more amino acids are inserted,
     * which is not a frame shift and where the insertion is not a copy of a sequence immediately N-terminal (5')
     *
     * HGVS Duplication: a sequence change between the translation initiation (start) and termination (stop) codon where, compared to
     * a reference sequence, a copy of one or more amino acids are inserted directly C-terminal of the original copy of that sequence.
     *
     * HGVS Frame shift: a sequence change between the translation initiation (start) and termination (stop) codon where,
     * compared to a reference sequence, translation shifts to another reading frame.
     *
     * @return
     */
    private HgvsProtein calculateInsertionHgvs() {
        // Insertion must fall inside the coding region
        int cdsVariantStartPosition = HgvsCalculator.getCdsStart(transcript, variant.getStart());
        if (cdsVariantStartPosition < 1 || cdsVariantStartPosition > transcript.getCdsLength()) {
            return null;
        }

        // Check if this is an in frame insertion
        int positionAtCodon = transcriptUtils.getPositionAtCodon(cdsVariantStartPosition);
        if (positionAtCodon == 1 && variant.getAlternate().length() % 3 == 0) {
            // Get variant alternate in the same strand than the transcript
            String alternate = transcript.getStrand().equals("+") ? variant.getAlternate() : reverseComplementary(variant.getAlternate());

            if (alternate != null) {
                String hgvsString;

                // Get amino acid sequence of the insertion
                List<String> aminoacids =  new ArrayList<>(alternate.length() / 3);
                List<String> codedAminoacids =  new ArrayList<>(alternate.length() / 3);
                String alternateCodon;
                for (int i = 0; i < alternate.length(); i += 3) {
                    alternateCodon = alternate.substring(i, i + 3);
                    String alternateAa = VariantAnnotationUtils.getAminoacid(MT.equals(variant.getChromosome()), alternateCodon);
                    aminoacids.add(alternateAa);
                    codedAminoacids.add(VariantAnnotationUtils.TO_ABBREVIATED_AA.get(alternateAa));
                }

                // Get position and flanking amino acids
                int codonPosition = transcriptUtils.getCodonPosition(cdsVariantStartPosition);
                int aminoacidPosition = codonPosition;
                String leftCodedAa = transcript.getProteinSequence().substring(codonPosition - 1, codonPosition);
                String leftAa = VariantAnnotationUtils.TO_LONG_AA.get(leftCodedAa);
                String rightCodedAa = transcript.getProteinSequence().substring(codonPosition, codonPosition + 1);
                String rightAa = VariantAnnotationUtils.TO_LONG_AA.get(rightCodedAa);

                // Create HGVS string
                // IMPORTANT: Check if this is a HGVS Duplication instead of a HGVS Insertion
                String previousSequence = transcript.getProteinSequence().substring(codonPosition - aminoacids.size() - 1, codonPosition - 1);
                if (previousSequence.equals(StringUtils.join(codedAminoacids, ""))) {
                    // HGVS Duplication: a sequence change between the translation initiation (start) and termination (stop) codon where,
                    // compared to a reference sequence, a copy of one or more amino acids are inserted directly C-terminal
                    // of the original copy of that sequence.
                    // Examples:
                    // p.Ala3dup (one amino acid)
                    //  a duplication of amino acid Ala3 in the sequence MetGlyAlaArgSerSerHis to MetGlyAlaAlaArgSerSerHis
                    // p.Ala3_Ser5dup (several amino acids)
                    //  a duplication of amino acids Ala3 to Ser5 in the sequence MetGlyAlaArgSerSerHis to MetGlyAlaArgSerAlaArgSerSerHis

                    String aaAfterDuplication = transcript.getProteinSequence()
                            .substring(aminoacidPosition + aminoacids.size(), aminoacidPosition + aminoacids.size() + 1);
                    while (transcript.getProteinSequence().substring(aminoacidPosition, aminoacidPosition + 1).equals(aaAfterDuplication)) {
                        aminoacidPosition += codedAminoacids.size();
                        aaAfterDuplication = transcript.getProteinSequence()
                                .substring(aminoacidPosition + aminoacids.size(), aminoacidPosition + aminoacids.size() + 1);
                    }

                    if (aminoacids.size() == 1) {
                        hgvsString = "p." + leftAa + codonPosition + "dup";
                    } else {
                        hgvsString = "p." + leftAa + codonPosition + "_" + rightAa + (codonPosition + aminoacids.size() - 1) + "dup";
                    }
                } else {
                    // HGVS Insertion: a sequence change between the translation initiation (start) and termination (stop) codon where,
                    // compared to the reference sequence, one or more amino acids are inserted, which is not a frame shift and
                    // where the insertion is not a copy of a sequence immediately N-terminal (5')
                    if (aminoacids.size() <= 5) {
                        // p.Lys2_Gly3insGlnSerLys
                        //  the insertion of amino acids GlnSerLys between amino acids Lys2 and Gly3
                        //  changing MetLysGlyHisGlnGlnCys to MetLysGlnSerLysGlyHisGlnGlnCys
                        hgvsString = "p." + leftAa + (codonPosition - 1) + "_"
                                + rightAa + codonPosition + "ins" + StringUtils.join(aminoacids, "");
                    } else {
                        // Check if last amino acid inserted is a STOP codon:
                        int stopPosition = aminoacids.indexOf("STOP") + 1;
                        if (stopPosition > 0) {
                            // NP_060250.2:p.Gln746_Lys747ins*63
                            //  the in-frame insertion of a 62 amino acid sequence ending at a stop codon at position *63 between
                            //  amino acids Gln746 and Lys747.
                            //  NOTE: it must be possible to deduce the inserted amino acid sequence from the description given
                            //  at DNA or RNA level
                            hgvsString = "p." + leftAa + (codonPosition - 1) + "_"
                                    + rightAa + codonPosition + "ins" + "*" + stopPosition;
                        } else {
                            // p.Arg78_Gly79ins23
                            //  the in-frame insertion of a 23 amino acid sequence between amino acids Arg78 and Gly79
                            //  NOTE: it must be possible to deduce the 23 inserted amino acids from the description at DNA or RNA level
                            hgvsString = "p." + leftAa + (codonPosition - 1) + "_"
                                    + rightAa + codonPosition + "ins" + aminoacids.size();
                        }
                    }
                }

                StringBuilder alternateProteinSequence = new StringBuilder(transcript.getProteinSequence());
                alternateProteinSequence.insert(codonPosition - 1, StringUtils.join(codedAminoacids, ""));
                List<String> proteinIds = Arrays.asList(transcript.getProteinID(), transcriptUtils.getXrefId(UNIPROT_LABEL));
                return new HgvsProtein(proteinIds, hgvsString, alternateProteinSequence.toString());
            } else {
                // no valid alternate provided, exception?
                return null;
            }
        } else {
            // call to frameshift
            return null;
        }
    }

    /**
     * This method can produce 2 different HGVS mutations:
     *
     * HGVS Deletion: a sequence change between the translation initiation (start) and termination (stop) codon where,
     * compared to a reference sequence, one or more amino acids are not present (deleted)
     *
     * HGVS Frame shift: a sequence change between the translation initiation (start) and termination (stop) codon where,
     * compared to a reference sequence, translation shifts to another reading frame.
     *
     * @return
     */
    private HgvsProtein calculateDeletionHgvs() {
        // Deletion must fall inside the coding region
        int cdsVariantStartPosition = HgvsCalculator.getCdsStart(transcript, variant.getStart());
        if (cdsVariantStartPosition < 1 || cdsVariantStartPosition > transcript.getCdsLength()) {
            return null;
        }

        // Check if this is an in frame deletion
        int positionAtCodon = transcriptUtils.getPositionAtCodon(cdsVariantStartPosition);
        if (positionAtCodon == 1 && variant.getReference().length() % 3 == 0) {
            String hgvsString;

            // Prepare variables
            int codonPosition = transcriptUtils.getCodonPosition(cdsVariantStartPosition);
            int aminoacidPosition = codonPosition;
//            if (transcript.getProteinSequence().startsWith("X")) {
//                aminoacidPosition++; // ??
//            }
            int deletionLength = variant.getReference().length() / 3;


            // Check for a very special case when the first Met1 is deleted but is flanked by another Met:
            String nextRightAa = transcript.getProteinSequence()
                    .substring(aminoacidPosition + deletionLength + 1, aminoacidPosition + deletionLength + 2);
            if (aminoacidPosition == 1 && nextRightAa.equals("M")) {
                // p.Gly2_Met46del
                //  a deletion of amino acids Gly2 to Met46 as a consequence of a variant silencing translation initiation ate Met1 but
                //  activating a new downstream translation initiation site (at Met46)
                //  NOTE: the 3’ rule has been applied.
                aminoacidPosition = 2;
                String leftCodedAa = transcript.getProteinSequence().substring(aminoacidPosition, aminoacidPosition + 1);
                String leftAa = VariantAnnotationUtils.TO_LONG_AA.get(leftCodedAa);
                String rightCodedAa = transcript.getProteinSequence()
                        .substring(aminoacidPosition + deletionLength, aminoacidPosition + deletionLength + 1);
                String rightAa = VariantAnnotationUtils.TO_LONG_AA.get(rightCodedAa);
                hgvsString = "p." + leftAa + aminoacidPosition + "_ " + rightAa + (aminoacidPosition + deletionLength - 1) + "del";
            } else {
                // Get HGVS for the normal Deletion
                List<String> aminoacids =  new ArrayList<>(variant.getReference().length() / 3);
                List<String> codedAminoacids =  new ArrayList<>(variant.getReference().length() / 3);
                String alternateCodon;
                for (int i = 0; i < variant.getReference().length(); i += 3) {
                    alternateCodon = variant.getReference().substring(i, i + 3);
                    String alternateAa = VariantAnnotationUtils.getAminoacid(MT.equals(variant.getChromosome()), alternateCodon);
                    aminoacids.add(alternateAa);
                    codedAminoacids.add(VariantAnnotationUtils.TO_ABBREVIATED_AA.get(alternateAa));
                }

                // Move to the C-terminal (right) as much as possible in both single and multiple amino acid deletion:
                // LRG_199p1:p.Trp4del  (1 amino acid)
                //  a deletion of amino acid Trp4 in the sequence MetLeuTrpTrpGlu to MetLeuTrp_Glu
                //  NOTE: for deletions in single amino acid stretches or tandem repeats, the most C-terminal residue is arbitrarily
                //  assigned to have been deleted
                // LRG_199p1:p.Trp4del  (more than 1 amino acid)
                //  a deletion of amino acid Trp4 in the sequence MetLeuTrpTrpGlu to MetLeuTrp_Glu
                //  NOTE: for deletions in single amino acid stretches or tandem repeats, the most C-terminal residue is arbitrarily
                //  assigned to have been deleted
//                while (transcript.getProteinSequence().substring(aminoacidPosition, aminoacidPosition + codedAminoacids.size())
//                        .equals(StringUtils.join(codedAminoacids, ""))) {
//                    aminoacidPosition += codedAminoacids.size();
//                }
                String aaAfterDeletion = transcript.getProteinSequence()
                        .substring(aminoacidPosition + deletionLength, aminoacidPosition + deletionLength + 1);
                while (transcript.getProteinSequence().substring(aminoacidPosition, aminoacidPosition + 1).equals(aaAfterDeletion)) {
                    aminoacidPosition += codedAminoacids.size();
                    aaAfterDeletion = transcript.getProteinSequence()
                            .substring(aminoacidPosition + deletionLength, aminoacidPosition + deletionLength + 1);
                }

                // Get flanking amino acids
                String leftCodedAa = transcript.getProteinSequence().substring(codonPosition - 1, codonPosition);
                String leftAa = VariantAnnotationUtils.TO_LONG_AA.get(leftCodedAa);

                // Only 1 amino acid deleted
                if (aminoacids.size() == 1) {
                    // LRG_199p1:p.Val7del
                    //  a deletion of amino acid Val7 in the reference sequence LRG_199p1
                    hgvsString = "p." + leftAa + aminoacidPosition + "del";
                } else {
                    // More than 1 amino acid deleted, calculate the right flank
                    String rightCodedAa = transcript.getProteinSequence()
                            .substring(aminoacidPosition + aminoacids.size(), aminoacidPosition + aminoacids.size() + 1);
                    String rightAa = VariantAnnotationUtils.TO_LONG_AA.get(rightCodedAa);

                    // NP_003997.1:p.Lys23_Val25del
                    //  a deletion of amino acids Lys23 to Val25 in reference sequence NP_003997.1
                    hgvsString = "p." + leftAa + aminoacidPosition + "_ " + rightAa + (aminoacidPosition + aminoacids.size() - 1) + "del";
                }
            }

            StringBuilder alternateProteinSequence = new StringBuilder(transcript.getProteinSequence());
            alternateProteinSequence.replace(aminoacidPosition, aminoacidPosition + deletionLength, "");
            List<String> proteinIds = Arrays.asList(transcript.getProteinID(), transcriptUtils.getXrefId(UNIPROT_LABEL));
            return new HgvsProtein(proteinIds, hgvsString, alternateProteinSequence.toString());
        } else {
            // call to frameshift
            return null;
        }
    }

    private HgvsProtein calculateIndelHgvs() {
        getAlternateProteinSequence();

        return calculateHgvsString();
    }


    private void getAlternateProteinSequence() {
        alternateProteinSequence = new StringBuilder();
        buildingComponents = new BuildingComponents();

        int phaseOffset = 0;
        // current position in the protein string. JAVIER:  int aaPosition = ((codonPosition - 1) / 3) + 1;
        int currentAaIndex = 0;
        if (transcriptUtils.hasUnconfirmedStart()) {
            phaseOffset = transcriptUtils.getFirstCodonPhase();

            // if reference protein sequence start with X, prepend X to our new alternate sequence also
            if (transcript.getProteinSequence().startsWith(HgvsCalculator.UNKNOWN_AMINOACID)) {
                alternateProteinSequence.append("X");
                currentAaIndex++;
            }
        }

        String alternateDnaSequence = getAlternateCdnaSequence();

        int variantCdnaPosition = transcript.getCdnaCodingStart() + HgvsCalculator.getCdsStart(transcript, variant.getStart());
//        int variantCdnaPosition = HgvsCalculator.getCdsStart(transcript, variant.getStart());
        System.out.println("variantCdnaPosition = " + HgvsCalculator.getCdsStart(transcript, variant.getStart()));
        System.out.println("variantCdnaPosition = " + variantCdnaPosition);

        // Initial codon position. Index variables are always 0-based for working with strings
        int codonIndex = transcript.getCdnaCodingStart() + phaseOffset - 1;
        int terPosition = 0;

        System.out.println(transcript.getcDnaSequence());
        System.out.println(alternateDnaSequence);
        // Loop through cDNA translating each codon
        while (alternateDnaSequence.length() > codonIndex + 3) {
//            String referecenCodon = transcript.getcDnaSequence().substring(codonIndex, codonIndex + 3);
//            String referenceAa = VariantAnnotationUtils.getAminoacid(MT.equals(variant.getChromosome()), referecenCodon);

            String alternateCodon = alternateDnaSequence.substring(codonIndex, codonIndex + 3);
            // Three letter AA, eg. PHE and single letter AA, eg. L
            String alternateAa = VariantAnnotationUtils.getAminoacid(MT.equals(variant.getChromosome()), alternateCodon);
            String alternateCodedAa = VariantAnnotationUtils.TO_ABBREVIATED_AA.get(alternateAa);

            // build the new sequence
            alternateProteinSequence.append(alternateCodedAa);

            String referenceCodedAa =  null;
            // Alternate protein can miss a STOP codon and be longer than the reference protein
            if (transcript.getProteinSequence().length() > currentAaIndex) {
                referenceCodedAa = String.valueOf(transcript.getProteinSequence().charAt(currentAaIndex));
            }

            if (terPosition > 0) {
                terPosition++;
            }

            if (STOP_STRING.equals(alternateAa)) {
                if (terPosition > 0) {
                    buildingComponents.setTerminator(terPosition);
                } else {
                    // if terPosition is 0 means the
                    buildingComponents.setStart(currentAaIndex + 1);
                    buildingComponents.setEnd(currentAaIndex + 1);
//                    buildingComponents.setReferenceStart(referenceAa);
//                    buildingComponents.setReferenceEnd(referenceAa);
                    buildingComponents.setAlternate(STOP_STRING);
                    buildingComponents.setTerminator(-1);
                    buildingComponents.setMutationType(BuildingComponents.MutationType.SUBSTITUTION);
                }
                break;
                // we found first different amino acid
            } else {
                // if terminator position is 0, we haven't found the first different AA yet
                if (terPosition == 0 && !alternateCodedAa.equals(referenceCodedAa)) {
                    buildingComponents.setAlternate(alternateAa);
                    // put back to 1 - base
                    int start = currentAaIndex + 1;
                    buildingComponents.setStart(start);
                    // FIXME valid only for insertions?
                    buildingComponents.setEnd(start - 1);
                    terPosition = 1;
                }
            }

            // move to the next letter
            currentAaIndex++;
            // move to next codon
            codonIndex = codonIndex + 3;
        }

        // Debug
        System.out.println("Reference:\n" + transcriptUtils.getFormattedCdnaSequence());
        System.out.println();
        System.out.println("Alternate:\n" + transcriptUtils.getFormattedCdnaSequence());
        System.out.println(alternateProteinSequence);

        populateBuildingComponents();
    }


    private void populateBuildingComponents() {

        buildingComponents.setProteinId(transcript.getProteinID());

        int start = buildingComponents.getStart();
        int end = buildingComponents.getEnd();

        String referenceStartShortSymbol = String.valueOf(transcript.getProteinSequence().charAt(end - 1));

        // Might get out of sequence boundaries after right aligning resulting in a protein extension. Will then
        // set referenceEndShortSymbol to null to enable following if to pass
        String referenceEndShortSymbol
                = start == (transcript.getProteinSequence().length() + 1)
                ? null
                : String.valueOf(transcript.getProteinSequence().charAt(start - 1));

//        String referenceStartShortSymbol = buildingComponents.getReferenceStart();
//        String referenceEndShortSymbol = buildingComponents.getReferenceEnd();

        // Do not generate protein HGVS if insertion affects an unconfirmed start, i.e. overlaps with an "X"
        // symbol in the protein sequence
        if (VariantAnnotationUtils.TO_LONG_AA.containsKey(referenceStartShortSymbol)
                && (referenceEndShortSymbol == null
                || VariantAnnotationUtils.TO_LONG_AA.containsKey(referenceEndShortSymbol))) {


            buildingComponents.setReferenceStart(VariantAnnotationUtils.buildUpperLowerCaseString(VariantAnnotationUtils
                    .TO_LONG_AA.get(referenceStartShortSymbol)));

            // null if the insertion is an extension, i.e. start is the next position after the last aa in the
            // protein sequence
            buildingComponents.setReferenceEnd(referenceEndShortSymbol == null
                    ? null
                    : VariantAnnotationUtils.buildUpperLowerCaseString(VariantAnnotationUtils
                    .TO_LONG_AA.get(referenceEndShortSymbol)));

            BuildingComponents.MutationType mutationType = getMutationType();
            buildingComponents.setMutationType(mutationType);
            buildingComponents.setKind(isFrameshift()
                    ? BuildingComponents.Kind.FRAMESHIFT
                    : BuildingComponents.Kind.INFRAME);

        }
    }

    private BuildingComponents.MutationType getMutationType() {

        String proteinSequence = transcript.getProteinSequence();
        int start = buildingComponents.getStart();
        String alternate = buildingComponents.getAlternate();

        // Insertion at stop codon - extension
        // Recall proteinVariant.getStart() is base 1
        if (start == (proteinSequence.length() + 1)) {
            return BuildingComponents.MutationType.EXTENSION;
        }

        // If stop gained then skip any normalisation; If there's a stop gain the stop indicator will always be the last
        // element of the predicted sequence, as prediction stops as soon as a STOP codon is found
        if (STOP_CODON_INDICATOR == alternate.charAt(alternate.length() - 1)) {
            return BuildingComponents.MutationType.STOP_GAIN;
        }

        if (STOP_STRING.equals(alternate)) {
            return BuildingComponents.MutationType.STOP_GAIN;
        }

        // Check duplication
        // TODO: Assuming end = start - 1; might need adjusting
        String previousSequence = proteinSequence.substring(Math.max(0, start - alternate.length() - 1), start - 1);
        // normalized one in order to take into
        // account potential
        // normalization/lef-right alignment
        // differences
        if (previousSequence.equals(alternate)) {
            return BuildingComponents.MutationType.DUPLICATION;
        }

        return BuildingComponents.MutationType.INSERTION;
    }

    /**
     * Using the variant and transcript provided, builds the HGVS protein string.
     *
     * @return protein HGVS string
     */
    private HgvsProtein calculateHgvsString() {

        // wasn't able to process sequence, won't be able to build hgvs string
        if (buildingComponents == null) {
            return null;
        }

        StringBuilder hgvsString = (new StringBuilder(buildingComponents.getProteinId()))
                .append(HgvsCalculator.PROTEIN_CHAR);

        if (BuildingComponents.MutationType.DUPLICATION.equals(buildingComponents.getMutationType())) {
            if (buildingComponents.getAlternate().length() == 1) {
                // assuming end = start - 1
                hgvsString.append(VariantAnnotationUtils
                        .buildUpperLowerCaseString(VariantAnnotationUtils
                                .TO_LONG_AA.get(String.valueOf(buildingComponents.getAlternate().charAt(0)))))
                        .append(buildingComponents.getEnd());
            } else {
                // assuming end = start - 1
                hgvsString.append(VariantAnnotationUtils
                        .buildUpperLowerCaseString(VariantAnnotationUtils
                                .TO_LONG_AA.get(String.valueOf(buildingComponents.getAlternate().charAt(0)))))
                        .append(buildingComponents.getStart() - buildingComponents.getAlternate().length())
                        .append(HgvsCalculator.UNDERSCORE)
                        .append(VariantAnnotationUtils
                                .buildUpperLowerCaseString(VariantAnnotationUtils
                                        .TO_LONG_AA.get(String.valueOf(buildingComponents
                                                .getAlternate()
                                                .charAt(buildingComponents.getAlternate().length() - 1)))))
                        .append(buildingComponents.getEnd());
            }
            hgvsString.append(DUP_SUFFIX);
//            .append(buildingComponents.getAlternate())


        } else {
            if (BuildingComponents.MutationType.EXTENSION.equals(buildingComponents.getMutationType())) {
                try {
                    hgvsString.append(TERMINATION_SUFFIX)
                            .append(buildingComponents.getStart())
                            .append(VariantAnnotationUtils
                                    .buildUpperLowerCaseString(VariantAnnotationUtils
                                            .TO_LONG_AA.get(String.valueOf(buildingComponents.getAlternate().charAt(0)))))
                            .append(EXTENSION_TAG)
                            .append(UNKOWN_STOP_CODON_POSITION);
                } catch (NullPointerException e) {
                    int a = 1;
                    throw e;
                }

            } else {
                if (BuildingComponents.Kind.FRAMESHIFT.equals(buildingComponents.getKind())) {
                    if (BuildingComponents.MutationType.STOP_GAIN.equals(buildingComponents.getMutationType())
                            && buildingComponents.getTerminator() < 0) {
                        hgvsString.append(buildingComponents.getReferenceEnd())
                                .append(buildingComponents.getStart())
                                .append(TERMINATION_SUFFIX);
                    } else {
                        // Appends aa name properly formated; first letter uppercase, two last letters lowercase e.g. Arg
                        hgvsString.append(buildingComponents.getReferenceEnd())
                                .append(buildingComponents.getStart())
                                .append(VariantAnnotationUtils.buildUpperLowerCaseString(buildingComponents.getAlternate()))
                                .append(FRAMESHIFT_SUFFIX)
                                .append(TERMINATION_SUFFIX);

                        if (buildingComponents.getTerminator() > 0) {
//                stringBuilder.append(FRAMESHIFT_SUFFIX);
                            hgvsString.append(buildingComponents.getTerminator());
                        } else {
                            hgvsString.append("?");
                        }
                    }

                } else {
                    if (BuildingComponents.MutationType.STOP_GAIN.equals(buildingComponents.getMutationType())) {
                        hgvsString.append(buildingComponents.getReferenceEnd())
                                .append(buildingComponents.getStart())
                                .append(TERMINATION_SUFFIX);
                    } else {
                        // assuming end = start - 1
                        hgvsString.append(buildingComponents.getReferenceStart())
                                .append(buildingComponents.getEnd())
                                .append(HgvsCalculator.UNDERSCORE)
                                .append(buildingComponents.getReferenceEnd())
                                .append(buildingComponents.getStart())
                                .append(INS_SUFFIX)
                                .append(formatAaSequence(buildingComponents.getAlternate()));
                    }
                }
            }
        }

        String alternateProteinSequence = getAlternateCdnaSequence();
        List<String> proteinIds = Arrays.asList(transcript.getProteinID(), transcriptUtils.getXrefId(UNIPROT_LABEL));
        return new HgvsProtein(proteinIds, hgvsString.toString(), alternateProteinSequence);
    }

    /**
     *
     * @return the DNA sequence updated with the alternate sequence
     */
    protected String getAlternateCdnaSequence() {
        StringBuilder alternateDnaSequence = new StringBuilder(transcript.getcDnaSequence());

        String reference = variant.getReference();
        String alternate = variant.getAlternate();

        if (this.transcript.getStrand().equals("-")) {
            alternate = reverseComplementary(alternate);
        }

        // genomic to cDNA
        int variantCdsPosition = HgvsCalculator.getCdsStart(transcript, variant.getStart());
        // -1 to fix inclusive positions
        int cdnaVariantPosition = transcript.getCdnaCodingStart() + variantCdsPosition - 1;
        int cdnaVariantIndex = cdnaVariantPosition - 1;

        System.out.println("cdnaVariantIndex = " + cdnaVariantIndex);
        switch (variant.getType()) {
            case SNV:
                alternateDnaSequence.setCharAt(cdnaVariantIndex, alternate.charAt(0));
                break;
            case INDEL:
                // insertion
                if (StringUtils.isBlank(variant.getReference())) {
                    alternateDnaSequence.insert(cdnaVariantIndex, alternate);
                // deletion
                } else if (StringUtils.isBlank(variant.getAlternate())) {
                    alternateDnaSequence.replace(cdnaVariantIndex, cdnaVariantIndex + reference.length(), "");
                } else {
                    logger.debug("No HGVS implementation available for variant MNV.");
                    return null;
                }
                break;
            default:
                logger.debug("No HGVS implementation available for structural variants. Found {}.", variant.getType());
                return null;
        }
        return alternateDnaSequence.toString();
    }

    protected char aaAt(int position) {
        return alternateProteinSequence.subSequence(position, position).charAt(0);
    }

    /**
     *
     * @return the AA sequence for the alternate
     */
    protected String getSequence() {
        return alternateProteinSequence.toString();
    }

    protected String getSequence(int start, int end) {
        return alternateProteinSequence.subSequence(start, end).toString();
    }

    private String formatAaSequence(String alternate) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < alternate.length(); i++) {
            stringBuilder.append(VariantAnnotationUtils
                    .buildUpperLowerCaseString(VariantAnnotationUtils.TO_LONG_AA
                            .get(String.valueOf(alternate.charAt(i)))));
        }
        return stringBuilder.toString();
    }

    // FIXME should be variant.start - cdnastart % 3 == 0, to be safe
    private boolean isFrameshift() {
        return !(variant.getAlternate().length() % 3 == 0);
    }

    // FIXME need to do this check early
    protected boolean onlySpansCodingSequence(Variant variant, Transcript transcript) {
        if (buildingComponents.getCdnaStart().getOffset() == 0  // Start falls within coding exon
                && buildingComponents.getCdnaEnd().getOffset() == 0) { // End falls within coding exon

            List<Exon> exonList = transcript.getExons();
            // Get the closest exon to the variant start, measured as the exon that presents the closest start OR end
            // coordinate to the position
            Exon nearestExon = exonList.stream().min(Comparator.comparing(exon ->
                    Math.min(Math.abs(variant.getStart() - exon.getStart()),
                            Math.abs(variant.getStart() - exon.getEnd())))).get();

            // Check if the same exon contains the variant end
            return variant.getEnd() >= nearestExon.getStart() && variant.getEnd() <= nearestExon.getEnd();

        }
        return false;
    }


    private String reverseComplementary(String string) {
        StringBuilder stringBuilder = new StringBuilder(string).reverse();
        for (int i = 0; i < stringBuilder.length(); i++) {
            char nextNt = stringBuilder.charAt(i);
            // Protection against weird characters, e.g. alternate:"TBS" found in ClinVar
            if (VariantAnnotationUtils.COMPLEMENTARY_NT.containsKey(nextNt)) {
                stringBuilder.setCharAt(i, VariantAnnotationUtils.COMPLEMENTARY_NT.get(nextNt));
            } else {
                return null;
            }
        }
        return stringBuilder.toString();
    }

}
