/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.bio4j.server.util;

import com.era7.bioinfo.bio4jmodel.nodes.ProteinNode;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class FastaUtil {

    public static void getFastaFormatForProtein(ProteinNode proteinNode,
            StringBuilder resultStBuilder) {
        
        if (proteinNode != null) {

            String headerSt = ">";

            //dataset
            if (proteinNode.getDataset().getName().toLowerCase().startsWith("trembl")) {
                headerSt += "tr|";
            } else {
                headerSt += "sp|";
            }
            //accession
            headerSt += proteinNode.getAccession() + "|";
            //name + fullname
            headerSt += proteinNode.getName() + " " + proteinNode.getFullName() + " ";
            //OS=
            headerSt += "OS=" + proteinNode.getOrganism().getScientificName() + " ";
            //GN=
            headerSt += "GN=";
            String[] geneNames = proteinNode.getGeneNames();
            if (geneNames.length > 0) {
                headerSt += geneNames[0];
            }

            resultStBuilder.append((headerSt + "\n"));
            resultStBuilder.append(com.era7.lib.bioinfo.bioinfoutil.fasta.FastaUtil.formatSequenceWithFastaFormat(proteinNode.getSequence().replaceAll(" ", ""), 60));

        }
    }
}
