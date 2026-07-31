package com.eneik.generated.knowledgebase;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.charset.StandardCharsets;

public class DocumentExportHelper {

    public static byte[] generatePdf(KbDocument doc, KbDocumentVersion version) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        BaseFont bf;
        try {
            bf = BaseFont.createFont("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            try {
                bf = BaseFont.createFont("/usr/share/fonts/truetype/freefont/FreeSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ex) {
                bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            }
        }

        Font titleFont = new Font(bf, 18, Font.BOLD);
        Font bodyFont = new Font(bf, 12, Font.NORMAL);
        Font infoFont = new Font(bf, 10, Font.ITALIC);

        // Title
        Paragraph titlePara = new Paragraph(doc.getTitle(), titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        document.add(titlePara);

        document.add(new Paragraph(" ")); // spacer

        // Metadata
        if (doc.getCategory() != null) {
            document.add(new Paragraph("Category: " + doc.getCategory(), infoFont));
        }
        if (doc.getTags() != null && !doc.getTags().isEmpty()) {
            document.add(new Paragraph("Tags: " + String.join(", ", doc.getTags()), infoFont));
        }

        document.add(new Paragraph(" ")); // spacer
        document.add(new Paragraph("---", bodyFont));
        document.add(new Paragraph(" ")); // spacer

        // Content
        String content = (version != null && version.getIndexedContent() != null) ? version.getIndexedContent() : "";
        String[] lines = content.split("\r?\n");
        for (String line : lines) {
            document.add(new Paragraph(line, bodyFont));
        }

        document.close();
        return out.toByteArray();
    }

    public static byte[] generateDocx(KbDocument doc, KbDocumentVersion version) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            // 1. Write [Content_Types].xml
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                    "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                    "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                    "  <Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n" +
                    "</Types>";
            zos.write(contentTypes.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 2. Write _rels/.rels
            zos.putNextEntry(new ZipEntry("_rels/.rels"));
            String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n" +
                    "</Relationships>";
            zos.write(rels.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 3. Write word/document.xml
            zos.putNextEntry(new ZipEntry("word/document.xml"));

            StringBuilder bodyBuilder = new StringBuilder();
            // Title Paragraph
            bodyBuilder.append("<w:p>")
                    .append("<w:r><w:rPr><w:sz w:val=\"36\"/><w:szCs w:val=\"36\"/><w:b/></w:rPr>")
                    .append("<w:t>").append(escapeXml(doc.getTitle())).append("</w:t></w:r>")
                    .append("</w:p>\n");

            // Category Paragraph
            if (doc.getCategory() != null) {
                bodyBuilder.append("<w:p>")
                        .append("<w:r><w:rPr><w:i/></w:rPr>")
                        .append("<w:t>Category: ").append(escapeXml(doc.getCategory())).append("</w:t></w:r>")
                        .append("</w:p>\n");
            }

            // Tags Paragraph
            if (doc.getTags() != null && !doc.getTags().isEmpty()) {
                bodyBuilder.append("<w:p>")
                        .append("<w:r><w:rPr><w:i/></w:rPr>")
                        .append("<w:t>Tags: ").append(escapeXml(String.join(", ", doc.getTags()))).append("</w:t></w:r>")
                        .append("</w:p>\n");
            }

            // Separator
            bodyBuilder.append("<w:p><w:r><w:t>---</w:t></w:r></w:p>\n");

            // Content Paragraphs
            String rawContent = (version != null && version.getIndexedContent() != null) ? version.getIndexedContent() : "";
            String[] lines = rawContent.split("\r?\n");
            for (String line : lines) {
                bodyBuilder.append("<w:p>")
                        .append("<w:r>")
                        .append("<w:t>").append(escapeXml(line)).append("</w:t></w:r>")
                        .append("</w:p>\n");
            }

            String documentXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n" +
                    "  <w:body>\n" +
                    bodyBuilder.toString() +
                    "  </w:body>\n" +
                    "</w:document>";
            zos.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
