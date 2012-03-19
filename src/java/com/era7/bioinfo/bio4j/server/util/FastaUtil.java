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
