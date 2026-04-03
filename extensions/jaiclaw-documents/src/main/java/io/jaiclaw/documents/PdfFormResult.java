package io.jaiclaw.documents;

public sealed interface PdfFormResult {
    record Success(byte[] pdfBytes, int fieldsSet) implements PdfFormResult {}
    record Failure(String reason) implements PdfFormResult {}
}
