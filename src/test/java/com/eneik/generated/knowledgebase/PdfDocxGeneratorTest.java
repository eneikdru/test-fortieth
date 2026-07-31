package com.eneik.generated.knowledgebase;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;

public class PdfDocxGeneratorTest {

    @Test
    public void testPdfGeneration() {
        String title = "Pediatrics Guideline (V1)";
        String category = "Pediatrics";
        String tags = "clinical, guide, child-care";
        String content = "This is a paragraph with special symbols like (parenthesis) and \\backslashes\\.\nLine 2 is here.";

        byte[] pdfBytes = PdfGenerator.generate(title, category, tags, content);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        String pdfString = new String(pdfBytes, StandardCharsets.UTF_8);
        assertTrue(pdfString.startsWith("%PDF-1.4"));
        assertTrue(pdfString.contains("Document Title: Pediatrics Guideline \\(V1\\)"));
        assertTrue(pdfString.contains("Category: Pediatrics"));
        assertTrue(pdfString.contains("Tags: clinical, guide, child-care"));
        assertTrue(pdfString.contains("This is a paragraph with special symbols like \\(parenthesis\\) and \\\\backslashes\\\\."));
        assertTrue(pdfString.contains("Line 2 is here."));
    }

    @Test
    public void testDocxGeneration() throws Exception {
        String title = "Anatomy Handout";
        String category = "Anatomy";
        String tags = "bone, muscle, ligaments";
        String content = "Anatomy is the branch of biology concerned with the study of the structure of organisms.\nAnd its parts <with special xml chars & escaping>.";

        byte[] docxBytes = DocxGenerator.generate(title, category, tags, content);
        assertNotNull(docxBytes);
        assertTrue(docxBytes.length > 0);

        // Verify ZIP format magic header
        assertEquals((byte) 0x50, docxBytes[0]);
        assertEquals((byte) 0x4B, docxBytes[1]);

        // Unzip and inspect files
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docxBytes));
        ZipEntry entry;
        boolean foundContentTypes = false;
        boolean foundRels = false;
        boolean foundDocumentXml = false;
        String documentXmlContent = "";

        while ((entry = zis.getNextEntry()) != null) {
            if ("[Content_Types].xml".equals(entry.getName())) {
                foundContentTypes = true;
            } else if ("_rels/.rels".equals(entry.getName())) {
                foundRels = true;
            } else if ("word/document.xml".equals(entry.getName())) {
                foundDocumentXml = true;
                byte[] buffer = new byte[8192];
                int bytesRead;
                StringBuilder sb = new StringBuilder();
                while ((bytesRead = zis.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                }
                documentXmlContent = sb.toString();
            }
            zis.closeEntry();
        }
        zis.close();

        assertTrue(foundContentTypes, "Should contain [Content_Types].xml");
        assertTrue(foundRels, "Should contain _rels/.rels");
        assertTrue(foundDocumentXml, "Should contain word/document.xml");

        // Inspect document.xml
        assertTrue(documentXmlContent.contains("<w:t>Document Title: Anatomy Handout</w:t>"));
        assertTrue(documentXmlContent.contains("<w:t>Category: Anatomy</w:t>"));
        assertTrue(documentXmlContent.contains("<w:t>Tags: bone, muscle, ligaments</w:t>"));
        // Ensure XML entities are correctly escaped
        assertTrue(documentXmlContent.contains("&lt;with special xml chars &amp; escaping&gt;"));
    }

    @Test
    public void testPdfGenerationWithNullContent() {
        byte[] pdfBytes = PdfGenerator.generate("Title", "Category", "Tag", null);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        String pdfString = new String(pdfBytes, StandardCharsets.UTF_8);
        assertTrue(pdfString.startsWith("%PDF-1.4"));
    }

    @Test
    public void testDocxGenerationWithNullContent() throws Exception {
        byte[] docxBytes = DocxGenerator.generate("Title", "Category", "Tag", null);
        assertNotNull(docxBytes);
        assertTrue(docxBytes.length > 0);

        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docxBytes));
        ZipEntry entry;
        boolean foundDocumentXml = false;
        String documentXmlContent = "";

        while ((entry = zis.getNextEntry()) != null) {
            if ("word/document.xml".equals(entry.getName())) {
                foundDocumentXml = true;
                byte[] buffer = new byte[8192];
                int bytesRead;
                StringBuilder sb = new StringBuilder();
                while ((bytesRead = zis.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                }
                documentXmlContent = sb.toString();
            }
            zis.closeEntry();
        }
        zis.close();

        assertTrue(foundDocumentXml);
        assertTrue(documentXmlContent.contains("<w:t>Document Title: Title</w:t>"));
    }
}
