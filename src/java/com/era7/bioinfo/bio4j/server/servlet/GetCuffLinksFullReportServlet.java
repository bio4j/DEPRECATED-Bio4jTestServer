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
import com.era7.bioinfo.bioinfoaws.s3.S3FileUploader;
import com.era7.bioinfo.bioinfoaws.util.CredentialsRetriever;
import com.era7.bioinfo.servletlibraryneo4j.servlet.BasicServletNeo4j;
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
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * Generates a multifasta file including the corresponding fasta format for every protein passed as a parameter. 
 * It expects a url for accessing the file including the protein accessions.
 * The result file is stored as a S3 object with the name and bucket specified as parameter.
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GetCuffLinksFullReportServlet extends BasicServletNeo4j {

    @Override
    protected Response processRequest(Request request, BasicSession session, Bio4jManager manager,
            HttpServletRequest hsr) throws Throwable {


        Response response = new Response();

        try {

            String method = request.getMethod();


            if (method.equals(RequestList.GET_CUFF_LINKS_FULL_REPORT_REQUEST)) {

                String urlSt = request.getParameters().getChildText("url");
                String fileName = request.getParameters().getChildText("file_name");
                String bucketName = request.getParameters().getChildText("bucket_name");
                URL url = new URL(urlSt);
                InputStream inputStream = url.openStream();
                
                NodeRetriever nodeRetriever = new NodeRetriever(manager);
                
                BufferedReader inBuff = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;

                File tempFile = new File(fileName);
                BufferedWriter outBuff = new BufferedWriter(new FileWriter(tempFile));
                HashMap<String,String> cuffLinkGenesMap = new HashMap<String, String>();
                HashMap<String,String> geneProteinMap = new HashMap<String, String>();

                System.out.println("getting cuff links and gene names...");
                
                //skipping the header
                inBuff.readLine();
                
                //---getting cufflinks and gene names----------
                while ((line = inBuff.readLine()) != null) {
                    String[] columns = line.split("\t");
                    cuffLinkGenesMap.put(columns[0], columns[2]);
                }
                inBuff.close();
                inputStream.close();
                System.out.println("done!");

                
                outBuff.write("<cuff_links_elements>\n");
                
                
                for (String cuffLinkId : cuffLinkGenesMap.keySet()) {                   
                    
                    String tempGeneName = cuffLinkGenesMap.get(cuffLinkId);
                    List<ProteinNode> proteins = nodeRetriever.getProteinsByGeneNames(tempGeneName);                    
                    
                    String proteinAccession = "";
                    
                    if(proteins.size() > 0){
                        if(proteins.size() == 1){
                            proteinAccession = proteins.get(0).getAccession();
                        }else{
                            
                            ProteinNode selectedProtein = null;
                            
                            for (int i=0;i<proteins.size();i++) {
                                
                                ProteinNode proteinNode = proteins.get(i);
                                if(proteinNode.getOrganism().getScientificName().equalsIgnoreCase("Homo Sapiens")){
                                    selectedProtein = proteinNode;
                                    if(proteinNode.getDataset().getName().equalsIgnoreCase("swiss-prot")){
                                        break;
                                        //we don't need to keep searching (any human sprot protein is ok)
                                    }
                                }
                            }
                            
                            if(selectedProtein != null){
                                proteinAccession = selectedProtein.getAccession();
                            } 
                        }
                    }
                    
                    geneProteinMap.put(tempGeneName, proteinAccession);                    
                    
                }
                
                
                System.out.println("getting go annotations...");
                ArrayList<ProteinXML> proteins = new ArrayList<ProteinXML>();
                for (String protId : geneProteinMap.values()) {
                    ProteinXML tempProt = new ProteinXML();
                    tempProt.setId(protId);
                    proteins.add(tempProt);
                }
                GoAnnotationXML goAnnotationXML = GoUtil.getGoAnnotation(proteins, manager);
                System.out.println("done!");
                
                for (String cuffLinkId : cuffLinkGenesMap.keySet()) { 
                    
                    String tempGeneName = cuffLinkGenesMap.get(cuffLinkId);
                    String tempProtId = geneProteinMap.get(tempGeneName);
                    
                    CuffLinksElement cuffLinksElement = new CuffLinksElement();
                    cuffLinksElement.setId(cuffLinkId);
                    cuffLinksElement.setGeneName(tempGeneName);
                    
                    ProteinXML proteinXML = new ProteinXML();
                    cuffLinksElement.addProtein(proteinXML);
                    
                    proteinXML.setId(tempProtId);
                    //---getting protein node
                    ProteinNode proteinNode = nodeRetriever.getProteinNodeByAccession(tempProtId);
                    
                    //---protein names---
                    proteinXML.setProteinName(proteinNode.getName());
                    proteinXML.setFullName(proteinNode.getFullName());
                    proteinXML.setShortName(proteinNode.getShortName());
                    
                    //---interpro---
                    for (InterproNode interproNode : proteinNode.getInterpro()) {
                        InterproXML interproXML = new InterproXML();
                        interproXML.setId(interproNode.getId());
                        interproXML.setName(interproNode.getName());
                        proteinXML.addInterpro(interproXML);
                    }
                    
                    //---keywords---
                    for (KeywordNode keywordNode : proteinNode.getKeywords()) {
                        KeywordXML keywordXML = new KeywordXML();
                        keywordXML.setId(keywordNode.getId());
                        keywordXML.setKeywordName(keywordNode.getName());
                        proteinXML.addKeyword(keywordXML);
                    }
                    
                    //---protein-protein interactions-----
                    for (ProteinProteinInteractionRel protProtIncInteraction : proteinNode.getProteinIncomingInteractions()) {
                        ProteinNode pNode = new ProteinNode(protProtIncInteraction.getStartNode());
                        ProteinXML otherProtein = new ProteinXML();
                        otherProtein.setId(pNode.getAccession());
                        otherProtein.setName(otherProtein.getName());
                        otherProtein.setFullName(otherProtein.getFullName());
                        proteinXML.addProteinProteinIncomingInteraction(proteinXML);
                    }
                    for (ProteinProteinInteractionRel protProtOutInteraction : proteinNode.getProteinOutgoingInteractions()) {
                        ProteinNode pNode = new ProteinNode(protProtOutInteraction.getEndNode());
                        ProteinXML otherProtein = new ProteinXML();
                        otherProtein.setId(pNode.getAccession());
                        otherProtein.setName(otherProtein.getName());
                        otherProtein.setFullName(otherProtein.getFullName());
                        proteinXML.addProteinProteinOutgoingInteraction(proteinXML);
                    }
                    //-----------------------------------------
                    
                    //---protein-isoform interactions-----
                    for (ProteinIsoformInteractionRel protIsoIncInteraction : proteinNode.getIsoformIncomingInteractions()) {
                        IsoformNode iNode = new IsoformNode(protIsoIncInteraction.getStartNode());
                        IsoformXML isoXML = new IsoformXML();
                        isoXML.setId(iNode.getId());
                        isoXML.setIsoformName(iNode.getName());
                        proteinXML.addProteinIsoformIncomingInteraction(isoXML);
                    }
                    for (ProteinIsoformInteractionRel protIsoOutInteraction : proteinNode.getIsoformOutgoingInteractions()) {
                        IsoformNode iNode = new IsoformNode(protIsoOutInteraction.getEndNode());
                        IsoformXML isoXML = new IsoformXML();
                        isoXML.setId(iNode.getId());
                        isoXML.setIsoformName(iNode.getName());
                        proteinXML.addProteinIsoformOutgoingInteraction(isoXML);
                    }
                    //-----------------------------------------
                    
                    //----------subcellular-location----------
                    for (SubcellularLocationNode subCellLoc : proteinNode.getSubcellularLocations()) {
                        SubcellularLocationXML subcellularLocationXML = new SubcellularLocationXML();
                        subcellularLocationXML.setSubcellularLocationName(subCellLoc.getName());
                        proteinXML.addSubcellularLocation(subcellularLocationXML);
                    }
                    //-------------------------------------------
                    
                    //-------------active site feature----------------------
                    for (ActiveSiteFeatureRel actFeatureRel : proteinNode.getActiveSiteFeature()) {
                        FeatureXML actFeatureXML = new FeatureXML();
                        actFeatureXML.setBegin(actFeatureRel.getBegin());
                        actFeatureXML.setEnd(actFeatureRel.getEnd());
                        actFeatureXML.setEvidence(actFeatureRel.getEvidence());
                        actFeatureXML.setDescription(actFeatureRel.getDescription());
                        actFeatureXML.setStatus(actFeatureRel.getStatus());
                        actFeatureXML.setOriginal(actFeatureRel.getOriginal());
                        actFeatureXML.setRef(actFeatureRel.getRef());
                        actFeatureXML.setType(ActiveSiteFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE);
                        proteinXML.addActiveSiteFeature(actFeatureXML);
                    }                    
                    //-------------------------------------------
                    
                    //-------------transmembrane region feature----------------------
                    for (TransmembraneRegionFeatureRel transRegFeatureRel : proteinNode.getTransmembraneRegionFeature()) {
                        FeatureXML trFeatureXML = new FeatureXML();
                        trFeatureXML.setBegin(transRegFeatureRel.getBegin());
                        trFeatureXML.setEnd(transRegFeatureRel.getEnd());
                        trFeatureXML.setEvidence(transRegFeatureRel.getEvidence());
                        trFeatureXML.setDescription(transRegFeatureRel.getDescription());
                        trFeatureXML.setStatus(transRegFeatureRel.getStatus());
                        trFeatureXML.setOriginal(transRegFeatureRel.getOriginal());
                        trFeatureXML.setRef(transRegFeatureRel.getRef());
                        trFeatureXML.setType(TransmembraneRegionFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE);
                        proteinXML.addTransmembraneRegionFeature(trFeatureXML);
                    }                    
                    //-------------------------------------------
                    
                    //-------------splice variant feature----------------------
                    for (SpliceVariantFeatureRel spVarFeatureRel : proteinNode.getSpliceVariantFeature()) {
                        FeatureXML spVarFeatureXML = new FeatureXML();
                        spVarFeatureXML.setBegin(spVarFeatureRel.getBegin());
                        spVarFeatureXML.setEnd(spVarFeatureRel.getEnd());
                        spVarFeatureXML.setEvidence(spVarFeatureRel.getEvidence());
                        spVarFeatureXML.setDescription(spVarFeatureRel.getDescription());
                        spVarFeatureXML.setStatus(spVarFeatureRel.getStatus());
                        spVarFeatureXML.setOriginal(spVarFeatureRel.getOriginal());
                        spVarFeatureXML.setRef(spVarFeatureRel.getRef());
                        spVarFeatureXML.setType(SpliceVariantFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE);
                        proteinXML.addSpliceVariantFeature(spVarFeatureXML);
                    }                    
                    //-------------------------------------------
                    
                    //------------signal peptide feature----------------------
                    for (SignalPeptideFeatureRel spFeatureRel : proteinNode.getSignalPeptideFeature()) {
                        FeatureXML spFeatureXML = new FeatureXML();
                        spFeatureXML.setBegin(spFeatureRel.getBegin());
                        spFeatureXML.setEnd(spFeatureRel.getEnd());
                        spFeatureXML.setEvidence(spFeatureRel.getEvidence());
                        spFeatureXML.setDescription(spFeatureRel.getDescription());
                        spFeatureXML.setStatus(spFeatureRel.getStatus());
                        spFeatureXML.setOriginal(spFeatureRel.getOriginal());
                        spFeatureXML.setRef(spFeatureRel.getRef());
                        spFeatureXML.setType(SignalPeptideFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE);
                        proteinXML.addSignalPeptideFeature(spFeatureXML);
                    }                    
                    //-------------------------------------------
                    
                    //--article-citations-----
                    for (ArticleNode article : proteinNode.getArticleCitations()) {
                        ArticleXML articleXML = new ArticleXML();
                        articleXML.setTitle(article.getTitle());
                        articleXML.setMedlineId(article.getMedlineId());
                        proteinXML.addArticleCitation(articleXML);
                    }
                    //------------------------
                    
                    
                }
                
                outBuff.write("</cuff_links_elements>");                
                outBuff.close();
                
                System.out.println("done!");
                
                System.out.println("Uploading file to S3...");

                //uploading file				
                AmazonS3Client s3Client = new AmazonS3Client(CredentialsRetriever.getBasicAWSCredentialsFromOurAMI());
                S3FileUploader.uploadEveryFileToS3Bucket(tempFile, bucketName, "", s3Client, false);

                System.out.println("Deleting temporal file...");
                
                //deleting temp file
                tempFile.delete();

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
