/*
 * Copyright 2015-2020 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.cellbase.lib.builders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang3.StringUtils;
import org.opencb.biodata.models.pharma.*;
import org.opencb.cellbase.core.serializer.CellBaseFileSerializer;
import org.opencb.commons.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.opencb.cellbase.lib.EtlCommons.PHARMGKB_DATA;
import static org.opencb.cellbase.lib.EtlCommons.PHARMGKB_NAME;

public class PharmGKBBuilder extends CellBaseBuilder {

    private final Path inputDir;
    private final Path pharmGKBDir;

    private static final String CHEMICALS_BASENAME = "chemicals";
    private static final String CHEMICALS_TSV_FILENAME = "chemicals.tsv";

    private static final String VARIANTS_BASENAME = "variants";
    private static final String VARIANTS_TSV_FILENAME = "variants.tsv";

    private static final String CLINICAL_ANNOTATIONS_BASENAME = "clinicalAnnotations";
    private static final String CLINICAL_ANNOTATIONS_TSV_FILENAME = "clinical_annotations.tsv";
    private static final String CLINICAL_ANN_ALLELES_TSV_FILENAME = "clinical_ann_alleles.tsv";
    private static final String CLINICAL_ANN_EVIDENCE_TSV_FILENAME = "clinical_ann_evidence.tsv";

    private static final String VARIANT_ANNOTATIONS_BASENAME = "variantAnnotations";
    private static final String VARIANT_ANNOTATIONS_TSV_FILENAME = "var_drug_ann.tsv";
    private static final String PHENOTYPE_ANNOTATIONS_TSV_FILENAME = "var_pheno_ann.tsv";
    private static final String FUNCTIONAL_ANNOTATIONS_TSV_FILENAME = "var_fa_ann.tsv";
    private static final String STUDY_PARAMETERS_TSV_FILENAME = "study_parameters.tsv";

    private static final String GUIDELINE_ANNOTATIONS_BASENAME = "guidelineAnnotations";

    private static final String DRUG_LABELS_BASENAME = "drugLabels";
    private static final String DRUG_LABELS_TSV_FILENAME = "drugLabels.tsv";

    private static final String GUIDELINE_ANNOTATION_EVIDENCE_TYPE = "Guideline Annotation";
    private static final String DRUG_LABEL_ANNOTATION_EVIDENCE_TYPE = "Label Annotation";
    private static final String VARIANT_ANNOTATION_EVIDENCE_TYPE = "Variant Drug Annotation";
    private static final String FUNCTIONAL_ANNOTATION_EVIDENCE_TYPE = "Variant Functional Assay Annotation";
    private static final String PHENOTYPE_ANNOTATION_EVIDENCE_TYPE = "Variant Phenotype Annotation";

    private static final String CHROMOSOME_KEY = "chrom";
    private static final String POSITION_KEY = "pos";

    public PharmGKBBuilder(Path inputDir, CellBaseFileSerializer serializer) {
        super(serializer);

        this.inputDir = inputDir;
        this.pharmGKBDir = inputDir.resolve(PHARMGKB_DATA);
    }

    @Override
    public void parse() throws Exception {
        // Check input folder
        FileUtils.checkDirectory(inputDir);

        // PharmGKB
        logger.info("Building PharmGKB data model ...");

        FileUtils.checkDirectory(pharmGKBDir);

        // Chemicals
        Map<String, PharmaChemical> chemicalsMap = parseChemicals();
        logger.info("Number of Chemical items read {}", chemicalsMap.size());

        // Clinical annotation
        parseClinicalAnnotationFiles(chemicalsMap);

//        ObjectMapper mapper = new ObjectMapper();
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(chemicalsMap.get("warfarin")));
//        for (Map.Entry<String, PharmaChemical> entry : chemicalsMap.entrySet()) {
//            System.out.println(entry.getKey());
//        }

        logger.info("Parsing PharmGKB files finished.");
    }

    private Map<String, PharmaChemical> parseChemicals() throws IOException {
        Path chemicalsFile = pharmGKBDir.resolve(CHEMICALS_BASENAME).resolve(CHEMICALS_TSV_FILENAME);
        Map<String, PharmaChemical> chemicalsMap = new HashMap<>();
        try (BufferedReader br = FileUtils.newBufferedReader(chemicalsFile)) {
            // Skip first line, i.e. the header line
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                // 0                      1     2              3            4              5    6                7      8
                // PharmGKB Accession ID  Name  Generic Names  Trade Names  Brand Mixtures Type Cross-references SMILES InChI
                // 9                10                  11                        12                       13            14
                // Dosing Guideline External Vocabulary Clinical Annotation Count Variant Annotation Count Pathway Count VIP Count
                // 15                        16                             17                           18
                // Dosing Guideline Sources  Top Clinical Annotation Level  Top FDA Label Testing Level  Top Any Drug Label Testing Level
                // 19                     20                 21                  22               23
                // Label Has Dosing Info  Has Rx Annotation  RxNorm Identifiers  ATC Identifiers  PubChem Compound Identifiers
                PharmaChemical pharmaChemical = new PharmaChemical()
                        .setId(fields[0])
                        .setSource(PHARMGKB_NAME)
                        .setName(fields[1])
                        .setSmiles(fields[7])
                        .setInChI(fields[8]);

                // Generic Names
                if (StringUtils.isNotEmpty(fields[2])) {
                    pharmaChemical.setGenericNames(stringFieldToList(fields[2]));
                }

                // Trade Names
                if (StringUtils.isNotEmpty(fields[3])) {
                    pharmaChemical.setTradeNames(stringFieldToList(fields[3]));
                }

                // Brand Mixtures
                if (StringUtils.isNotEmpty(fields[4])) {
                    pharmaChemical.setTradeMixtures(stringFieldToList(fields[4]));
                }

                // Types
                if (StringUtils.isNotEmpty(fields[5])) {
                    pharmaChemical.setTypes(Arrays.stream(fields[5].split(",")).map(String::trim).collect(Collectors.toList()));
                }

                // We need to keep the name not the ID to map by drug name in the clinical annotation method
                chemicalsMap.put(pharmaChemical.getName(), pharmaChemical);
            }
        }
        return chemicalsMap;
    }

    /**
     * This method parses clinical_annotations.tsv, then it parses alleles and evidences to add them to the first one.
     * @param chemicalsMap
     * @throws IOException
     */
    private void parseClinicalAnnotationFiles(Map<String, PharmaChemical> chemicalsMap) throws IOException {
        Map<String, PharmaClinicalAnnotation> clinicalAnnotationMap = new HashMap<>();
        Map<String, List<String>> drugToClinicalAnnotationIdMap = new HashMap<>();

        Map<String, Map<String, Object>> variantMap = parseVariants();

        // clinical_annotations.tsv
        try (BufferedReader br = FileUtils.newBufferedReader(pharmGKBDir.resolve(CLINICAL_ANNOTATIONS_BASENAME)
                .resolve(CLINICAL_ANNOTATIONS_TSV_FILENAME))) {
            // Skip first line, i.e. the header line
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t", -1);

                // Sanity check
                if (StringUtils.isEmpty(fields[0])) {
                    logger.warn("Clinical annotation ID is missing in clinical annotations line: {}", line);
                    continue;
                }

                // 0                       1                   2     3                  4               5                6
                // Clinical Annotation ID  Variant/Haplotypes  Gene  Level of Evidence  Level Override  Level Modifiers  Score
                // 7                   8           9               10       11            12                                13
                // Phenotype Category  PMID Count  Evidence Count  Drug(s)  Phenotype(s)  Latest History Date (YYYY-MM-DD)  URL
                // 14
                // Specialty Population
                PharmaClinicalAnnotation clinicalAnnotation = new PharmaClinicalAnnotation()
                        .setVariantId(fields[1])
                        .setGene(fields[2])
                        .setLevelOfEvidence(fields[3])
                        .setLevelOverride(fields[4])
                        .setLevelModifiers(fields[5])
                        .setScore(fields[6])
                        .setPhenotypeCategory(fields[7])
                        .setLatestUpdateDate(fields[12])
                        .setUrl(fields[13])
                        .setSpecialtyPopulation(fields[14]);

                if (StringUtils.isNotEmpty(fields[11])) {
                    clinicalAnnotation.setPhenotypes(stringFieldToList(fields[11]));
                }

                // Add some fields from the variant map
                if (variantMap.containsKey(clinicalAnnotation.getVariantId())) {
                    clinicalAnnotation.setChromosome((String) variantMap.get(clinicalAnnotation.getVariantId()).get(CHROMOSOME_KEY));
                    clinicalAnnotation.setPosition((int) variantMap.get(clinicalAnnotation.getVariantId()).get(POSITION_KEY));
                } else {
                    logger.warn("Variant {} from clinical annotation not found in the variant map, so chromosome and position are not set",
                            clinicalAnnotation.getVariantId());
                }

                // Add the annotation to the annotationMap by annotation ID
                clinicalAnnotationMap.put(fields[0], clinicalAnnotation);

                // Process the drug names to update the drugToClinicalAnnotationId map
                // This will be used at the end of the method to update the chemical map
                if (StringUtils.isNotEmpty(fields[10])) {
                    // Drugs are separated by ;
                    String[] drugs = fields[10].split(";");
                    for (String drug : drugs) {
                        if (!drugToClinicalAnnotationIdMap.containsKey(drug)) {
                            // Add the drug to the map
                            drugToClinicalAnnotationIdMap.put(drug, new ArrayList<>());
                        }
                        // Add the clinical annotation ID to that drug
                        drugToClinicalAnnotationIdMap.get(drug).add(fields[0]);
                    }
                }
            }
        }

        // Update the clinical annotation map by parsing the clinical annotation evidences
        parseClinicalAnnotationEvidences(clinicalAnnotationMap);

        // Update the clinical annotation map by parsing the clinical annotation alleles
        parseClinicalAnnotationAlleles(clinicalAnnotationMap);

        // Update chemicals map by adding the clinical annotation
        for (String drug : drugToClinicalAnnotationIdMap.keySet()) {
            if (chemicalsMap.containsKey(drug)) {
                for (String clinicalAnnotationId : drugToClinicalAnnotationIdMap.get(drug)) {
                    chemicalsMap.get(drug).getVariants().add(clinicalAnnotationMap.get(clinicalAnnotationId));
                }
            } else {
                logger.warn("Drug '{}' not found in the chemicals map", drug);
            }
        }
    }

    private Map<String, Map<String, Object>> parseVariants() throws IOException {
        Map<String, Map<String, Object>> variantMap = new HashMap<>();
        // Parse the variant file (i.e., variants.tsv)
        Path varPath = pharmGKBDir.resolve(VARIANTS_BASENAME).resolve(VARIANTS_TSV_FILENAME);
        try (BufferedReader br = FileUtils.newBufferedReader(varPath)) {
            // Skip first line, i.e. the header line
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                String variantName = fields[1];

                // Sanity check
                if (StringUtils.isEmpty(variantName)) {
                    logger.warn("Variant name is missing in variant line: {}", line);
                    continue;
                }

                if (variantMap.containsKey(variantName)) {
                    logger.warn("Variant name is duplicated in variant line: {}", line);
                    continue;
                }

                // 0           1             2         3             4         5                         6
                // Variant ID  Variant Name  Gene IDs  Gene Symbols  Location  Variant Annotation count  Clinical Annotation count
                // 7                                    8                           9                      10
                // Level 1/2 Clinical Annotation count  Guideline Annotation count  Label Annotation count Synonyms
                String location = fields[4];
                if (StringUtils.isEmpty(location)) {
                    logger.warn("Location is missing for Variant name {}", variantName);
                    continue;
                }
                if (!location.startsWith("NC_")) {
                    logger.warn("Unknown location {}, it has to be a RefSeq ID", location);
                    continue;
                }
                Map<String, Object> attrMap = new HashMap<>();
                String[] splits = location.split("[_\\.:]");
                try {
                    int chrom = Integer.parseInt(splits[1]);
                    if (chrom >= 1 && chrom <= 22) {
                        attrMap.put(CHROMOSOME_KEY, String.valueOf(chrom));
                    } else if (chrom == 23) {
                        attrMap.put(CHROMOSOME_KEY, "X");
                    } else if (chrom == 24) {
                        attrMap.put(CHROMOSOME_KEY, "Y");
                    } else if (chrom == 12920) {
                        attrMap.put(CHROMOSOME_KEY, "MT");
                    } else {
                        logger.warn("Unknown chromosome {}", chrom);
                        continue;
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Error computing chromosome from location {}: {}", location, e.getMessage());
                    continue;
                }
                try {
                    int position = Integer.parseInt(splits[3]);
                    attrMap.put(POSITION_KEY, position);
                } catch (NumberFormatException e) {
                    logger.warn("Error computing chromosome position from location {}: {}", location, e.getMessage());
                    continue;
                }

                // Add it to the variant map
                variantMap.put(variantName, attrMap);
            }
        }
        logger.info("Number of variants = {}", variantMap.size());

        return variantMap;
    }

    private void parseClinicalAnnotationEvidences(Map<String, PharmaClinicalAnnotation> clinicalAnnotationMap) throws IOException {
        // Processing clinical annotation evidences implies to process the variant annotation, guideline annotations,
        // drug label annotations, phenotype annotations and functional analysis annotations
        Map<String, PharmaVariantAssociation> variantAnnotationsMap = parseVariantAnnotations();
        Map<String, PharmaVariantAssociation> phenotypeAnnotationsMap = parsePhenotypeAnnotations();
        Map<String, PharmaVariantAssociation> functionalAnnotationsMap = parseFunctionalAnnotations();
        Map<String, PharmaGuidelineAnnotation> guidelineAnnotationsMap = parseGuidelineAnnotations();
        Map<String, PharmaDrugLabelAnnotation> drugLabelAnnotationsMap = parseDrugLabelAnnotations();

        // Parse study parameters and update the variant, phenotype and functional annotations with the parsed study parameters
        parseStudyParameters(variantAnnotationsMap, phenotypeAnnotationsMap, functionalAnnotationsMap);

        // Parse the clinical annotation alleles file (i.e., clinical_ann_alleles.tsv)
        Path evidencesPath = pharmGKBDir.resolve(CLINICAL_ANNOTATIONS_BASENAME).resolve(CLINICAL_ANN_EVIDENCE_TSV_FILENAME);
        try (BufferedReader br = FileUtils.newBufferedReader(evidencesPath)) {
            // Skip first line, i.e. the header line
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                String clinicalAnnotationId = fields[0];

                // Sanity check
                if (StringUtils.isEmpty(clinicalAnnotationId)) {
                    logger.warn("Clinical annotation ID is missing in clinical annotation evidence line: {}", line);
                    continue;
                }

                // 0                       1            2              3             4     5        6
                // Clinical Annotation ID  Evidence ID  Evidence Type  Evidence URL  PMID  Summary  Score
                String evidenceId = fields[1];
                String evidenceType = fields[2];
                PharmaClinicalEvidence evidence = new PharmaClinicalEvidence()
                        .setType(evidenceType)
                        .setUrl(fields[3])
                        .setPubmed(fields[4])
                        .setSummary(fields[5])
                        .setScore(fields[6]);

                switch (evidenceType) {
                    case VARIANT_ANNOTATION_EVIDENCE_TYPE: {
                        if (variantAnnotationsMap.containsKey(evidenceId)) {
                            // TODO remove this line
                            evidence.getVariantAnnotations().add(variantAnnotationsMap.get(evidenceId));
                            evidence.getVariantAssociations().add(variantAnnotationsMap.get(evidenceId));
                        } else {
                            logger.warn("Evidence ID '{}' of type '{}' not found in the variant annotations map",
                                    evidenceId, VARIANT_ANNOTATION_EVIDENCE_TYPE);
                        }
                        break;
                    }
                    case GUIDELINE_ANNOTATION_EVIDENCE_TYPE: {
                        if (guidelineAnnotationsMap.containsKey(evidenceId)) {
                            evidence.getGuidelineAnnotations().add(guidelineAnnotationsMap.get(evidenceId));
                        } else {
                            logger.warn("Evidence ID '{}' of type '{}' not found in the variant annotations map",
                                    evidenceId, GUIDELINE_ANNOTATION_EVIDENCE_TYPE);
                        }
                        break;
                    }
                    case DRUG_LABEL_ANNOTATION_EVIDENCE_TYPE: {
                        if (drugLabelAnnotationsMap.containsKey(evidenceId)) {
                            evidence.getDrugLabelAnnotations().add(drugLabelAnnotationsMap.get(evidenceId));
                        } else {
                            logger.warn("Evidence ID '{}' of type '{}' not found in the drug label annotations map",
                                    evidenceId, DRUG_LABEL_ANNOTATION_EVIDENCE_TYPE);
                        }
                        break;
                    }
                    case PHENOTYPE_ANNOTATION_EVIDENCE_TYPE: {
                        if (phenotypeAnnotationsMap.containsKey(evidenceId)) {
                            // TODO remove this line
                            evidence.getPhenotypeAnnotations().add(phenotypeAnnotationsMap.get(evidenceId));
                            evidence.getVariantAssociations().add(variantAnnotationsMap.get(evidenceId));
                        } else {
                            logger.warn("Evidence ID '{}' of type '{}' not found in the phenotype annotations map",
                                    evidenceId, PHENOTYPE_ANNOTATION_EVIDENCE_TYPE);
                        }
                        break;
                    }
                    case FUNCTIONAL_ANNOTATION_EVIDENCE_TYPE: {
                        if (functionalAnnotationsMap.containsKey(evidenceId)) {
                            // TODO remove this line
                            evidence.getFunctionalAnnotations().add(functionalAnnotationsMap.get(evidenceId));
                            evidence.getVariantAssociations().add(variantAnnotationsMap.get(evidenceId));
                        } else {
                            logger.warn("Evidence ID '{}' of type '{}' not found in the functional analysis annotations map",
                                    evidenceId, FUNCTIONAL_ANNOTATION_EVIDENCE_TYPE);
                        }
                        break;
                    }
                    default: {
                        logger.warn("Unknown evidence type '{}': this evidence is skipped. Valid evidence types are: {}",
                                evidenceType,
                                StringUtils.join(
                                        Arrays.asList(VARIANT_ANNOTATION_EVIDENCE_TYPE, GUIDELINE_ANNOTATION_EVIDENCE_TYPE,
                                                DRUG_LABEL_ANNOTATION_EVIDENCE_TYPE, FUNCTIONAL_ANNOTATION_EVIDENCE_TYPE,
                                                PHENOTYPE_ANNOTATION_EVIDENCE_TYPE), ","));
                        break;
                    }
                }

                // Add evidence to clinical annotation
                if (clinicalAnnotationMap.containsKey(clinicalAnnotationId)) {
                    clinicalAnnotationMap.get(clinicalAnnotationId).getEvidences().add(evidence);
                } else {
                    logger.warn("Clinical annotation ID {} from clinical annotation evidence not found in clinical annotations",
                            clinicalAnnotationId);
                }
            }
        }
    }

    private void parseClinicalAnnotationAlleles(Map<String, PharmaClinicalAnnotation> clinicalAnnotationMap) throws IOException {
        // Parse the clinical annotation alleles file (i.e., clinical_ann_alleles.tsv)
        Path allelesPath = pharmGKBDir.resolve(CLINICAL_ANNOTATIONS_BASENAME).resolve(CLINICAL_ANN_ALLELES_TSV_FILENAME);
        try (BufferedReader br = FileUtils.newBufferedReader(allelesPath)) {
            // Skip first line, i.e. the header line
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                String clinicalAnnotationId = fields[0];

                // Sanity check
                if (StringUtils.isEmpty(clinicalAnnotationId)) {
                    logger.warn("Clinical annotation ID is missing in clinical annotation alleles line: {}", line);
                    continue;
                }

                // 0                       1                2                3
                // Clinical Annotation ID  Genotype/Allele  Annotation Text  Allele Function
                PharmaClinicalAllele clinicalAllele = new PharmaClinicalAllele()
                        .setAllele(fields[1])
                        .setAnnotation(fields[2])
                        .setDescription(fields[3]);

                // Add allele to clinical annotation
                if (clinicalAnnotationMap.containsKey(clinicalAnnotationId)) {
                    clinicalAnnotationMap.get(clinicalAnnotationId).getAlleles().add(clinicalAllele);
                } else {
                    logger.warn("Clinical annotation ID {} from clinical annotation alleles file not found in the clinical annotations map",
                            clinicalAnnotationId);
                }
            }
        }
    }

    private Map<String, PharmaVariantAssociation> parseVariantAnnotations() throws IOException {
        Map<String, PharmaVariantAssociation> variantAnnotationMap = new HashMap<>();
        // Parse the variant annotation file (i.e., var_drug_ann.tsv)
        Path varDrugPath = pharmGKBDir.resolve(VARIANT_ANNOTATIONS_BASENAME).resolve(VARIANT_ANNOTATIONS_TSV_FILENAME);
        try (BufferedReader br = FileUtils.newBufferedReader(varDrugPath)) {
            // Skip first line, i.e. the header line
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                String variantAnnotationId = fields[0];

                // Sanity check
                if (StringUtils.isEmpty(variantAnnotationId)) {
                    logger.warn("Variant annotation ID is missing in variant annotations line: {}", line);
                    continue;
                }

                // 0                        1                       2       3       4           5
                // Variant Annotation ID   Variant/Haplotypes      Gene    Drug(s) PMID    Phenotype Category
                // 6           7       8               9           10
                // Significance    Notes   Sentence        Alleles Specialty Population
                PharmaVariantAssociation variantAssociation = new PharmaVariantAssociation()
                        .setVariantId(fields[1])
                        .setGene(fields[2])
                        .setPubmed(fields[4])
                        .setPhenotypeCategory(fields[5])
                        .setSignificance(fields[6])
                        .setDiscussion(fields[7])
                        .setSentence(fields[8])
                        .setAlleles(fields[9])
                        .setSpecialtyPopulation(fields[10]);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("PharmGKB Association Type", "VAR_DRUG");
                variantAssociation.setAttributes(attributes);

                if (StringUtils.isNotEmpty(fields[3])) {
                    variantAssociation.setDrugs(stringFieldToList(fields[3]));
                }

                // Add the annotation to the variantAnnotationMap by variant and gene
                variantAnnotationMap.put(variantAnnotationId, variantAssociation);
            }
        }
        logger.info("Number of variant annotations = {}", variantAnnotationMap.size());

        return variantAnnotationMap;
    }

    private Map<String, PharmaGuidelineAnnotation> parseGuidelineAnnotations() throws IOException {
        Map<String, PharmaGuidelineAnnotation> guidelineAnnotationMap = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        ObjectReader objectReader = mapper.readerFor(PharmaGuidelineAnnotation.class);

        // Parse the guideline annotations JSON files
        Path guidelinesPath = pharmGKBDir.resolve(GUIDELINE_ANNOTATIONS_BASENAME);
        FileUtils.checkDirectory(guidelinesPath);
        for (File file : guidelinesPath.toFile().listFiles()) {
            if (!file.getName().endsWith("json")) {
                continue;
            }
            PharmaGuidelineAnnotation guidelineAnnotation = objectReader.readValue(file);
            if (guidelineAnnotationMap != null && guidelineAnnotation.getGuideline() != null
                    && StringUtils.isEmpty(guidelineAnnotation.getGuideline().getId())) {
                logger.warn("Guideline ID is missing for guideline filename: {}", file.getName());
                continue;
            }

            // Add the guideline annotation to the map by guideline ID (= Evidence ID)
            guidelineAnnotationMap.put(guidelineAnnotation.getGuideline().getId(), guidelineAnnotation);
        }
        logger.info("Number of guideline annotations = {}", guidelineAnnotationMap.size());

        return guidelineAnnotationMap;
    }

    private Map<String, PharmaDrugLabelAnnotation> parseDrugLabelAnnotations() throws IOException {
        Map<String, PharmaDrugLabelAnnotation> drugLabelAnnotationMap = new HashMap<>();
        // Parse the drug labels annotations file (i.e., drugLabels.tsv)
        Path drugLabelPath = pharmGKBDir.resolve(DRUG_LABELS_BASENAME).resolve(DRUG_LABELS_TSV_FILENAME);
        try (BufferedReader br = FileUtils.newBufferedReader(drugLabelPath)) {
            // Skip first line, i.e. the header line
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                String drugLabelId = fields[0];

                // Sanity check
                if (StringUtils.isEmpty(drugLabelId)) {
                    logger.warn("PharmGKB ID is missing in drug label line: {}", line);
                    continue;
                }

                // 0            1     2       3               4              5                     6                7
                // PharmGKB ID  Name  Source  Biomarker Flag  Testing Level  Has Prescribing Info  Has Dosing Info  Has Alternate Drug
                // 8              9            10         11     12                   13
                // Cancer Genome  Prescribing  Chemicals  Genes  Variants/Haplotypes  Latest History Date (YYYY-MM-DD)
                PharmaDrugLabelAnnotation labelAnnotation = new PharmaDrugLabelAnnotation()
                        .setName(fields[1])
                        .setSource(fields[2])
                        .setBiomarkerFlag(fields[3])
                        .setTestingLevel(fields[4])
                        .setPrescribingInfo(fields[5])
                        .setDosingInfo(fields[6])
                        .setAlternateDrug(fields[7])
                        .setCancerGenome(fields[8]);

                // Add the drug label annotation to the map by ParhmGKB (= Evidence ID)
                drugLabelAnnotationMap.put(drugLabelId, labelAnnotation);
            }
        }
        logger.info("Number of drug label annotations = {}", drugLabelAnnotationMap.size());

        return drugLabelAnnotationMap;
    }

    private Map<String, PharmaVariantAssociation> parsePhenotypeAnnotations() throws IOException {
        Map<String, PharmaVariantAssociation> phenotypeAnnotationMap = new HashMap<>();
        // Parse the variant annotation file (i.e., var_pheno_ann.tsv)
        Path varDrugPath = pharmGKBDir.resolve(VARIANT_ANNOTATIONS_BASENAME).resolve(PHENOTYPE_ANNOTATIONS_TSV_FILENAME);
        try (BufferedReader br = FileUtils.newBufferedReader(varDrugPath)) {
            // Skip first line, i.e. the header line
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                String variantAnnotationId = fields[0];

                // Sanity check
                if (StringUtils.isEmpty(variantAnnotationId)) {
                    logger.warn("Variant annotation ID is missing in phenotype annotations line: {}", line);
                    continue;
                }

                // 0                      1                   2     3        4     5                   6             7      8
                // Variant Annotation ID  Variant/Haplotypes  Gene  Drug(s)  PMID  Phenotype Category  Significance  Notes  Sentence
                // 9        10                   .....
                // Alleles  Specialty Population .....
                PharmaVariantAssociation variantAssociation = new PharmaVariantAssociation()
                        .setVariantId(fields[1])
                        .setGene(fields[2])
                        .setPubmed(fields[4])
                        .setPhenotypeCategory(fields[5])
                        .setSignificance(fields[6])
                        .setDiscussion(fields[7])
                        .setSentence(fields[8])
                        .setAlleles(fields[9])
                        .setSpecialtyPopulation(fields[10]);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("PharmGKB Association Type", "VAR_PHENOTYPE");
                variantAssociation.setAttributes(attributes);

                if (StringUtils.isNotEmpty(fields[3])) {
                    variantAssociation.setDrugs(stringFieldToList(fields[3]));
                }

                // Add the annotation to the variantAnnotationMap by variant and gene
                phenotypeAnnotationMap.put(variantAnnotationId, variantAssociation);
            }
        }
        logger.info("Number of phenotype annotations = {}", phenotypeAnnotationMap.size());

        return phenotypeAnnotationMap;
    }

    private Map<String, PharmaVariantAssociation> parseFunctionalAnnotations() throws IOException {
        Map<String, PharmaVariantAssociation> functionalAnnotationMap = new HashMap<>();
        // Parse the variant annotation file (i.e., var_fa_ann.tsv)
        Path varDrugPath = pharmGKBDir.resolve(VARIANT_ANNOTATIONS_BASENAME).resolve(FUNCTIONAL_ANNOTATIONS_TSV_FILENAME);
        try (BufferedReader br = FileUtils.newBufferedReader(varDrugPath)) {
            // Skip first line, i.e. the header line
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                String variantAnnotationId = fields[0];

                // Sanity check
                if (StringUtils.isEmpty(variantAnnotationId)) {
                    logger.warn("Variant annotation ID is missing in variant annotations line: {}", line);
                    continue;
                }

                // 0                        1                       2       3       4           5
                // Variant Annotation ID   Variant/Haplotypes      Gene    Drug(s) PMID    Phenotype Category
                // 6           7       8               9           10                    11          .....
                // Significance    Notes   Sentence        Alleles Specialty Population  Assay type  .....
                PharmaVariantAssociation variantAssociation = new PharmaVariantAssociation()
                        .setVariantId(fields[1])
                        .setGene(fields[2])
                        .setPubmed(fields[4])
                        .setPhenotypeCategory(fields[5])
                        .setSignificance(fields[6])
                        .setDiscussion(fields[7])
                        .setSentence(fields[8])
                        .setAlleles(fields[9])
                        .setSpecialtyPopulation(fields[10])
                        .setAssayType(fields[11]);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("PharmGKB Association Type", "VAR_FUNCTIONAL_ANALYSIS");
                variantAssociation.setAttributes(attributes);

                if (StringUtils.isNotEmpty(fields[3])) {
                    variantAssociation.setDrugs(stringFieldToList(fields[3]));
                }

                // Add the annotation to the variantAnnotationMap by variant and gene
                functionalAnnotationMap.put(variantAnnotationId, variantAssociation);
            }
        }
        logger.info("Number of variant annotations = {}", functionalAnnotationMap.size());

        return functionalAnnotationMap;
    }

    private void parseStudyParameters(Map<String, PharmaVariantAssociation> variantAnnotationsMap,
                                      Map<String, PharmaVariantAssociation> phenotypeAnnotationsMap,
                                      Map<String, PharmaVariantAssociation> functionalAnnotationsMap) throws IOException {
        Map<String, List<PharmaStudyParameters>> studyParametersMap = new HashMap<>();
        // Parse the study parameters file (i.e., study_parameters.tsv)
        Path studyParamsPath = pharmGKBDir.resolve(VARIANT_ANNOTATIONS_BASENAME).resolve(STUDY_PARAMETERS_TSV_FILENAME);
        try (BufferedReader br = FileUtils.newBufferedReader(studyParamsPath)) {
            // Skip first line, i.e. the header line
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                String variantAnnotationId = fields[1];

                // Sanity check
                if (StringUtils.isEmpty(variantAnnotationId)) {
                    logger.warn("Variant annotation ID is missing in study parameters line: {}", line);
                    continue;
                }

                // 0                    1                      2           3            4               5
                // Study Parameters ID  Variant Annotation ID  Study Type  Study Cases  Study Controls  Characteristics
                // 6                     7                   8                             9
                // Characteristics Type  Frequency In Cases  Allele Of Frequency In Cases  Frequency In Controls
                // 10                               11       12               13          14                        15
                // Allele Of Frequency In Controls  P Value  Ratio Stat Type  Ratio Stat  Confidence Interval Start Confidence Interval Stop
                // 16
                // Biogeographical Groups
                PharmaStudyParameters studyParams = new PharmaStudyParameters()
                        .setStudyType(fields[2])
                        .setStudyCases(fields[3])
                        .setStudyControls(fields[4])
                        .setCharacteristics(fields[5])
                        .setCharacteristicsType(fields[6])
                        .setFrequencyInCases(fields[7])
                        .setAlleleOfFrequencyInCases(fields[8])
                        .setFrequencyInControls(fields[9])
                        .setAlleleOfFrequencyInControls(fields[10])
                        .setpValue(fields[11])
                        .setRatioStatType(fields[12])
                        .setRatioStat(fields[13])
                        .setConfidenceIntervalStart(fields[14])
                        .setConfidenceIntervalStop(fields[15])
                        .setBiogeographicalGroups(fields[16]);

                // Add the study parameters map
                if (!studyParametersMap.containsKey(variantAnnotationId)) {
                    studyParametersMap.put(variantAnnotationId, new ArrayList<>());
                }
                studyParametersMap.get(variantAnnotationId).add(studyParams);
            }
        }
        logger.info("Number of study parameters lines = {}", studyParametersMap.size());

        for (Map.Entry<String, List<PharmaStudyParameters>> entry : studyParametersMap.entrySet()) {
            if (variantAnnotationsMap.containsKey(entry.getKey())) {
                variantAnnotationsMap.get(entry.getKey()).setStudyParameters(entry.getValue());
            } else if (phenotypeAnnotationsMap.containsKey(entry.getKey())) {
                phenotypeAnnotationsMap.get(entry.getKey()).setStudyParameters(entry.getValue());
            } else if (functionalAnnotationsMap.containsKey(entry.getKey())) {
                functionalAnnotationsMap.get(entry.getKey()).setStudyParameters(entry.getValue());
            } else {
                logger.warn("Study parameters with variant annotation ID {} not found in variant, phenotype and functional annotations",
                        entry.getKey());
            }
        }
    }

    private List<String> stringFieldToList(String field) {
        if (field.startsWith("\"")) {
            // FIXME: double double quotes
            return Arrays.stream(field.replace("\", \"", "\",\"").split("\",\"")).map(String::trim).collect(Collectors.toList());
        } else {
            if (field.contains(", ")) {
                return Arrays.stream(field.replace(", ", ",").split(",")).map(String::trim).collect(Collectors.toList());
            } else {
                return Collections.singletonList(field);
            }
        }
    }

}
