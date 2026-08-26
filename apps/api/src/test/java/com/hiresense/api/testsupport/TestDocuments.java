package com.hiresense.api.testsupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class TestDocuments {

    private TestDocuments() {}

    public static byte[] pdfWithText(String text) {
        Map<Integer, String> objects = new LinkedHashMap<>();
        objects.put(1, "<</Type/Catalog/Pages 2 0 R>>");
        objects.put(2, "<</Type/Pages/Kids[3 0 R]/Count 1>>");
        objects.put(
                3,
                "<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Contents 4 0 R"
                        + "/Resources<</Font<</F1 5 0 R>>>>>>");
        String escaped = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        String stream = "BT /F1 24 Tf 72 700 Td (" + escaped + ") Tj ET";
        objects.put(4, "<</Length " + stream.length() + ">>\nstream\n" + stream + "\nendstream");
        objects.put(5, "<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>");

        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        long[] offsets = new long[objects.size() + 1];
        for (Map.Entry<Integer, String> entry : objects.entrySet()) {
            offsets[entry.getKey()] = pdf.length();
            pdf.append(entry.getKey()).append(" 0 obj").append(entry.getValue()).append("endobj\n");
        }
        long xrefOffset = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append('\n');
        pdf.append("0000000000 65535 f \n");
        for (int i = 1; i <= objects.size(); i++) {
            pdf.append(String.format("%010d 00000 n \n", offsets[i]));
        }
        pdf.append("trailer<</Size ")
                .append(objects.size() + 1)
                .append("/Root 1 0 R>>\nstartxref\n")
                .append(xrefOffset)
                .append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    public static byte[] docxWithText(String text) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zip = new ZipOutputStream(baos)) {
                zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
                zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                                + "<Default Extension=\"rels\""
                                + " ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                                + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd."
                                + "openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                                + "</Types>")
                        .getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("_rels/.rels"));
                zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                                + "<Relationship Id=\"rId1\""
                                + " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                                + "/officeDocument\" Target=\"word/document.xml\"/>"
                                + "</Relationships>")
                        .getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("word/document.xml"));
                zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                                + "<w:body><w:p><w:r><w:t>" + text + "</w:t></w:r></w:p></w:body></w:document>")
                        .getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
