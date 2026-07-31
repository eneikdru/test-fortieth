package com.eneik.generated.knowledgebase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DocxGenerator {

    public static byte[] generate(String title, String category, String tags, String content) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(bos);

            // 1. [Content_Types].xml
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                    "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                    "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                    "  <Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n" +
                    "</Types>";
            zos.write(contentTypes.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 2. _rels/.rels
            zos.putNextEntry(new ZipEntry("_rels/.rels"));
            String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n" +
                    "</Relationships>";
            zos.write(rels.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 3. word/document.xml
            zos.putNextEntry(new ZipEntry("word/document.xml"));
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            sb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n");
            sb.append("  <w:body>\n");

            // Add fields
            addParagraph(sb, "Document Title: " + title);
            addParagraph(sb, "Category: " + category);
            addParagraph(sb, "Tags: " + tags);
            addParagraph(sb, "");
            addParagraph(sb, "Content:");

            if (content != null) {
                String[] lines = content.split("\\r?\\n");
                for (String line : lines) {
                    addParagraph(sb, line);
                }
            }

            sb.append("  </w:body>\n");
            sb.append("</w:document>");

            zos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate DOCX", e);
        }
    }

    private static void addParagraph(StringBuilder sb, String text) {
        if (text == null) text = "";
        // Simple XML escaping
        String escaped = text.replace("&", "&amp;")
                             .replace("<", "&lt;")
                             .replace(">", "&gt;")
                             .replace("\"", "&quot;")
                             .replace("'", "&apos;");
        sb.append("    <w:p>\n")
          .append("      <w:r>\n")
          .append("        <w:t>").append(escaped).append("</w:t>\n")
          .append("      </w:r>\n")
          .append("    </w:p>\n");
    }
}
