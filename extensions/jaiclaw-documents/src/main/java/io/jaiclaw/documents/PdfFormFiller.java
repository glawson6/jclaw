package io.jaiclaw.documents;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class PdfFormFiller {

    private final boolean flatten;

    public PdfFormFiller() {
        this(true);
    }

    public PdfFormFiller(boolean flatten) {
        this.flatten = flatten;
    }

    public PdfFormResult fill(byte[] templateBytes, Map<String, String> fieldValues) {
        try (PDDocument doc = Loader.loadPDF(templateBytes)) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                return new PdfFormResult.Failure("PDF has no AcroForm (no fillable fields)");
            }

            int fieldsSet = 0;
            for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
                PDField field = acroForm.getField(entry.getKey());
                if (field != null) {
                    field.setValue(entry.getValue());
                    fieldsSet++;
                }
            }

            if (flatten) {
                acroForm.flatten();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return new PdfFormResult.Success(out.toByteArray(), fieldsSet);
        } catch (IOException e) {
            return new PdfFormResult.Failure("Failed to fill PDF: " + e.getMessage());
        }
    }
}
