/*
 * Copyright (C) 2010-2011  "Bio4j"
 *
 * This file is part of Bio4j
 *
 * Bio4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.era7.bioinfo.bio4j.server.servlet;

import com.amazonaws.services.s3.AmazonS3Client;
import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4jmodel.nodes.InterproNode;
import com.era7.bioinfo.bio4jmodel.nodes.IsoformNode;
import com.era7.bioinfo.bio4jmodel.nodes.KeywordNode;
import com.era7.bioinfo.bio4jmodel.nodes.ProteinNode;
import com.era7.bioinfo.bio4jmodel.nodes.SubcellularLocationNode;
import com.era7.bioinfo.bio4jmodel.nodes.citation.ArticleNode;
import com.era7.bioinfo.bio4jmodel.relationships.features.ActiveSiteFeatureRel;
import com.era7.bioinfo.bio4jmodel.relationships.features.SignalPeptideFeatureRel;
import com.era7.bioinfo.bio4jmodel.relationships.features.SpliceVariantFeatureRel;
import com.era7.bioinfo.bio4jmodel.relationships.features.TransmembraneRegionFeatureRel;
import com.era7.bioinfo.bio4jmodel.relationships.protein.ProteinIsoformInteractionRel;
import com.era7.bioinfo.bio4jmodel.relationships.protein.ProteinProteinInteractionRel;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.bioinfo.bio4jmodel.util.GoUtil;
import com.era7.bioinfo.bio4jmodel.util.NodeRetriever;
import com.era7.bioinfo.bioinfoaws.s3.S3FileDownloader;
import com.era7.bioinfo.bioinfoaws.s3.S3FileUploader;
import com.era7.bioinfo.bioinfoaws.util.CredentialsRetriever;
import com.era7.bioinfo.servletlibraryneo4j.servlet.BasicServletNeo4j;
import com.era7.lib.bioinfo.bioinfoutil.go.GOExporter;
import com.era7.lib.bioinfoxml.cufflinks.CuffLinksElement;
import com.era7.lib.bioinfoxml.go.GoAnnotationXML;
import com.era7.lib.bioinfoxml.uniprot.ArticleXML;
import com.era7.lib.bioinfoxml.uniprot.FeatureXML;
import com.era7.lib.bioinfoxml.uniprot.InterproXML;
import com.era7.lib.bioinfoxml.uniprot.IsoformXML;
import com.era7.lib.bioinfoxml.uniprot.KeywordXML;
import com.era7.lib.bioinfoxml.uniprot.ProteinXML;
import com.era7.lib.bioinfoxml.uniprot.SubcellularLocationXML;
import com.era7.lib.communication.model.BasicSession;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * Generates a multifasta file including the corresponding fasta format for every protein passed as a parameter. 
 * It expects a url for accessing the file including the protein accessions.
 * The result file is stored as a S3 object with the name and bucket specified as parameter.
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GetCuffLinksFullReportServlet extends BasicServletNeo4j {
    
    public static final String TSV_HEADER = "CUFF_LINKS_ID\tGENE_NAME\tPROTEIN_ACCESSION\tPROTEIN_NAMES\tINTERPRO" + 
            "\tKEYWORDS\tPROTEIN_PROTEIN_INCOMING_INTERACTIONS\tPROTEIN_PROTEIN_OUTGOING_INTERACTIONS\t" + 
            "PROTEIN_ISOFORM_INCOMING_INTERACTIONS\tPROTEIN_ISOFORM_OUTGOING_INTERACTIONS\t" +
            "SUBCELLULAR_LOCATIONS\tACTIVE_SITES\tTRANSMEMBRANE_REGIONS\tSPLICE_VARIANTS\tSIGNAL_PEPTIDES\tARTICLE_CITATIONS\n";

    @Override
    protected Response processRequest(Request request, BasicSession session, Bio4jManager manager,
            HttpServletRequest hsr) throws Throwable {


        Response response = new Response();

        try {

            String method = request.getMethod();


            if (method.equals(RequestList.GET_CUFF_LINKS_FULL_REPORT_REQUEST)) {

//                String urlSt = request.getParameters().getChildText("url");
//                String fileName = request.getParameters().getChildText("file_name");
//                String bucketName = request.getParameters().getChildText("bucket_name");
//                URL url = new URL(urlSt);
//                InputStream inputStream = url.openStream();
                
                String inputBucketNameSt = request.getParameters().getChildText("input_bucket_name");
                String outputBucketNameSt = request.getParameters().getChildText("output_bucket_name");
                String inputFileNameSt = request.getParameters().getChildText("input_file_name");
                String outputFileNameSt = request.getParameters().getChildText("output_file_name");
                
                System.out.println("outputFileNameSt = " + outputFileNameSt);
                System.out.println("inputFileNameSt = " + inputFileNameSt);
                System.out.println("inputBucketNameSt = " + inputBucketNameSt);
                System.out.println("outputBucketNameSt = " + outputBucketNameSt);

                NodeRetriever nodeRetriever = new NodeRetriever(manager);

                InputStream inputStream = S3FileDownloader.getS3FileInputStream(inputFileNameSt, inputBucketNameSt, new AmazonS3Client(CredentialsRetriever.getBasicAWSCredentialsFromOurAMI()));
                BufferedReader inBuff = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;

                File tempFile = new File(outputFileNameSt + ".xml");
                File tsvTempFile = new File(outputFileNameSt + ".tsv");
                BufferedWriter outBuff = new BufferedWriter(new FileWriter(tempFile));
                BufferedWriter outTsvBuff = new BufferedWriter(new FileWriter(tsvTempFile));
                outTsvBuff.write(TSV_HEADER);
                HashMap<String, String> cuffLinkGenesMap = new HashMap<String, String>();
                HashMap<String, String> geneProteinMap = new HashMap<String, String>();
                HashMap<String, LinkedList<String>> proteinCuffLinkMap = new HashMap<String, LinkedList<String>>();
                LinkedList<String> emptyCufflinks = new LinkedList<String>();

                System.out.println("getting cuff links and gene names...");

                //skipping the header
                inBuff.readLine();

                //---getting cufflinks and gene names----------
                while ((line = inBuff.readLine()) != null) {
                    String[] columns = line.split("\t");
                    String geneName = columns[2];
                    String cuffLinkID = columns[0];
                    if (!geneName.equals("-")) {
                        cuffLinkGenesMap.put(cuffLinkID, geneName);
                    } else {
                        emptyCufflinks.add(cuffLinkID);
                    }

                }
                inBuff.close();
                inputStream.close();
                System.out.println("done!");


                outBuff.write("<cuff_links_elements>\n");


                for (String cuffLinkId : cuffLinkGenesMap.keySet()) {
                    
                    
                    String tempGeneName = cuffLinkGenesMap.get(cuffLinkId);
                    List<ProteinNode> proteins = nodeRetriever.getProteinsByGeneNames(tempGeneName);                    
                    
                    String proteinAccession = null;

                    if (proteins.size() > 0) {
                        if (proteins.size() == 1) {
                            proteinAccession = proteins.get(0).getAccession();
                        } else {

                            ProteinNode selectedProtein = null;

                            for (int i = 0; i < proteins.size(); i++) {

                                ProteinNode proteinNode = proteins.get(i);
                                if (proteinNode.getOrganism().getScientificName().equalsIgnoreCase("Homo Sapiens")) {
                                    selectedProtein = proteinNode;
                                    if (proteinNode.getDataset().getName().equalsIgnoreCase("swiss-prot")) {
                                        break;
                                        //we don't need to keep searching (any human sprot protein is ok)
                                    }
                                }
                            }

                            if (selectedProtein != null) {
                                proteinAccession = selectedProtein.getAccession();
                            }
                        }
                    }

                    if (proteinAccession != null) {

                        geneProteinMap.put(tempGeneName, proteinAccession);

                        //-------filling protein --> cufflinks hashmap -----------
                        LinkedList<String> cuffLinkedList = proteinCuffLinkMap.get(proteinAccession);
                        if (cuffLinkedList == null) {
                            cuffLinkedList = new LinkedList<String>();
                            proteinCuffLinkMap.put(proteinAccession, cuffLinkedList);
                        }
                        cuffLinkedList.add(cuffLinkId);
                        //---------------------------------------------------------

                    } else {
                        emptyCufflinks.add(cuffLinkId);
                    }


                }


                System.out.println("getting go annotations...");
                ArrayList<ProteinXML> proteins = new ArrayList<ProteinXML>();
                for (String protId : geneProteinMap.values()) {
                    ProteinXML tempProt = new ProteinXML();
                    tempProt.setId(protId);
                    proteins.add(tempProt);
                }
                GoAnnotationXML goAnnotationXML = GoUtil.getGoAnnotation(proteins, manager);
                List<File> goResultFiles = GOExporter.calculateFrequenciesAndExportToFiles(goAnnotationXML, inputFileNameSt.split("\\.")[0]);
                System.out.println("done!");

                //=========modifying go reports (including cufflink ids info)=================
                List<File> filesToBeUploaded = new LinkedList<File>();
                
                for (File goTempFile : goResultFiles) {
                    
                    if (goTempFile.getName().toLowerCase().indexOf("freq") > 0) {

                        File tempOutFile = new File(goTempFile.getName().split("\\.")[0] + ".tsv");
                        filesToBeUploaded.add(tempOutFile);
                        BufferedWriter tempOutBuff = new BufferedWriter(new FileWriter(tempOutFile));
                        BufferedReader tempBuffReader = new BufferedReader(new FileReader(goTempFile));
                        String tempLine = null;
                        //---changing header---
                        String header = tempBuffReader.readLine();
                        tempOutBuff.write(header + "\t" + "GENE_NAMES" + "\t" + "CUFFLINKS_IDS" + "\t" + "NUMBER_OF_CUFFLINKS_GENES" + "\n");
                        
                        while ((tempLine = tempBuffReader.readLine()) != null) {
                            
                            tempOutBuff.write(tempLine + "\t");
                            String[] cols = tempLine.split("\t");
                            String[] tempProts = cols[cols.length - 1].split(",");
                            
                            int cufflinksIdsCounter = 0;
                            
                            String cufflinksIdsAndCounterSt = "";
                            
                            HashSet<String> tempGeneNamesSet = new HashSet<String>();
                            
                            for (int i = 0; i < tempProts.length; i++) {
                                LinkedList<String> cufflinkIdsList = proteinCuffLinkMap.get(tempProts[i]);
                                if (i != tempProts.length - 1) {
                                    for (String cufflinkIdTemp : cufflinkIdsList) {
                                        cufflinksIdsAndCounterSt += cufflinkIdTemp + ",";
                                        cufflinksIdsCounter++;
                                        tempGeneNamesSet.add(cuffLinkGenesMap.get(cufflinkIdTemp));
                                    }                                    
                                } else {
                                    for (int j=0; j < cufflinkIdsList.size() - 1; j++) {
                                        cufflinksIdsAndCounterSt += cufflinkIdsList.get(j) + ",";
                                        cufflinksIdsCounter++;
                                        tempGeneNamesSet.add(cuffLinkGenesMap.get(cufflinkIdsList.get(j)));
                                    }
                                    cufflinksIdsAndCounterSt += cufflinkIdsList.get(cufflinkIdsList.size() - 1) + "\t";
                                    cufflinksIdsCounter++;
                                    tempGeneNamesSet.add(cuffLinkGenesMap.get(cufflinkIdsList.get(cufflinkIdsList.size() - 1)));
                                }
                            }
                            
                            cufflinksIdsAndCounterSt += cufflinksIdsCounter;
                            
                            //----writing gene names-----
                            String genesSt = "";
                            for (String tempGeneNAme : tempGeneNamesSet) {
                                genesSt += tempGeneNAme + ",";
                            }
                            tempOutBuff.write(genesSt.substring(0,genesSt.length() - 1) + "\t");
                            
                            //---writing value for cufflinks freq column----
                            tempOutBuff.write(cufflinksIdsAndCounterSt + "\n");
                            
                        }
                        
                        tempBuffReader.close();
                        tempOutBuff.close();
                    } else {
                        //delete file because we don't need it
                        goTempFile.delete();
                    }
                }

                //===========================================================================

                for (String cuffLinkId : cuffLinkGenesMap.keySet()) {
                    
                    outTsvBuff.write(cuffLinkId + "\t");

                    String tempGeneName = cuffLinkGenesMap.get(cuffLinkId);
                    String tempProtId = geneProteinMap.get(tempGeneName);
                    
                    outTsvBuff.write(tempGeneName + "\t");

                    CuffLinksElement cuffLinksElement = new CuffLinksElement();
                    cuffLinksElement.setId(cuffLinkId);
                    cuffLinksElement.setGeneName(tempGeneName);

                    if (tempProtId != null) {
                        ProteinXML proteinXML = new ProteinXML();
                        cuffLinksElement.addProtein(proteinXML);

                        proteinXML.setId(tempProtId);
                        //---getting protein node
                        ProteinNode proteinNode = nodeRetriever.getProteinNodeByAccession(tempProtId);
                        
                        outTsvBuff.write(tempProtId + "\t");

                        String protName,protFullName,protShortName;
                        protName = proteinNode.getName();
                        protFullName = proteinNode.getFullName();
                        protShortName = proteinNode.getShortName();
                        
                        //---protein names---
                        proteinXML.setProteinName(protName);
                        proteinXML.setFullName(protFullName);
                        proteinXML.setShortName(protShortName);              
                        
                        outTsvBuff.write(protName + "," + protFullName + "," + protShortName + "\t");                        
                        
                        String interproSt = "";
                        //---interpro---
                        for (InterproNode interproNode : proteinNode.getInterpro()) {
                            InterproXML interproXML = new InterproXML();
                            interproXML.setId(interproNode.getId());
                            interproXML.setInterproName(interproNode.getName());
                            proteinXML.addInterpro(interproXML);
                            interproSt += "{" + interproXML.getId() + "," + interproXML.getInterproName() + "},";
                        }
                        if(interproSt.length() > 1){
                            interproSt = interproSt.substring(0, interproSt.length() - 1);
                        }
                        outTsvBuff.write(interproSt + "\t");

                        String keywordsSt = "";
                        //---keywords---
                        for (KeywordNode keywordNode : proteinNode.getKeywords()) {
                            KeywordXML keywordXML = new KeywordXML();
                            keywordXML.setId(keywordNode.getId());
                            keywordXML.setKeywordName(keywordNode.getName());
                            proteinXML.addKeyword(keywordXML);
                            keywordsSt += "{" + keywordXML.getId() + "," + keywordXML.getKeywordName() + "},";
                        }
                        if(keywordsSt.length() > 1){
                            keywordsSt = keywordsSt.substring(0, keywordsSt.length() - 1);
                        }
                        outTsvBuff.write(keywordsSt + "\t");

                        String protProtIncIntSt = "";
                        //---protein-protein interactions-----
                        for (ProteinProteinInteractionRel protProtIncInteraction : proteinNode.getProteinIncomingInteractions()) {
                            ProteinNode pNode = new ProteinNode(protProtIncInteraction.getStartNode());
                            ProteinXML otherProtein = new ProteinXML();
                            otherProtein.setId(pNode.getAccession());
                            otherProtein.setProteinName(pNode.getName());
                            otherProtein.setFullName(pNode.getFullName());
                            protProtIncIntSt += pNode.getAccession() + ",";
                            proteinXML.addProteinProteinIncomingInteraction(otherProtein);
                        }
                        if(protProtIncIntSt.length() > 1){
                            protProtIncIntSt = protProtIncIntSt.substring(0, protProtIncIntSt.length() - 1);
                        }
                        outTsvBuff.write(protProtIncIntSt + "\t");
                        
                        String protProtOutIntSt = "";
                        for (ProteinProteinInteractionRel protProtOutInteraction : proteinNode.getProteinOutgoingInteractions()) {
                            ProteinNode pNode = new ProteinNode(protProtOutInteraction.getEndNode());
                            ProteinXML otherProtein = new ProteinXML();
                            otherProtein.setId(pNode.getAccession());
                            otherProtein.setProteinName(pNode.getName());
                            otherProtein.setFullName(pNode.getFullName());
                            protProtOutIntSt += pNode.getAccession() + ",";
                            proteinXML.addProteinProteinOutgoingInteraction(otherProtein);
                        }
                        if(protProtOutIntSt.length() > 1){
                            protProtOutIntSt = protProtOutIntSt.substring(0, protProtOutIntSt.length() - 1);
                        }
                        outTsvBuff.write(protProtOutIntSt + "\t");
                        //-----------------------------------------
                        

                        //---protein-isoform interactions-----
                        String protIsoIncIntSt = "";
                        for (ProteinIsoformInteractionRel protIsoIncInteraction : proteinNode.getIsoformIncomingInteractions()) {
                            IsoformNode iNode = new IsoformNode(protIsoIncInteraction.getStartNode());
                            IsoformXML isoXML = new IsoformXML();
                            isoXML.setId(iNode.getId());
                            String isoName = "";
                            try{
                                isoName = iNode.getName();
                            }catch(Exception e){
                                isoName = "";
                                System.out.println("Isoform name not found for: " + isoXML.getId());
                            }
                            isoXML.setIsoformName(isoName);
                            protIsoIncIntSt += "{" + isoXML.getId() + "," + isoXML.getIsoformName() + "},";
                            proteinXML.addProteinIsoformIncomingInteraction(isoXML);
                        }
                        if(protIsoIncIntSt.length() > 1){
                            protIsoIncIntSt = protIsoIncIntSt.substring(0, protIsoIncIntSt.length() - 1);
                        }
                        outTsvBuff.write(protIsoIncIntSt + "\t");
                        
                        
                        String protIsoOutIntSt = "";
                        for (ProteinIsoformInteractionRel protIsoOutInteraction : proteinNode.getIsoformOutgoingInteractions()) {
                            IsoformNode iNode = new IsoformNode(protIsoOutInteraction.getEndNode());
                            IsoformXML isoXML = new IsoformXML();
                            isoXML.setId(iNode.getId());
                            String isoName = "";
                            try{
                                isoName = iNode.getName();
                            }catch(Exception e){
                                isoName = "";
                                System.out.println("Isoform name not found for: " + isoXML.getId());
                            }
                            isoXML.setIsoformName(isoName);
                            protIsoOutIntSt += "{" + isoXML.getId() + "," + isoXML.getIsoformName() + "},";
                            proteinXML.addProteinIsoformOutgoingInteraction(isoXML);
                        }
                        if(protIsoOutIntSt.length() > 1){
                            protIsoOutIntSt = protIsoOutIntSt.substring(0, protIsoOutIntSt.length() - 1);
                        }
                        outTsvBuff.write(protIsoOutIntSt + "\t");
                        //-----------------------------------------

                        String subCellSt = "";
                        //----------subcellular-location----------
                        for (SubcellularLocationNode subCellLoc : proteinNode.getSubcellularLocations()) {
                            SubcellularLocationXML subcellularLocationXML = new SubcellularLocationXML();
                            subcellularLocationXML.setSubcellularLocationName(subCellLoc.getName());
                            proteinXML.addSubcellularLocation(subcellularLocationXML);
                            subCellSt += subcellularLocationXML.getSubcellularLocationName() + ",";
                        }
                        if(subCellSt.length() > 1){
                            subCellSt = subCellSt.substring(0, subCellSt.length() - 1);
                        }
                        outTsvBuff.write(subCellSt + "\t");
                        //-------------------------------------------

                        //-------------active site feature----------------------
                        String activeSitesSt = "";
                        for (ActiveSiteFeatureRel actFeatureRel : proteinNode.getActiveSiteFeature()) {
                            FeatureXML actFeatureXML = new FeatureXML();
                            if(!actFeatureRel.getBegin().isEmpty()){
                                actFeatureXML.setBegin(Integer.parseInt(actFeatureRel.getBegin()));
                            }
                            if(!actFeatureRel.getEnd().isEmpty()){
                                actFeatureXML.setEnd(Integer.parseInt(actFeatureRel.getEnd()));
                            }  
                            actFeatureXML.setEvidence(actFeatureRel.getEvidence());
                            actFeatureXML.setDescription(actFeatureRel.getDescription());
                            actFeatureXML.setStatus(actFeatureRel.getStatus());
                            actFeatureXML.setOriginal(actFeatureRel.getOriginal());
                            actFeatureXML.setRef(actFeatureRel.getRef());
                            actFeatureXML.setType(ActiveSiteFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE);
                            proteinXML.addActiveSiteFeature(actFeatureXML);
                            
                            activeSitesSt += "{" + actFeatureRel.getBegin() + "," + actFeatureRel.getEnd() + ","
                                           + actFeatureRel.getEvidence() + "," + actFeatureRel.getDescription() + "," +
                                            actFeatureRel.getStatus() + "," + actFeatureRel.getOriginal() + "," 
                                            + actFeatureRel.getRef() + "," + actFeatureXML.getType() + "},";
                            
                        }
                        if(activeSitesSt.length() > 1){
                            activeSitesSt = activeSitesSt.substring(0, activeSitesSt.length() - 1);
                        }
                        outTsvBuff.write(activeSitesSt + "\t");
                        //-------------------------------------------

                        //-------------transmembrane region feature----------------------
                        String transRegSt = "";
                        for (TransmembraneRegionFeatureRel transRegFeatureRel : proteinNode.getTransmembraneRegionFeature()) {
                            FeatureXML trFeatureXML = new FeatureXML();
                            if(!transRegFeatureRel.getBegin().isEmpty()){
                                trFeatureXML.setBegin(Integer.parseInt(transRegFeatureRel.getBegin()));
                            }
                            if(!transRegFeatureRel.getEnd().isEmpty()){
                                trFeatureXML.setEnd(Integer.parseInt(transRegFeatureRel.getEnd()));
                            }                            
                            trFeatureXML.setEvidence(transRegFeatureRel.getEvidence());
                            trFeatureXML.setDescription(transRegFeatureRel.getDescription());
                            trFeatureXML.setStatus(transRegFeatureRel.getStatus());
                            trFeatureXML.setOriginal(transRegFeatureRel.getOriginal());
                            trFeatureXML.setRef(transRegFeatureRel.getRef());
                            trFeatureXML.setType(TransmembraneRegionFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE);
                            proteinXML.addTransmembraneRegionFeature(trFeatureXML);
                            
                            activeSitesSt += "{" + transRegFeatureRel.getBegin() + "," + transRegFeatureRel.getEnd() + ","
                                           + transRegFeatureRel.getEvidence() + "," + transRegFeatureRel.getDescription() + "," +
                                            transRegFeatureRel.getStatus() + "," + transRegFeatureRel.getOriginal() + "," 
                                            + transRegFeatureRel.getRef() + "," + trFeatureXML.getType() + "},";
                        }
                        if(transRegSt.length() > 1){
                            transRegSt = transRegSt.substring(0, transRegSt.length() - 1);
                        }
                        outTsvBuff.write(transRegSt + "\t");
                        //-------------------------------------------

                        //-------------splice variant feature----------------------
                        String spliceVarSt = "";
                        for (SpliceVariantFeatureRel spVarFeatureRel : proteinNode.getSpliceVariantFeature()) {
                            FeatureXML spVarFeatureXML = new FeatureXML();
                            if(!spVarFeatureRel.getBegin().isEmpty()){
                                spVarFeatureXML.setBegin(Integer.parseInt(spVarFeatureRel.getBegin()));
                            }
                            if(!spVarFeatureRel.getEnd().isEmpty()){
                                spVarFeatureXML.setEnd(Integer.parseInt(spVarFeatureRel.getEnd()));
                            } 
                            spVarFeatureXML.setEvidence(spVarFeatureRel.getEvidence());
                            spVarFeatureXML.setDescription(spVarFeatureRel.getDescription());
                            spVarFeatureXML.setStatus(spVarFeatureRel.getStatus());
                            spVarFeatureXML.setOriginal(spVarFeatureRel.getOriginal());
                            spVarFeatureXML.setRef(spVarFeatureRel.getRef());
                            spVarFeatureXML.setType(SpliceVariantFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE);
                            proteinXML.addSpliceVariantFeature(spVarFeatureXML);
                            
                            spliceVarSt += "{" + spVarFeatureRel.getBegin() + "," + spVarFeatureRel.getEnd() + ","
                                           + spVarFeatureRel.getEvidence() + "," + spVarFeatureRel.getDescription() + "," +
                                            spVarFeatureRel.getStatus() + "," + spVarFeatureRel.getOriginal() + "," 
                                            + spVarFeatureRel.getRef() + "," + spVarFeatureXML.getType() + "},";
                        }
                        if(spliceVarSt.length() > 1){
                            spliceVarSt = spliceVarSt.substring(0, spliceVarSt.length() - 1);
                        }
                        outTsvBuff.write(spliceVarSt + "\t");
                        //-------------------------------------------

                        //------------signal peptide feature----------------------
                        String signalPepSt = "";
                        for (SignalPeptideFeatureRel spFeatureRel : proteinNode.getSignalPeptideFeature()) {
                            FeatureXML spFeatureXML = new FeatureXML();
                            if(!spFeatureRel.getBegin().isEmpty()){
                                spFeatureXML.setBegin(Integer.parseInt(spFeatureRel.getBegin()));
                            }
                            if(!spFeatureRel.getEnd().isEmpty()){
                                spFeatureXML.setEnd(Integer.parseInt(spFeatureRel.getEnd()));
                            } 
                            spFeatureXML.setEvidence(spFeatureRel.getEvidence());
                            spFeatureXML.setDescription(spFeatureRel.getDescription());
                            spFeatureXML.setStatus(spFeatureRel.getStatus());
                            spFeatureXML.setOriginal(spFeatureRel.getOriginal());
                            spFeatureXML.setRef(spFeatureRel.getRef());
                            spFeatureXML.setType(SignalPeptideFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE);
                            proteinXML.addSignalPeptideFeature(spFeatureXML);
                            
                            signalPepSt += "{" + spFeatureRel.getBegin() + "," + spFeatureRel.getEnd() + ","
                                           + spFeatureRel.getEvidence() + "," + spFeatureRel.getDescription() + "," +
                                            spFeatureRel.getStatus() + "," + spFeatureRel.getOriginal() + "," 
                                            + spFeatureRel.getRef() + "," + spFeatureXML.getType() + "},";
                            
                        }
                        if(signalPepSt.length() > 1){
                            signalPepSt = signalPepSt.substring(0, signalPepSt.length() - 1);
                        }
                        outTsvBuff.write(signalPepSt + "\t");
                        //-------------------------------------------

                        //--article-citations-----
                        String articlesSt = "";
                        for (ArticleNode article : proteinNode.getArticleCitations()) {
                            ArticleXML articleXML = new ArticleXML();
                            articleXML.setTitle(article.getTitle());
                            articleXML.setMedlineId(article.getMedlineId());
                            proteinXML.addArticleCitation(articleXML);
                            articlesSt += "{" + articleXML.getTitle() + "," + articleXML.getMedlineId() + "},";
                        }
                        if(articlesSt.length() > 1){
                            articlesSt = articlesSt.substring(0, articlesSt.length() - 1);
                        }
                        outTsvBuff.write(articlesSt + "\t");
                        //------------------------  
                    }


                    outBuff.write(cuffLinksElement.toString() + "\n");
                    outTsvBuff.write("\n");
                    

                }

                //-----adding also the empty ones to the final report-----
                for (String emptyCufflink : emptyCufflinks) {
                    
                    CuffLinksElement cuffLinksElement = new CuffLinksElement();
                    cuffLinksElement.setId(emptyCufflink);
                    outBuff.write(cuffLinksElement.toString() + "\n");
                    
                    outTsvBuff.write(cuffLinksElement.getId() + "\n");
                }

                outBuff.write("</cuff_links_elements>");
                outBuff.close();
                
                outTsvBuff.close();

                System.out.println("done!");

                System.out.println("Uploading file to S3...");

                //uploading xml file				
                AmazonS3Client s3Client = new AmazonS3Client(CredentialsRetriever.getBasicAWSCredentialsFromOurAMI());
                S3FileUploader.uploadEveryFileToS3Bucket(tempFile, outputBucketNameSt, "", s3Client, false);
                System.out.println("Deleting temporal xml file...");
                //deleting temp xml file
                tempFile.delete();
                
                //uploading tsv file
                S3FileUploader.uploadEveryFileToS3Bucket(tsvTempFile, outputBucketNameSt, "", s3Client, false);
                System.out.println("Deleting temporal tsv file...");
                //deleting temp tsv file
                tsvTempFile.delete();

                //--uploading go files---
                for (File file : filesToBeUploaded) {
                    S3FileUploader.uploadEveryFileToS3Bucket(file, outputBucketNameSt, "", s3Client, false);
                    file.delete();//deleting the temporal file
                }

                response.setStatus(Response.SUCCESSFUL_RESPONSE);

                System.out.println("Cool! ;)");

            } else {
                response.setError("There is no such method");
            }

        } catch (Exception e) {
            response.setError(e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    @Override
    protected void logSuccessfulOperation(Request rqst, Response rspns, Bio4jManager manager,
            BasicSession session) {
    }

    @Override
    protected void logErrorResponseOperation(Request rqst, Response rspns, Bio4jManager manager,
            BasicSession session) {
    }

    @Override
    protected void logErrorExceptionOperation(Request rqst, Response rspns, Throwable thrwbl,
            Bio4jManager manager) {
    }

    @Override
    protected void noSession(Request request) {
    }

    @Override
    protected boolean checkPermissions(ArrayList<?> al, Request rqst) {
        return true;
    }

    @Override
    protected boolean defineCheckSessionFlag() {
        return false;
    }

    @Override
    protected boolean defineCheckPermissionsFlag() {
        return false;
    }

    @Override
    protected boolean defineLoggableFlag() {
        return false;
    }

    @Override
    protected boolean defineLoggableErrorsFlag() {
        return false;
    }

    @Override
    protected boolean defineUtf8CharacterEncodingRequest() {
        return false;
    }

    @Override
    protected void initServlet() {
    }

    @Override
    protected String defineNeo4jDatabaseFolder() {
        return CommonData.DATABASE_FOLDER;
    }
}
