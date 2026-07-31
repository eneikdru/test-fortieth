package com.eneik.generated.knowledgebase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PdfGenerator {

    public static byte[] generate(String title, String category, String tags, String content) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            List<Long> offsets = new ArrayList<>();

            // Header
            bos.write("%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));

            // Object 1: Catalog
            offsets.add((long) bos.size());
            bos.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.UTF_8));

            // Object 2: Pages
            offsets.add((long) bos.size());
            bos.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.UTF_8));

            // Object 3: Page
            offsets.add((long) bos.size());
            bos.write("3 0 obj\n<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 4 0 R >> >> /MediaBox [0 0 612 792] /Contents 5 0 R >>\nendobj\n".getBytes(StandardCharsets.UTF_8));

            // Object 4: Font
            offsets.add((long) bos.size());
            bos.write("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.UTF_8));

            // Prepare text stream
            ByteArrayOutputStream textStream = new ByteArrayOutputStream();
            textStream.write("BT\n/F1 12 Tf\n50 700 Td\n14 TL\n".getBytes(StandardCharsets.UTF_8));

            writePdfLine(textStream, "Document Title: " + (title != null && !title.trim().isEmpty() ? title : "-"));
            writePdfLine(textStream, "Category: " + (category != null && !category.trim().isEmpty() ? category : "-"));
            writePdfLine(textStream, "Tags: " + (tags != null && !tags.trim().isEmpty() ? tags : "-"));
            writePdfLine(textStream, "");
            writePdfLine(textStream, "Content:");
            if (content != null && !content.trim().isEmpty()) {
                String[] lines = content.split("\\r?\\n");
                for (String line : lines) {
                    writePdfLine(textStream, line);
                }
            } else {
                writePdfLine(textStream, "-");
            }
            textStream.write("ET\n".getBytes(StandardCharsets.UTF_8));
            byte[] textBytes = textStream.toByteArray();

            // Object 5: Content stream
            offsets.add((long) bos.size());
            bos.write("5 0 obj\n".getBytes(StandardCharsets.UTF_8));
            bos.write(("<< /Length " + textBytes.length + " >>\n").getBytes(StandardCharsets.UTF_8));
            bos.write("stream\n".getBytes(StandardCharsets.UTF_8));
            bos.write(textBytes);
            bos.write("\nendstream\nendobj\n".getBytes(StandardCharsets.UTF_8));

            // Xref
            long xrefOffset = bos.size();
            bos.write("xref\n".getBytes(StandardCharsets.UTF_8));
            bos.write(("0 " + (offsets.size() + 1) + "\n").getBytes(StandardCharsets.UTF_8));
            bos.write("0000000000 65535 f \n".getBytes(StandardCharsets.UTF_8));
            for (Long offset : offsets) {
                String formatted = String.format("%010d 00000 n \n", offset);
                bos.write(formatted.getBytes(StandardCharsets.UTF_8));
            }

            // Trailer
            bos.write("trailer\n".getBytes(StandardCharsets.UTF_8));
            bos.write(("<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\n").getBytes(StandardCharsets.UTF_8));
            bos.write("startxref\n".getBytes(StandardCharsets.UTF_8));
            bos.write((xrefOffset + "\n").getBytes(StandardCharsets.UTF_8));
            bos.write("%%EOF\n".getBytes(StandardCharsets.UTF_8));

            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private static void writePdfLine(ByteArrayOutputStream bos, String text) throws IOException {
        if (text == null) text = "";
        // Escape special PDF characters: (, ), \
        String escaped = text.replace("\\", "\\\\")
                             .replace("(", "\\(")
                             .replace(")", "\\)");
        bos.write(("(" + escaped + ") Tj T*\n").getBytes(StandardCharsets.UTF_8));
    }
}
